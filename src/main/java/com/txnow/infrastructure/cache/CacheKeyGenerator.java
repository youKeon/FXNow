package com.txnow.infrastructure.cache;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 캐시 키 생성기
 * 패턴: fxnow:{service}:{entity}:{identifier}
 */
@Component
public class CacheKeyGenerator {

    private static final String PREFIX = "fxnow:";

    /**
     * 환율 데이터 키
     * 예: fxnow:exchange_rate:USD:2025-09-30
     */
    public String exchangeRateKey(String currencyCode) {
        LocalDate today = LocalDate.now();
        return PREFIX + "exchange_rate:" + currencyCode + ":" + today;
    }

    /**
     * 차트 데이터 키
     * 예: fxnow:chart:USD:1d
     */
    public String chartDataKey(String currencyCode, String period) {
        return PREFIX + "chart:" + currencyCode + ":" + period;
    }

    /**
     * 환율 목록 키 (여러 통화)
     * 예: fxnow:exchange_rates:CNY,EUR,JPY,USD
     */
    public String exchangeRateListKey(List<String> currencies) {
        String sorted = currencies.stream()
                .sorted()
                .collect(Collectors.joining(","));
        return PREFIX + "exchange_rates:" + sorted;
    }

    /**
     * 통화 목록 키
     * 예: fxnow:currencies:all
     */
    public String currenciesKey() {
        return PREFIX + "currencies:all";
    }

    /**
     * Rate Limit 카운터 키
     * 예: fxnow:rate_limit:bok_api:count
     */
    public String rateLimitCounterKey(String apiName) {
        return PREFIX + "rate_limit:" + apiName + ":count";
    }

    /**
     * Rate Limit 윈도우 시작 시간 키
     * 예: fxnow:rate_limit:bok_api:window_start
     */
    public String rateLimitWindowKey(String apiName) {
        return PREFIX + "rate_limit:" + apiName + ":window_start";
    }

    /**
     * 타임스탬프 키 (데이터 신선도 체크)
     * 예: fxnow:timestamp:USD
     */
    public String timestampKey(String currencyCode) {
        return PREFIX + "timestamp:" + currencyCode;
    }

    /**
     * Stale 데이터 키 (만료된 캐시 백업)
     * 예: fxnow:stale:exchange_rate:USD
     */
    public String staleDataKey(String currencyCode) {
        return PREFIX + "stale:exchange_rate:" + currencyCode;
    }
}