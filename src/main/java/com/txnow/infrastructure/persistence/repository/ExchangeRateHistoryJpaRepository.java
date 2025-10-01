package com.txnow.infrastructure.persistence.repository;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.infrastructure.persistence.entity.ExchangeRateHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ExchangeRateHistory JPA Repository
 * Infrastructure Layer에서 JPA 영속성을 담당
 */
public interface ExchangeRateHistoryJpaRepository extends JpaRepository<ExchangeRateHistoryJpaEntity, Long> {

    /**
     * 특정 통화의 시간대별 환율 이력 조회 (오름차순)
     */
    List<ExchangeRateHistoryJpaEntity> findByCurrencyAndTimestampBetweenOrderByTimestampAsc(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * 특정 통화의 시간대별 환율 이력 조회 (내림차순, limit 1)
     */
    ExchangeRateHistoryJpaEntity findFirstByCurrencyAndTimestampBetweenOrderByTimestampDesc(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    );
}
