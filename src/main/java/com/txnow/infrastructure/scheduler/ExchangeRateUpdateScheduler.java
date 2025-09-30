package com.txnow.infrastructure.scheduler;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.CurrentExchangeRate;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import com.txnow.infrastructure.websocket.ActiveCurrencyPairTracker;
import com.txnow.infrastructure.websocket.ExchangeRateBroadcastService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateUpdateScheduler {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeRateBroadcastService broadcastService;
    private final ActiveCurrencyPairTracker currencyPairTracker;
    private final ExchangeRateHistoryRepository historyRepository;

    @Scheduled(fixedRate = 10000) // 10초
    public void updateExchangeRates() {
        Set<String> activePairs = currencyPairTracker.getActivePairs();

        if (activePairs.isEmpty()) {
            log.debug("No active currency pairs to update");
            return;
        }

        log.debug("Starting scheduled exchange rate update for {} active pairs",
            activePairs.size()
        );

        for (String pairKey : activePairs) {
            String[] currencies = pairKey.split("/");
            String fromCode = currencies[0];
            String toCode = currencies[1];

            Currency fromCurrency = Currency.valueOf(fromCode);
            Currency toCurrency = Currency.valueOf(toCode);

            exchangeRateProvider.getExchangeRate(fromCurrency, toCurrency)
                .ifPresentOrElse(
                    rate -> {
                        broadcastService.broadcastExchangeRateUpdate(fromCode, toCode, rate);
                        log.debug("Updated exchange rate: {} -> {} = {}", fromCode, toCode, rate);
                    },
                    () -> log.warn("Failed to get exchange rate for {} -> {}", fromCode, toCode)
                );
        }

        log.debug("Completed scheduled exchange rate update");
    }

    /**
     * 30분마다 주요 통화의 환율을 DB에 저장 시간대별 차트 조회를 위한 히스토리 데이터 축적
     */
    @Scheduled(cron = "0 */30 * * * *") // 30분마다
    public void saveExchangeRateHistory() {
        log.info("Starting exchange rate history save");

        Currency[] majorCurrencies = {
            Currency.USD,
            Currency.EUR,
            Currency.JPY,
            Currency.CNY,
            Currency.GBP
        };

        LocalDateTime now = LocalDateTime.now();

        for (Currency currency : majorCurrencies) {
            // 현재 환율 조회
            BigDecimal currentRate = exchangeRateProvider.getCurrentExchangeRate(currency)
                .orElse(null);

            if (currentRate == null) {
                log.warn("Failed to get current rate for {}, skipping", currency);
                continue;
            }

            // 변동폭 계산 (임시로 0으로 설정, 추후 개선 가능)
            BigDecimal change = BigDecimal.ZERO;

            // 히스토리 엔티티 생성 및 저장
            CurrentExchangeRate current = new CurrentExchangeRate(
                currency,
                currentRate,
                change,
                now
            );

            ExchangeRateHistory history = ExchangeRateHistory.from(current);
            historyRepository.save(history);
        }
    }
}