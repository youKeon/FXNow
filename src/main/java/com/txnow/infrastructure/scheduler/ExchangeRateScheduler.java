package com.txnow.infrastructure.scheduler;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import com.txnow.infrastructure.external.bok.BokApiClient;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class ExchangeRateScheduler {

    private final BokApiClient bokApiClient;
    private final ExchangeRateHistoryRepository historyRepository;

    private static final Currency[] SUPPORTED_CURRENCIES = {Currency.USD, Currency.EUR,
        Currency.JPY, Currency.CNY};

    /**
     * 평일 오전 11:30에 실행 (한국은행 데이터 갱신 후)
     */
    @Scheduled(cron = "0 30 11 * * MON-FRI")
    @Transactional
    public void setDailyExchangeRates() {
        LocalDate today = LocalDate.now();

        for (Currency currency : SUPPORTED_CURRENCIES) {
            if (!currency.isSupportedCurrency()) {
                log.warn("Currency {} not supported by BOK API", currency);
                continue;
            }

            // 1. 오늘 환율 조회
            BigDecimal currentRate = bokApiClient.getCurrentExchangeRate(currency);

            if (currentRate == null) {
                log.warn("No exchange rate data available for {} (holiday or API unavailable)", currency);
                continue;
            }

            // 2. DB에서 전일 데이터 조회 (변동폭 계산용)
            LocalDate yesterday = today.minusDays(1);
            ExchangeRateHistory yesterdayRate = historyRepository
                .findExchangeRateByTimestamp(
                    currency,
                    yesterday.atStartOfDay(),
                    yesterday.atTime(23, 59, 59)
                );

            // 3. 전일 대비 변동폭 계산
            BigDecimal change = BigDecimal.ZERO;
            if (yesterdayRate != null) {
                change = currentRate.subtract(yesterdayRate.getRate());
            }

            // 4. 저장
            ExchangeRateHistory history = ExchangeRateHistory.builder()
                .currency(currency)
                .rate(currentRate)
                .change(change)
                .timestamp(today.atTime(11, 0))  // 오전 11시로 고정
                .build();

            historyRepository.save(history);
            log.info("Saved exchange rate for {}: {}", currency, currentRate);
        }

        log.info("Daily exchange rate synchronization completed");
    }
}
