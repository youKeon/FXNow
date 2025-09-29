package com.txnow.infrastructure.scheduler;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.infrastructure.websocket.ActiveCurrencyPairTracker;
import com.txnow.infrastructure.websocket.ExchangeRateBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateUpdateScheduler {

    private final ExchangeRateProvider exchangeRateProvider;
    private final ExchangeRateBroadcastService broadcastService;
    private final ActiveCurrencyPairTracker currencyPairTracker;

    @Scheduled(fixedRate = 10000) // 10초마다 실행
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

            BigDecimal rate = exchangeRateProvider.getExchangeRate(fromCurrency, toCurrency);
            broadcastService.broadcastExchangeRateUpdate(fromCode, toCode, rate);

            log.debug("Updated exchange rate: {} -> {} = {}", fromCode, toCode, rate);
        }

        log.debug("Completed scheduled exchange rate update");
    }
}