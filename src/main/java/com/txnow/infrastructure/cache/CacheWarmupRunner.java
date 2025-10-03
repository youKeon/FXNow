package com.txnow.infrastructure.cache;

import com.txnow.domain.exchange.model.ChartPeriod;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
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

    private final ExchangeRateProvider exchangeRateProvider;
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

        long startTime = System.currentTimeMillis();

        // 1. 환율 데이터 워밍업
        warmupExchangeRates();

        // 2. 차트 데이터 워밍업
        warmupChartData();

        long duration = System.currentTimeMillis() - startTime;

        log.info("========================================");
        log.info("Cache warmup completed in {}ms", duration);
        log.info("========================================");
    }

    /**
     * 주요 통화 환율 워밍업
     */
    private void warmupExchangeRates() throws InterruptedException {
        log.info("Warming up exchange rates...");

        int successCount = 0;
        int failCount = 0;

        for (Currency currency : MAJOR_CURRENCIES) {
            try {
                BigDecimal rate = exchangeRateProvider.getCurrentExchangeRate(currency);
                log.debug("Warmed up: {} = {}", currency, rate);
                successCount++;
            } catch (Exception e) {
                log.warn("Failed to warm up {}: {}", currency, e.getMessage());
                failCount++;
            }

            // Rate Limit 방지
            Thread.sleep(100);
        }

        log.info("Exchange rates warmup completed (Success: {}, Failed: {})", successCount, failCount);
    }

    /**
     * 인기 차트 데이터 워밍업
     */
    private void warmupChartData() throws InterruptedException {
        log.info("Warming up chart data...");

        // USD와 EUR만 차트 워밍업 (가장 많이 조회되는 통화)
        List<Currency> chartCurrencies = List.of(Currency.USD, Currency.EUR);

        int successCount = 0;
        int failCount = 0;

        for (Currency currency : chartCurrencies) {
            for (ChartPeriod period : CHART_PERIODS) {
                try {
                    var chartData = exchangeRateProvider.getExchangeRateHistory(currency, period);
                    log.debug("Warmed up chart: {} - {} ({} points)",
                            currency, period.getCode(), chartData.size());
                    successCount++;
                } catch (Exception e) {
                    log.warn("Failed to warm up chart {} - {}: {}", currency, period.getCode(), e.getMessage());
                    failCount++;
                }

                // Rate Limit 방지: 요청 간 200ms 대기
                Thread.sleep(200);
            }
        }

        log.info("Chart data warmup completed (Success: {}, Failed: {})", successCount, failCount);
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
