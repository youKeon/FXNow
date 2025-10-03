package com.txnow.domain.exchange.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record DailyRate(
    LocalDate date,
    BigDecimal rate
) {
    public DailyRate {
        Objects.requireNonNull(date, "Date cannot be null");
        Objects.requireNonNull(rate, "Rate cannot be null");

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
    }
}
