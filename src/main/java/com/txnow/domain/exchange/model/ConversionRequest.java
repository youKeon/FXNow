package com.txnow.domain.exchange.model;

import java.math.BigDecimal;
import java.util.Objects;

public record ConversionRequest(
    BigDecimal amount,
    Currency fromCurrency,
    Currency toCurrency
) {

    public ConversionRequest {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(fromCurrency, "From currency cannot be null");
        Objects.requireNonNull(toCurrency, "To currency cannot be null");

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }

        if (fromCurrency == toCurrency) {
            throw new IllegalArgumentException("From and to currencies cannot be the same");
        }
    }

    public CurrencyPair toCurrencyPair() {
        return new CurrencyPair(fromCurrency, toCurrency);
    }
}