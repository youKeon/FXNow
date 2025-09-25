package com.txnow.domain.exchange.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

public record CurrentExchangeRateData(
    Currency baseCurrency,
    LocalDateTime lastUpdated,
    Map<Currency, CurrentExchangeRate> rates
) {

    public CurrentExchangeRateData {
        Objects.requireNonNull(baseCurrency, "Base currency cannot be null");
        Objects.requireNonNull(lastUpdated, "Last updated timestamp cannot be null");
        Objects.requireNonNull(rates, "Rates map cannot be null");

        if (rates.isEmpty()) {
            throw new IllegalArgumentException("Rates map cannot be empty");
        }
    }
}