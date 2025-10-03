package com.txnow.infrastructure.cache;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 캐시 키 생성기
 * 패턴: fxnow:{service}:{entity}:{identifier}
 */
@Component
public class CacheKeyGenerator {

    private static final String PREFIX = "fxnow:";
    private static final Random RANDOM = new Random();

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
     * BOK API Rate Limit Sorted Set 키
     * 예: fxnow:bok_api:rate_limit
     */
    public String bokApiRateLimitKey() {
        return PREFIX + "bok_api:rate_limit";
    }

    /**
     * BOK API 마지막 호출 시각 키
     * 예: fxnow:bok_api:last_call
     */
    public String bokApiLastCallKey() {
        return PREFIX + "bok_api:last_call";
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

    /**
     * TTL Jitter 계산 (캐시 만료 시간 분산)
     * 기본 TTL에 랜덤 jitter를 추가하여 Cache Stampede 방지
     *
     * @param baseTtlSeconds 기본 TTL (초)
     * @param jitterPercentage Jitter 비율 (0.0 ~ 1.0, 예: 0.1 = ±10%)
     * @return Jitter가 적용된 TTL (초)
     */
    public long calculateTtlWithJitter(long baseTtlSeconds, double jitterPercentage) {
        if (jitterPercentage <= 0 || jitterPercentage > 1.0) {
            return baseTtlSeconds;
        }

        // Jitter 범위 계산: baseTtl ± (baseTtl * jitterPercentage)
        long jitterRange = (long) (baseTtlSeconds * jitterPercentage);

        // -jitterRange ~ +jitterRange 사이의 랜덤 값
        long jitter = RANDOM.nextLong(jitterRange * 2 + 1) - jitterRange;

        long finalTtl = baseTtlSeconds + jitter;

        // 최소 TTL 보장 (기본 TTL의 50% 이상)
        long minTtl = baseTtlSeconds / 2;
        return Math.max(finalTtl, minTtl);
    }
}