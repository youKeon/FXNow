package com.txnow.domain.exchange.provider;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.HistoricalRate;
import com.txnow.infrastructure.external.bok.ChartPeriod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 환율 데이터 제공자 인터페이스
 * Domain Layer에서 정의하는 순수한 도메인 인터페이스
 * 모든 메서드는 Optional을 반환하여 "데이터 없음" 케이스를 명시적으로 처리합니다.
 */
public interface ExchangeRateProvider {

    /**
     * 특정 통화의 현재 환율 조회 (대원화)
     * @param currency 조회할 통화
     * @return 1 통화당 KRW 환율, 조회 실패 시 empty
     */
    Optional<BigDecimal> getCurrentExchangeRate(Currency currency);

    /**
     * 두 통화 간의 환율 조회
     * @param fromCurrency 기준 통화
     * @param toCurrency 대상 통화
     * @return fromCurrency 1단위당 toCurrency 환율, 조회 실패 시 empty
     */
    Optional<BigDecimal> getExchangeRate(Currency fromCurrency, Currency toCurrency);

    /**
     * 특정 통화의 히스토리 환율 데이터 조회
     * @param currency 조회할 통화
     * @param period 조회 기간
     * @return 히스토리 환율 리스트 (날짜 오름차순), 조회 실패 시 empty
     */
    Optional<List<HistoricalRate>> getExchangeRateHistory(Currency currency, ChartPeriod period);
}