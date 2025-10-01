package com.txnow.infrastructure.persistence.entity;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ExchangeRateHistory JPA Entity
 * Infrastructure Layer에서 JPA 영속성을 담당
 */
@Entity
@Table(name = "exchange_rate_history", indexes = {
    @Index(name = "idx_currency_timestamp", columnList = "currency,timestamp")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRateHistoryJpaEntity {

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
    public ExchangeRateHistoryJpaEntity(Long id, Currency currency, BigDecimal rate,
                                        BigDecimal change, LocalDateTime timestamp) {
        this.id = id;
        this.currency = currency;
        this.rate = rate;
        this.change = change;
        this.timestamp = timestamp;
    }

    /**
     * Domain Entity로 변환
     */
    public ExchangeRateHistory toDomain() {
        return ExchangeRateHistory.builder()
            .id(this.id)
            .currency(this.currency)
            .rate(this.rate)
            .change(this.change)
            .timestamp(this.timestamp)
            .build();
    }

    /**
     * Domain Entity로부터 JPA Entity 생성
     */
    public static ExchangeRateHistoryJpaEntity fromDomain(ExchangeRateHistory domain) {
        return ExchangeRateHistoryJpaEntity.builder()
            .id(domain.getId())
            .currency(domain.getCurrency())
            .rate(domain.getRate())
            .change(domain.getChange())
            .timestamp(domain.getTimestamp())
            .build();
    }
}
