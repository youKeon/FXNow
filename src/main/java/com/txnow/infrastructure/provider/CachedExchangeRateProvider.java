package com.txnow.infrastructure.provider;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.HistoricalRate;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.infrastructure.cache.CacheKeyGenerator;
import com.txnow.infrastructure.external.bok.ChartPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 환율 캐싱 (L1 캐시)
 */
@Slf4j
@RequiredArgsConstructor
public class CachedExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;


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

        // Redis에 없으면 delegate(DB → API)에서 조회
        BigDecimal rate = delegate.getCurrentExchangeRate(currency);

        // Redis에 캐싱
        saveToRedisCache(currency, rate);

        return rate;
    }

    @Override
    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        // 통화 간 환율은 항상 현재 환율 기반으로 계산
        if (fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Currencies must not be null");
        }

        if (fromCurrency == toCurrency) {
            return BigDecimal.ONE;
        }

        BigDecimal fromRate = getCurrentExchangeRate(fromCurrency);

        if (toCurrency == Currency.KRW) {
            return fromRate;
        }

        BigDecimal toRate = getCurrentExchangeRate(toCurrency);
        return fromRate.divide(toRate, 6, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public List<HistoricalRate> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        // 히스토리 데이터는 캐싱하지 않음 (별도의 @Cacheable로 처리 가능)
        return delegate.getExchangeRateHistory(currency, period);
    }

    /**
     * Redis에서 환율 조회
     */
    private BigDecimal getFromRedisCache(Currency currency) {
        try {
            String cacheKey = cacheKeyGenerator.exchangeRateKey(currency.name());
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if (cached instanceof BigDecimal) {
                    return (BigDecimal) cached;
                } else if (cached instanceof Number) {
                    return new BigDecimal(cached.toString());
                }
            }
        } catch (Exception e) {
            log.error("Redis error while fetching {}: {}", currency, e.getMessage());
        }
        return null;
    }

    /**
     * Redis에 환율 저장 (24시간 TTL)
     */
    private void saveToRedisCache(Currency currency, BigDecimal rate) {
        try {
            String cacheKey = cacheKeyGenerator.exchangeRateKey(currency.name());
            redisTemplate.opsForValue().set(cacheKey, rate, 24, TimeUnit.HOURS);
            log.debug("Saved to Redis: {} = {}", currency, rate);

            // Stale 데이터도 저장 (Fallback용, 7일 TTL)
            String staleKey = cacheKeyGenerator.staleDataKey(currency.name());
            redisTemplate.opsForValue().set(staleKey, rate, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Failed to save to Redis: {}", currency, e);
        }
    }
}
