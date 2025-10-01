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
        log.info("Starting daily exchange rate synchronization...");

        LocalDate today = LocalDate.now();

        for (Currency currency : SUPPORTED_CURRENCIES) {
            if (!currency.isBokSupported()) {
                log.warn("Currency {} not supported by BOK API", currency);
                return;
            }

            String bokCode = currency.getBokCode();

            // 1. 오늘 환율 조회
            BokApiResponse response = bokApiClient.getExchangeRate(bokCode, today,
                today);

            // 2. DB에서 전일 데이터 조회 (변동폭 계산용)
            LocalDate yesterday = today.minusDays(1);

            ExchangeRateHistory yesterdayRate = historyRepository
                .findLatestByCurrencyAndTimestampBetween(
                    currency,
                    yesterday.atStartOfDay(),
                    yesterday.atTime(23, 59, 59)
                );

            // 3. 데이터 파싱 및 저장
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            for (var row : response.statisticSearch().rows()) {
                LocalDate date = LocalDate.parse(row.time(), formatter);
                BigDecimal rate = new BigDecimal(row.dataValue());

                // JPY는 100엔 기준이므로 1엔당으로 변환
                if (currency == Currency.JPY) {
                    rate = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                }

                // 4. 전일 대비 변동폭 계산
                BigDecimal change = BigDecimal.ZERO;
                if (yesterdayRate != null) {
                    change = rate.subtract(yesterdayRate.getRate());
                }

                // 5. 저장
                ExchangeRateHistory history = ExchangeRateHistory.builder()
                    .currency(currency)
                    .rate(rate)
                    .change(change)
                    .timestamp(date.atTime(11, 0))  // 오전 11시로 고정
                    .build();

                historyRepository.save(history);
            }
        }

    }
}
