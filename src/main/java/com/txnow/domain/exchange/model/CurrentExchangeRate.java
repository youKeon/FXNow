package com.txnow.domain.exchange.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record CurrentExchangeRate(
    Currency currency,
    BigDecimal rate,
    BigDecimal change,
    LocalDateTime lastUpdated
) {

    public CurrentExchangeRate {
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(rate, "Rate cannot be null");
        Objects.requireNonNull(change, "Change cannot be null");
        Objects.requireNonNull(lastUpdated, "Last updated timestamp cannot be null");

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
    }
}