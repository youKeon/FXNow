package com.txnow.infrastructure.cache;

import com.txnow.domain.exchange.model.ChartPeriod;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.DailyRate;
import com.txnow.infrastructure.provider.DatabaseExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;

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

    // 차트 조회 주기
    private static final List<ChartPeriod> CHART_PERIODS = List.of(
        ChartPeriod.ONE_DAY,
        ChartPeriod.ONE_WEEK,
        ChartPeriod.ONE_MONTH
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

        // 1. 환율 데이터 워밍업
        warmupExchangeRates();

        // 2. 차트 데이터 워밍업
        warmupChartData();
        log.info("Cache warmup completed");
    }

    /**
     * 주요 통화 환율 워밍업 (DB → Redis)
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

    /**
     * 인기 차트 데이터 워밍업 (DB → Redis)
     */
    private void warmupChartData() {
        log.info("Warming up chart data...");

        // USD와 EUR만 차트 워밍업 (가장 많이 조회되는 통화)
        List<Currency> chartCurrencies = List.of(Currency.USD, Currency.EUR);

        for (Currency currency : chartCurrencies) {
            for (ChartPeriod period : CHART_PERIODS) {
                try {
                    List<DailyRate> chartData = databaseProvider.getExchangeRateHistory(currency, period);
                    String cacheKey = cacheKeyGenerator.exchangeRateHistoryKey(
                        currency.name(), period.getCode());

                    redisTemplate.opsForValue().set(cacheKey, chartData, ttlSeconds, TimeUnit.SECONDS);
                    log.debug("Warmed up chart: {} - {} ({} points)",
                        currency, period.getCode(), chartData.size());
                } catch (Exception e) {
                    log.warn("Failed to warmup chart for {} - {}: {}",
                        currency, period.getCode(), e.getMessage());
                }
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
