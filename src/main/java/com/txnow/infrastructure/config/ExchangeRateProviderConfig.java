package com.txnow.infrastructure.config;

import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import com.txnow.infrastructure.cache.CacheKeyGenerator;
import com.txnow.infrastructure.external.bok.BokApiClient;
import com.txnow.infrastructure.provider.CachedExchangeRateProvider;
import com.txnow.infrastructure.provider.DatabaseExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * ExchangeRateProvider Decorator Chain 구성
 * Redis (L1) → Database (L2) → BOK API (L3)
 */
@Configuration
@RequiredArgsConstructor
public class ExchangeRateProviderConfig {

    private final BokApiClient bokApiClient;
    private final ExchangeRateHistoryRepository historyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    /**
     * L2 Cache: Database
     */
    @Bean
    public DatabaseExchangeRateProvider databaseExchangeRateProvider() {
        return new DatabaseExchangeRateProvider(
            bokApiClient,
            historyRepository
        );
    }

    /**
     * L1 Cache: Redis (Primary)
     */
    @Bean
    @Primary
    public CachedExchangeRateProvider cachedExchangeRateProvider(
        DatabaseExchangeRateProvider databaseProvider
    ) {
        return new CachedExchangeRateProvider(
            databaseProvider,
            redisTemplate,
            cacheKeyGenerator
        );
    }
}
