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

@Slf4j
@RequiredArgsConstructor
public class CachedExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    @Value("${cache.exchange-rate.ttl-seconds:86400}")
    private long ttlSeconds;


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
        // L1: Redis 캐시 조회
        List<DailyRate> cachedHistory = getHistoryFromRedisCache(currency, period);
        if (cachedHistory != null && !cachedHistory.isEmpty()) {
            log.debug("Cache HIT (Redis): {} - {}", currency, period.getCode());
            return cachedHistory;
        }

        log.debug("Cache MISS (Redis): {} - {}", currency, period.getCode());

        // Redis에 없으면 DB에서 조회
        List<DailyRate> history = delegate.getExchangeRateHistory(currency, period);

        // Redis에 캐싱
        saveHistoryToRedisCache(currency, period, history);

        return history;
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
     * Redis에 환율 저장
     */
    private void saveToRedisCache(Currency currency, BigDecimal rate) {
        String cacheKey = cacheKeyGenerator.exchangeRateKey(currency.name());
        redisTemplate.opsForValue().set(cacheKey, rate, ttlSeconds, TimeUnit.SECONDS);
        log.debug("Saved to Redis: {} = {} (TTL: {}s)", currency, rate, ttlSeconds);
    }

    /**
     * Redis에서 차트 데이터 조회
     */
    private List<DailyRate> getHistoryFromRedisCache(Currency currency, ChartPeriod period) {
        String cacheKey = cacheKeyGenerator.exchangeRateHistoryKey(currency.name(), period.getCode());

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                return (List<DailyRate>) cached;
            }
        } catch (Exception e) {
            // 역직렬화 실패 시 (타입 정보 없는 구 데이터) 캐시 삭제 후 null 반환
            redisTemplate.delete(cacheKey);
        }

        return null;
    }

    /**
     * Redis에 차트 데이터 저장
     */
    private void saveHistoryToRedisCache(Currency currency, ChartPeriod period, List<DailyRate> history) {
        String cacheKey = cacheKeyGenerator.exchangeRateHistoryKey(currency.name(), period.getCode());
        redisTemplate.opsForValue().set(cacheKey, history, ttlSeconds, TimeUnit.SECONDS);
        log.debug("Saved history to Redis: {} - {} ({} points, TTL: {}s)",
            currency, period.getCode(), history.size(), ttlSeconds);
    }
}
