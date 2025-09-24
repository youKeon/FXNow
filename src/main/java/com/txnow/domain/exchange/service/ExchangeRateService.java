package com.txnow.domain.exchange.service;

import com.txnow.domain.exchange.model.*;
import com.txnow.domain.exchange.repository.ExchangeRateRepository;

import java.time.LocalDateTime;

public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    public ConversionResult convert(ConversionRequest request) {
        CurrencyPair currencyPair = request.toCurrencyPair();

        ExchangeRate exchangeRate = exchangeRateRepository.findLatestByCurrencyPair(currencyPair)
            .orElseThrow(() -> new IllegalArgumentException(
                "Exchange rate not found for " + currencyPair.getPairCode())
            );

        var convertedAmount = exchangeRate.convert(request.amount());
        return new ConversionResult(
            request.amount(),
            request.fromCurrency(),
            convertedAmount,
            request.toCurrency(),
            exchangeRate.rate(),
            LocalDateTime.now()
        );
    }

    public ExchangeRate getLatestRate(CurrencyPair currencyPair) {
        return exchangeRateRepository.findLatestByCurrencyPair(currencyPair)
            .orElseThrow(() -> new IllegalArgumentException(
                "Exchange rate not found for " + currencyPair.getPairCode())
            );
    }
}