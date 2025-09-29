package com.txnow.domain.exchange.provider;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.infrastructure.external.bok.ChartPeriod;
import com.txnow.infrastructure.external.bok.BokApiResponse;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 환율 데이터 제공자 인터페이스
 * Domain Layer에서 정의하는 순수한 도메인 인터페이스
 */
public interface ExchangeRateProvider {

    /**
     * 특정 통화의 현재 환율 조회 (대원화)
     * @param currency 조회할 통화
     * @return 1 통화당 KRW 환율
     */
    Optional<BigDecimal> getCurrentExchangeRate(Currency currency);

    /**
     * 특정 통화의 히스토리 환율 데이터 조회
     * @param currency 조회할 통화
     * @param period 조회 기간
     * @return BOK API 히스토리 응답 데이터
     */
    Optional<BokApiResponse> getExchangeRateHistory(Currency currency, ChartPeriod period);
}