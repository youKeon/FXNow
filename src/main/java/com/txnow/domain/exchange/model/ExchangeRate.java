package com.txnow.domain.exchange.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

public record ExchangeRate(
    CurrencyPair currencyPair,
    BigDecimal rate,
    LocalDateTime timestamp
) {

    private static final int RATE_PRECISION = 4;

    public ExchangeRate {
        Objects.requireNonNull(currencyPair, "Currency pair cannot be null");
        Objects.requireNonNull(rate, "Exchange rate cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
    }

    public BigDecimal convert(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        BigDecimal convertedAmount = amount.multiply(rate);
        int targetScale = currencyPair.target().getDecimalPlaces();

        return convertedAmount.setScale(targetScale, RoundingMode.HALF_UP);
    }

    public ExchangeRate reverse() {
        BigDecimal reversedRate = BigDecimal.ONE.divide(rate, RATE_PRECISION, RoundingMode.HALF_UP);
        return new ExchangeRate(currencyPair.reverse(), reversedRate, timestamp);
    }
}