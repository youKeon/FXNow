package com.txnow.domain.exchange.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 환율 이력 Domain Entity (Pure POJO)
 * JPA 의존성 없이 순수 비즈니스 로직만 포함
 */
@Getter
public class ExchangeRateHistory {

    private final Long id;
    private final Currency currency;
    private final BigDecimal rate;
    private final BigDecimal change;
    private final LocalDateTime timestamp;

    @Builder
    public ExchangeRateHistory(Long id, Currency currency, BigDecimal rate,
                               BigDecimal change, LocalDateTime timestamp) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(rate, "Rate cannot be null");
        Objects.requireNonNull(change, "Change cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }

        this.id = id;
        this.currency = currency;
        this.rate = rate;
        this.change = change;
        this.timestamp = timestamp;
    }

    /**
     * CurrentExchangeRate로부터 ExchangeRateHistory 생성
     */
    public static ExchangeRateHistory from(CurrentExchangeRate current) {
        return ExchangeRateHistory.builder()
            .currency(current.currency())
            .rate(current.rate())
            .change(current.change())
            .timestamp(current.lastUpdated())
            .build();
    }
}