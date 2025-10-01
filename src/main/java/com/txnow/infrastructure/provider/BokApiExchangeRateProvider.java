package com.txnow.infrastructure.provider;

import com.txnow.domain.exchange.exception.ExchangeRateNotFoundException;
import com.txnow.domain.exchange.exception.ExchangeRateUnavailableException;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.HistoricalRate;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.infrastructure.external.bok.BokApiClient;
import com.txnow.infrastructure.external.bok.ChartPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * BOK(한국은행) API를 통한 환율 데이터 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BokApiExchangeRateProvider implements ExchangeRateProvider {

    private final BokApiClient bokApiClient;

    @Override
    public BigDecimal getCurrentExchangeRate(Currency currency) {
        if (currency == Currency.KRW) {
            return BigDecimal.ONE;
        }

        if (!currency.isBokSupported()) {
            throw new ExchangeRateNotFoundException(currency, "Currency not supported by BOK API");
        }

        String bokCode = currency.getBokCode();
        log.info("Fetching current rate from BOK API: {}", currency);

        var response = bokApiClient.getTodayExchangeRate(bokCode);
        if (response.isEmpty()) {
            throw new ExchangeRateUnavailableException(currency, "No data received from BOK API");
        }

        var statisticSearch = response.get().statisticSearch();
        if (statisticSearch == null || statisticSearch.rows() == null || statisticSearch.rows().isEmpty()) {
            throw new ExchangeRateNotFoundException(currency, "Empty data from BOK API");
        }

        // 최신 데이터 (첫 번째 row)
        var latestRow = statisticSearch.rows().getFirst();
        BigDecimal currentRate = new BigDecimal(latestRow.dataValue());

        // JPY는 100엔 기준이므로 1엔당으로 변환
        if (currency == Currency.JPY) {
            currentRate = currentRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }

        log.info("Fetched rate from BOK API: {} = {}", currency, currentRate);
        return currentRate;
    }

    @Override
    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
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
        return fromRate.divide(toRate, 6, RoundingMode.HALF_UP);
    }

    @Override
    public List<HistoricalRate> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        if (!currency.isBokSupported()) {
            throw new ExchangeRateNotFoundException(currency, "BOK code not found for currency");
        }

        String bokCode = currency.getBokCode();
        log.info("Fetching history from BOK API: {} period: {}", currency, period);

        var response = bokApiClient.getExchangeRateHistory(bokCode, period);
        if (response.isEmpty()) {
            throw new ExchangeRateUnavailableException(currency, "No history data received from BOK API");
        }

        var statisticSearch = response.get().statisticSearch();
        if (statisticSearch == null || statisticSearch.rows() == null || statisticSearch.rows().isEmpty()) {
            throw new ExchangeRateNotFoundException(currency, "Empty history data from BOK API");
        }

        // BokApiResponse를 Domain 타입(HistoricalRate)으로 변환
        List<HistoricalRate> historicalRates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (var row : statisticSearch.rows()) {
            try {
                LocalDate date = LocalDate.parse(row.time(), formatter);
                BigDecimal rate = new BigDecimal(row.dataValue());

                // JPY는 100엔 기준이므로 1엔당으로 변환
                if (currency == Currency.JPY) {
                    rate = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                }

                historicalRates.add(new HistoricalRate(date, rate));
            } catch (Exception e) {
                log.warn("Failed to parse history row for {}: time={}, value={}",
                    currency, row.time(), row.dataValue(), e);
            }
        }

        if (historicalRates.isEmpty()) {
            throw new ExchangeRateNotFoundException(currency, "No valid history data parsed");
        }

        // 날짜 오름차순 정렬
        historicalRates.sort((a, b) -> a.date().compareTo(b.date()));

        log.info("Fetched {} history data points from BOK API: {}", historicalRates.size(), currency);
        return historicalRates;
    }
}
