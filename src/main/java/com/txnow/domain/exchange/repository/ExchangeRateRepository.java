package com.txnow.domain.exchange.repository;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.CurrentExchangeRateData;

import java.util.Optional;

public interface ExchangeRateRepository {

    Optional<CurrentExchangeRateData> findCurrentRates(Currency baseCurrency);
}