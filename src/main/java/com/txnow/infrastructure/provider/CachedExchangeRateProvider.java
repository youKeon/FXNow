package com.txnow.infrastructure.provider;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.DailyRate;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.infrastructure.cache.CacheKeyGenerator;
import com.txnow.domain.exchange.model.ChartPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 환율 캐싱 (L1 캐시)
 * TTL Jitter를 적용하여 Cache Stampede 방지
 */
@Slf4j
@RequiredArgsConstructor
public class CachedExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    @Value("${cache.exchange-rate.ttl-seconds:86400}")
    private long baseTtlSeconds;

    @Value("${cache.exchange-rate.jitter-percentage:0.2}")
    private double jitterPercentage;

    @Value("${cache.stale-data.ttl-seconds:604800}")
    private long staleTtlSeconds;


    @Override
    public BigDecimal getCurrentExchangeRate(Currency currency) {
        if (currency == Currency.KRW) {
            return BigDecimal.ONE;
        }

        // L1: Redis 캐시 조회
        BigDecimal cachedRate = getFromRedisCache(currency);
        if (cachedRate != null) {
            log.debug("Cache HIT (Redis): {}", currency);
            return cachedRate;
        }

        log.debug("Cache MISS (Redis): {}", currency);

        // Redis에 없으면 DB에서 조회
        BigDecimal rate = delegate.getCurrentExchangeRate(currency);

        // Redis에 캐싱
        saveToRedisCache(currency, rate);

        return rate;
    }

    @Override
    public List<DailyRate> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        // 일별 데이터는 캐싱하지 않음 (별도의 @Cacheable로 처리 가능)
        return delegate.getExchangeRateHistory(currency, period);
    }

    /**
     * Redis에서 환율 조회
     */
    private BigDecimal getFromRedisCache(Currency currency) {
        String cacheKey = cacheKeyGenerator.exchangeRateKey(currency.name());
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (cached instanceof BigDecimal) {
                return (BigDecimal) cached;
            } else if (cached instanceof Number) {
                return new BigDecimal(cached.toString());
            }
        }
        return null;
    }

    /**
     * Redis에 환율 저장 (TTL Jitter 적용)
     * Cache Stampede 방지를 위해 각 통화별로 다른 만료 시간 설정
     */
    private void saveToRedisCache(Currency currency, BigDecimal rate) {
        String cacheKey = cacheKeyGenerator.exchangeRateKey(currency.name());

        // TTL Jitter 계산 (±20% 분산)
        long ttlWithJitter = cacheKeyGenerator.calculateTtlWithJitter(baseTtlSeconds, jitterPercentage);

        redisTemplate.opsForValue().set(cacheKey, rate, ttlWithJitter, TimeUnit.SECONDS);

        log.debug("Saved to Redis: {} = {} (TTL: {}s, base: {}s, jitter: {}%)",
            currency, rate, ttlWithJitter, baseTtlSeconds, (int)(jitterPercentage * 100));

        // Stale 데이터도 저장 (Fallback용)
        String staleKey = cacheKeyGenerator.staleDataKey(currency.name());
        redisTemplate.opsForValue().set(staleKey, rate, staleTtlSeconds, TimeUnit.SECONDS);
    }
}
