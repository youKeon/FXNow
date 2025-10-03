package com.txnow.domain.exchange.provider;

import com.txnow.domain.exchange.exception.ExchangeRateNotFoundException;
import com.txnow.domain.exchange.exception.ExchangeRateUnavailableException;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.DailyRate;
import com.txnow.domain.exchange.model.ChartPeriod;

import java.math.BigDecimal;
import java.util.List;

/**
 * 환율 데이터 Provider
 */
public interface ExchangeRateProvider {

    /**
     * 특정 통화의 현재 환율 조회 (대원화)
     * @param currency 조회할 통화
     * @return 1 통화당 KRW 환율
     * @throws ExchangeRateNotFoundException 환율 데이터가 존재하지 않는 경우
     * @throws ExchangeRateUnavailableException 서비스 장애로 조회 불가능한 경우
     */
    BigDecimal getCurrentExchangeRate(Currency currency);

    /**
     * 특정 통화의 일별 환율 데이터 조회
     * @param currency 조회할 통화
     * @param period 조회 기간
     * @return 일별 환율 리스트 (날짜 오름차순)
     * @throws ExchangeRateNotFoundException 환율 데이터가 존재하지 않는 경우
     * @throws ExchangeRateUnavailableException 서비스 장애로 조회 불가능한 경우
     */
    List<DailyRate> getExchangeRateHistory(Currency currency, ChartPeriod period);
}