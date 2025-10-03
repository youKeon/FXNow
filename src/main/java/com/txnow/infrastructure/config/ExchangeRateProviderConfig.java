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

@Configuration
@RequiredArgsConstructor
public class ExchangeRateProviderConfig {

    private final BokApiExchangeRateProvider bokApiProvider;
    private final ExchangeRateHistoryRepository historyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    /**
     * L2 Cache: Database
     */
    @Bean
    public DatabaseExchangeRateProvider databaseExchangeRateProvider() {
        return new DatabaseExchangeRateProvider(
            bokApiProvider,
            historyRepository
        );
    }

    /**
     * L1 Cache: Redis
     */
    @Bean
    @Primary
    public ExchangeRateProvider exchangeRateProvider(
        DatabaseExchangeRateProvider databaseProvider
    ) {
        return new CachedExchangeRateProvider(
            databaseProvider,
            redisTemplate,
            cacheKeyGenerator
        );
    }
}
