package com.txnow.infrastructure.config;

import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import com.txnow.infrastructure.cache.CacheKeyGenerator;
import com.txnow.infrastructure.provider.BokApiExchangeRateProvider;
import com.txnow.infrastructure.provider.CachedExchangeRateProvider;
import com.txnow.infrastructure.provider.DatabaseExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * ExchangeRateProvider Decorator Chain 설정
 *
 * 구조:
 * CachedExchangeRateProvider (L1: Redis)
 *   → DatabaseExchangeRateProvider (L2: DB)
 *     → BokApiExchangeRateProvider (L3: API)
 */
@Configuration
@RequiredArgsConstructor
public class ExchangeRateProviderConfig {

    private final BokApiExchangeRateProvider bokApiProvider;
    private final ExchangeRateHistoryRepository historyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    @Bean
    @Primary
    public ExchangeRateProvider exchangeRateProvider() {
        ExchangeRateProvider dbProvider = new DatabaseExchangeRateProvider(
            bokApiProvider,
            historyRepository
        );

        return new CachedExchangeRateProvider(
            dbProvider,
            redisTemplate,
            cacheKeyGenerator
        );
    }
}
