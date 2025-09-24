package com.txnow.domain.exchange.repository;

import com.txnow.domain.exchange.model.CurrencyPair;
import com.txnow.domain.exchange.model.ExchangeRate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository {

    Optional<ExchangeRate> findLatestByCurrencyPair(CurrencyPair currencyPair);

    List<ExchangeRate> findByCurrencyPairBetween(
        CurrencyPair currencyPair,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    ExchangeRate save(ExchangeRate exchangeRate);

    List<ExchangeRate> saveAll(List<ExchangeRate> exchangeRates);
}