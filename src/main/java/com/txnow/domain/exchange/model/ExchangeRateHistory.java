package com.txnow.domain.exchange.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "exchange_rate_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 통화 코드 (USD, EUR, JPY 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;

    /**
     * 환율 (1 통화당 KRW)
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rate;

    /**
     * 전일 대비 변동폭
     */
    @Column(name = "change_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal change;

    /**
     * 환율 기록 시각
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Builder
    public ExchangeRateHistory(Currency currency, BigDecimal rate, BigDecimal change, LocalDateTime timestamp) {
        Objects.requireNonNull(currency, "Currency cannot be null");
        Objects.requireNonNull(rate, "Rate cannot be null");
        Objects.requireNonNull(change, "Change cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }

        this.currency = currency;
        this.rate = rate;
        this.change = change;
        this.timestamp = timestamp;
    }

    public static ExchangeRateHistory from(CurrentExchangeRate current) {
        return ExchangeRateHistory.builder()
            .currency(current.currency())
            .rate(current.rate())
            .change(current.change())
            .timestamp(current.lastUpdated())
            .build();
    }
}