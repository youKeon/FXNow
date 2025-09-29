package com.txnow.infrastructure.persistence;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.infrastructure.external.bok.BokApiClient;
import com.txnow.infrastructure.external.bok.BokApiResponse;
import com.txnow.infrastructure.external.bok.ChartPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency == null || toCurrency == null) {
            throw new IllegalArgumentException("Currencies must not be null");
        }

        if (fromCurrency == toCurrency) {
            return BigDecimal.ONE;
        }

        BigDecimal fromRate = getCurrentExchangeRate(fromCurrency)
            .orElseThrow(() -> new IllegalArgumentException("Exchange rate not available for currency: " + fromCurrency));

        if (toCurrency == Currency.KRW) {
            return fromRate;
        }

        BigDecimal toRate = getCurrentExchangeRate(toCurrency)
            .orElseThrow(() -> new IllegalArgumentException("Exchange rate not available for currency: " + toCurrency));

        return fromRate.divide(toRate, 6, RoundingMode.HALF_UP);
    }

    @Override
    public Optional<BokApiResponse> getExchangeRateHistory(Currency currency, ChartPeriod period) {
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

        log.debug("Retrieved {} history data points for {}", statisticSearch.rows().size(), currency);
        return response;
    }
}
