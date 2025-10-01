package com.txnow.domain.exchange.repository;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 환율 이력 Repository
 */
@Repository
public interface ExchangeRateHistoryRepository extends JpaRepository<ExchangeRateHistory, Long> {

    /**
     * 특정 통화의 시간대별 환율 이력 조회 (오름차순)
     * @param currency 통화
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 시간순으로 정렬된 환율 이력
     */
    List<ExchangeRateHistory> findByCurrencyAndTimestampBetweenOrderByTimestampAsc(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * 특정 통화의 시간대별 환율 이력 조회 (내림차순)
     * @param currency 통화
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 최신순으로 정렬된 환율 이력
     */
    List<ExchangeRateHistory> findByCurrencyAndTimestampBetweenOrderByTimestampDesc(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * 특정 통화 및 기간에 데이터가 존재하는지 확인
     * @param currency 통화
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 존재 여부
     */
    boolean existsByCurrencyAndTimestampBetween(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    );
}