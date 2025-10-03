package com.txnow.infrastructure.provider;

import com.txnow.domain.exchange.exception.ExchangeRateNotFoundException;
import com.txnow.domain.exchange.model.ChartPeriod;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.DailyRate;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.infrastructure.external.bok.BokApiClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

        if (!currency.isSupportedCurrency()) {
            throw new ExchangeRateNotFoundException(currency, "Currency not supported by BOK API");
        }

        String bokCode = currency.getBokCode();

        var response = bokApiClient.getTodayExchangeRate(bokCode);
        var statisticSearch = response.statisticSearch();

        var row = statisticSearch.rows().getFirst();
        BigDecimal currentRate = new BigDecimal(row.dataValue());

        return currency.normalizeFromBokApi(currentRate);
    }

    @Override
    public List<DailyRate> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        if (!currency.isSupportedCurrency()) {
            throw new ExchangeRateNotFoundException(currency, "BOK code not found for currency");
        }

        String bokCode = currency.getBokCode();
        log.info("Fetching history from BOK API: {} period: {}", currency, period);

        var response = bokApiClient.getExchangeRate(bokCode, period);

        var statisticSearch = response.statisticSearch();

        List<DailyRate> dailyRates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (var row : statisticSearch.rows()) {
            LocalDate date = LocalDate.parse(row.time(), formatter);
            BigDecimal rate = new BigDecimal(row.dataValue());
            rate = currency.normalizeFromBokApi(rate);

            dailyRates.add(new DailyRate(date, rate));
        }

        if (dailyRates.isEmpty()) {
            throw new ExchangeRateNotFoundException(currency, "No valid history data parsed");
        }

        dailyRates.sort(Comparator.comparing(DailyRate::date));

        return dailyRates;
    }
}
