package com.txnow.infrastructure.cache;

import java.time.LocalDate;
import org.springframework.stereotype.Component;

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
     * 환율 히스토리 키
     * 예: fxnow:chart:USD:1d
     */
    public String exchangeRateHistoryKey(String currencyCode, String period) {
        return chartDataKey(currencyCode, period);
    }

    /**
     * BOK API Rate Limit Sorted Set 키
     * 예: fxnow:bok_api:rate_limit
     */
    public String bokApiRateLimitKey() {
        return PREFIX + "bok_api:rate_limit";
    }
}