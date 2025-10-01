package com.txnow.infrastructure.cache;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.domain.exchange.model.ChartPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {

    private final ExchangeRateProvider exchangeRateProvider;

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

        for (Currency currency : MAJOR_CURRENCIES) {
            BigDecimal rate = exchangeRateProvider.getCurrentExchangeRate(currency);
            log.debug("Warmed up: {} = {}", currency, rate);

            // Rate Limit 방지
            Thread.sleep(100);
        }

        log.info("Exchange rates warmup completed");
    }

    /**
     * 인기 차트 데이터 워밍업
     */
    private void warmupChartData() throws InterruptedException {
        log.info("Warming up chart data...");

        // USD와 EUR만 차트 워밍업 (가장 많이 조회되는 통화)
        List<Currency> chartCurrencies = List.of(Currency.USD, Currency.EUR);

        for (Currency currency : chartCurrencies) {
            for (ChartPeriod period : CHART_PERIODS) {
                var chartData = exchangeRateProvider.getExchangeRateHistory(currency, period);
                log.debug("Warmed up chart: {} - {} ({} points)",
                        currency, period.getCode(), chartData.size());

                // Rate Limit 방지: 요청 간 200ms 대기
                Thread.sleep(200);
            }
        }

        log.info("Chart data warmup completed");
    }
}