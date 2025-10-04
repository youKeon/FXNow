package com.txnow.infrastructure.cache;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.infrastructure.provider.DatabaseExchangeRateProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {

    private final DatabaseExchangeRateProvider databaseProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    @Value("${cache.exchange-rate.ttl-seconds:86400}")
    private long ttlSeconds;

    // 주요 통화 목록
    private static final List<Currency> MAJOR_CURRENCIES = List.of(
        Currency.USD, Currency.EUR, Currency.JPY, Currency.CNY
    );

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========================================");
        log.info("Starting cache warmup...");
        log.info("========================================");

        if (isExchangeRateWarmupAlreadyDone()) {
            log.info("Cache already primed");
            return;
        }

        // 환율 데이터 워밍업 (현재 환율만)
        warmupExchangeRates();

        log.info("Cache warmup completed");
    }

    /**
     * 주요 통화 환율 워밍업 (DB → Redis)
     * 차트 데이터는 워밍업하지 않음 (날짜 범위가 유동적이므로 DB에서 직접 조회)
     */
    private void warmupExchangeRates() {
        for (Currency currency : MAJOR_CURRENCIES) {
            try {
                BigDecimal rate = databaseProvider.getCurrentExchangeRate(currency);
                String cacheKey = cacheKeyGenerator.exchangeRateKey(currency.name());

                redisTemplate.opsForValue().set(cacheKey, rate, ttlSeconds, TimeUnit.SECONDS);
                log.debug("Warmed up exchange rate: {} = {}", currency, rate);
            } catch (Exception e) {
                log.warn("Failed to warmup exchange rate for {}: {}", currency, e.getMessage());
            }
        }
    }

    private boolean isExchangeRateWarmupAlreadyDone() {
        return MAJOR_CURRENCIES.stream()
            .map(currency -> cacheKeyGenerator.exchangeRateKey(currency.name()))
            .allMatch(this::isKeyPresent);
    }

    private boolean isKeyPresent(String key) {
        return redisTemplate.hasKey(key);
    }
}
