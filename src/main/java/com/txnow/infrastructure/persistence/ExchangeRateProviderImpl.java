package com.txnow.infrastructure.persistence;

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
import java.util.Optional;

/**
 * 환율 제공자 구현체
 * Infrastructure Layer에서 Domain의 ExchangeRateProvider 인터페이스를 구현
 * BOK API를 직접 호출하여 환율 데이터를 제공합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateProviderImpl implements ExchangeRateProvider {

    private final BokApiClient bokApiClient;

    @Override
    public Optional<BigDecimal> getCurrentExchangeRate(Currency currency) {
        if (currency == Currency.KRW) {
            return Optional.of(BigDecimal.ONE);
        }

        if (!currency.isBokSupported()) {
            return Optional.empty();
        }

        String bokCode = currency.getBokCode();

        var response = bokApiClient.getTodayExchangeRate(bokCode);

        if (response.isEmpty()) {
            log.warn("No exchange rate data received for currency: {}", currency);
            return Optional.empty();
        }

        var statisticSearch = response.get().statisticSearch();
        if (statisticSearch == null || statisticSearch.rows() == null || statisticSearch.rows().isEmpty()) {
            log.warn("Empty exchange rate data for currency: {}", currency);
            return Optional.empty();
        }

        // 최신 데이터 (첫 번째 row)
        var latestRow = statisticSearch.rows().getFirst();
        BigDecimal currentRate = new BigDecimal(latestRow.dataValue());

        // JPY는 100엔 기준이므로 1엔당으로 변환
        if (currency == Currency.JPY) {
            currentRate = currentRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        }

        log.debug("Retrieved current exchange rate for {} -> KRW: {}", currency, currentRate);
        return Optional.of(currentRate);
    }

    @Override
    public Optional<BigDecimal> getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            log.warn("Currencies must not be null");
            return Optional.empty();
        }

        if (fromCurrency == toCurrency) {
            return Optional.of(BigDecimal.ONE);
        }

        Optional<BigDecimal> fromRateOpt = getCurrentExchangeRate(fromCurrency);
        if (fromRateOpt.isEmpty()) {
            log.warn("Exchange rate not available for currency: {}", fromCurrency);
            return Optional.empty();
        }

        BigDecimal fromRate = fromRateOpt.get();

        if (toCurrency == Currency.KRW) {
            return Optional.of(fromRate);
        }

        Optional<BigDecimal> toRateOpt = getCurrentExchangeRate(toCurrency);
        if (toRateOpt.isEmpty()) {
            log.warn("Exchange rate not available for currency: {}", toCurrency);
            return Optional.empty();
        }

        BigDecimal toRate = toRateOpt.get();
        BigDecimal exchangeRate = fromRate.divide(toRate, 6, RoundingMode.HALF_UP);

        return Optional.of(exchangeRate);
    }

    @Override
    public Optional<List<HistoricalRate>> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        if (!currency.isBokSupported()) {
            log.warn("BOK code not found for currency: {}", currency);
            return Optional.empty();
        }

        String bokCode = currency.getBokCode();

        log.info("Fetching history data for {} period: {}", currency, period);

        var response = bokApiClient.getExchangeRateHistory(bokCode, period);
        if (response.isEmpty()) {
            log.warn("No history data received for currency: {} period: {}", currency, period);
            return Optional.empty();
        }

        var statisticSearch = response.get().statisticSearch();
        if (statisticSearch == null || statisticSearch.rows() == null || statisticSearch.rows().isEmpty()) {
            log.warn("Empty history data for currency: {} period: {}", currency, period);
            return Optional.empty();
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
            log.warn("No valid history data parsed for currency: {} period: {}", currency, period);
            return Optional.empty();
        }

        // 날짜 오름차순 정렬
        historicalRates.sort((a, b) -> a.date().compareTo(b.date()));

        log.debug("Retrieved {} history data points for {}", historicalRates.size(), currency);
        return Optional.of(historicalRates);
    }
}
