package com.txnow.domain.exchange.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record ConversionResult(
    BigDecimal originalAmount,
    Currency fromCurrency,
    BigDecimal convertedAmount,
    Currency toCurrency,
    BigDecimal exchangeRate,
    LocalDateTime timestamp
) {

    public ConversionResult {
        Objects.requireNonNull(originalAmount, "Original amount cannot be null");
        Objects.requireNonNull(fromCurrency, "From currency cannot be null");
        Objects.requireNonNull(convertedAmount, "Converted amount cannot be null");
        Objects.requireNonNull(toCurrency, "To currency cannot be null");
        Objects.requireNonNull(exchangeRate, "Exchange rate cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }
}