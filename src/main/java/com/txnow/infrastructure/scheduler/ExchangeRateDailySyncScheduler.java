package com.txnow.infrastructure.scheduler;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import com.txnow.infrastructure.external.bok.BokApiClient;
import com.txnow.infrastructure.external.bok.BokApiResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환율 데이터 일일 동기화 스케줄러 평일 오전 11:30에 전일~오늘 데이터 수집
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateDailySyncScheduler {

    private final BokApiClient bokApiClient;
    private final ExchangeRateHistoryRepository historyRepository;

    private static final int API_CALL_DELAY_MS = 2000; // 2초
    private static final Currency[] SUPPORTED_CURRENCIES = {Currency.USD, Currency.EUR,
        Currency.JPY, Currency.CNY};

    /**
     * 평일 오전 11:30에 실행 (한국은행 데이터 갱신 후)
     */
    @Scheduled(cron = "0 30 11 * * MON-FRI")
    @Transactional
    public void syncDailyExchangeRates() {
        log.info("Starting daily exchange rate synchronization...");

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();

        int successCount = 0;
        int failCount = 0;

        for (Currency currency : SUPPORTED_CURRENCIES) {
            try {
                boolean success = syncCurrencyData(currency, yesterday, today);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }

                // API Rate Limit 방지
                Thread.sleep(API_CALL_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Daily sync interrupted", e);
                break;
            } catch (Exception e) {
                log.error("Failed to sync {} exchange rate", currency, e);
                failCount++;
            }
        }

        log.info("Daily sync completed. Success: {}, Failed: {}", successCount, failCount);
    }

    /**
     * 특정 통화의 데이터 동기화
     */
    private boolean syncCurrencyData(Currency currency, LocalDate startDate, LocalDate endDate) {
        if (!currency.isBokSupported()) {
            log.warn("Currency {} not supported by BOK API", currency);
            return false;
        }

        String bokCode = currency.getBokCode();
        log.info("Syncing {} data from {} to {}", currency, startDate, endDate);

        Optional<BokApiResponse> responseOpt = bokApiClient.getExchangeRate(bokCode, startDate,
            endDate);
        if (responseOpt.isEmpty()) {
            log.warn("No data received from BOK API for {}", currency);
            return false;
        }

        BokApiResponse response = responseOpt.get();
        var statisticSearch = response.statisticSearch();
        if (statisticSearch == null || statisticSearch.rows() == null || statisticSearch.rows()
            .isEmpty()) {
            log.warn("Empty data from BOK API for {}", currency);
            return false;
        }

        // 데이터 파싱 및 저장
        List<ExchangeRateHistory> histories = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        BigDecimal previousRate = null;
        for (var row : statisticSearch.rows()) {
            try {
                LocalDate date = LocalDate.parse(row.time(), formatter);
                BigDecimal rate = new BigDecimal(row.dataValue());

                // JPY는 100엔 기준이므로 1엔당으로 변환
                if (currency == Currency.JPY) {
                    rate = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                }

                // 중복 체크
                boolean exists = historyRepository.existsByCurrencyAndTimestampBetween(
                    currency,
                    date.atStartOfDay(),
                    date.atTime(23, 59, 59)
                );

                if (!exists) {
                    // 변동폭 계산 (전일 대비)
                    BigDecimal change = BigDecimal.ZERO;
                    if (previousRate != null) {
                        change = rate.subtract(previousRate);
                    } else {
                        // 전일 데이터 조회
                        LocalDate prevDate = date.minusDays(1);
                        List<ExchangeRateHistory> prevData = historyRepository
                            .findByCurrencyAndTimestampBetweenOrderByTimestampDesc(
                                currency,
                                prevDate.atStartOfDay(),
                                prevDate.atTime(23, 59, 59)
                            );
                        if (!prevData.isEmpty()) {
                            previousRate = prevData.get(0).getRate();
                            change = rate.subtract(previousRate);
                        }
                    }

                    ExchangeRateHistory history = ExchangeRateHistory.builder()
                        .currency(currency)
                        .rate(rate)
                        .change(change)
                        .timestamp(date.atTime(11, 0))  // 오전 11시로 고정
                        .build();
                    histories.add(history);
                }

                previousRate = rate;
            } catch (Exception e) {
                log.warn("Failed to parse row for {}: time={}, value={}",
                    currency, row.time(), row.dataValue(), e);
            }
        }

        if (!histories.isEmpty()) {
            historyRepository.saveAll(histories);
            log.info("Synced {} new records for {}", histories.size(), currency);
        } else {
            log.info("No new records for {} (all exist)", currency);
        }

        return true;
    }
}
