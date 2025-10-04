package com.txnow.infrastructure.cache;

import com.txnow.domain.exchange.model.ChartPeriod;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.infrastructure.provider.CachedExchangeRateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {

    private final CachedExchangeRateProvider cachedExchangeRateProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

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
     * 주요 통화 환율 워밍업
     */
    private void warmupExchangeRates() {
        for (Currency currency : MAJOR_CURRENCIES) {
            BigDecimal rate = cachedExchangeRateProvider.getCurrentExchangeRate(currency);
            log.debug("Warmed up: {} = {}", currency, rate);
        }
    }

    /**
     * 인기 차트 데이터 워밍업
     */
    private void warmupChartData() {
        log.info("Warming up chart data...");

        // USD와 EUR만 차트 워밍업 (가장 많이 조회되는 통화)
        List<Currency> chartCurrencies = List.of(Currency.USD, Currency.EUR);

        for (Currency currency : chartCurrencies) {
            for (ChartPeriod period : CHART_PERIODS) {
                var chartData = cachedExchangeRateProvider.getExchangeRateHistory(currency, period);
                log.debug("Warmed up chart: {} - {} ({} points)",
                    currency, period.getCode(), chartData.size());
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
