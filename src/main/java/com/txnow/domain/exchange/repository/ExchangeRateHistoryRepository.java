package com.txnow.domain.exchange.repository;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateHistory;

import java.time.LocalDateTime;
import java.util.List;

public interface ExchangeRateHistoryRepository {

    /**
     * 특정 통화의 시간대별 환율 이력 조회
     * @param currency 통화
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 시간순으로 정렬된 환율 이력 (오름차순)
     */
    List<ExchangeRateHistory> findByCurrencyAndTimestampBetween(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * 특정 통화의 특정 기간 내 가장 최근 환율 조회
     * @param currency 통화
     * @param startTime 시작 시각
     * @param endTime 종료 시각
     * @return 가장 최근 환율 (없으면 null)
     */
    ExchangeRateHistory findExchangeRateByTimestamp(
        Currency currency,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    /**
     * 환율 이력 단일 저장
     * @param history 저장할 환율 이력
     */
    void save(ExchangeRateHistory history);
}