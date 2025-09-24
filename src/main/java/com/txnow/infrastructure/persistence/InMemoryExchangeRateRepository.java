package com.txnow.infrastructure.persistence;

import com.txnow.domain.exchange.model.CurrencyPair;
import com.txnow.domain.exchange.model.ExchangeRate;
import com.txnow.domain.exchange.repository.ExchangeRateRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryExchangeRateRepository implements ExchangeRateRepository {

    private final ConcurrentMap<String, ExchangeRate> exchangeRates = new ConcurrentHashMap<>();

    @Override
    public Optional<ExchangeRate> findLatestByCurrencyPair(CurrencyPair currencyPair) {
        ExchangeRate rate = exchangeRates.get(currencyPair.getPairCode());
        return Optional.ofNullable(rate);
    }

    @Override
    public List<ExchangeRate> findByCurrencyPairBetween(
        CurrencyPair currencyPair,
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {
        ExchangeRate rate = exchangeRates.get(currencyPair.getPairCode());
        if (rate != null &&
            rate.timestamp().isAfter(startTime) &&
            rate.timestamp().isBefore(endTime)) {
            return List.of(rate);
        }
        return List.of();
    }

    @Override
    public ExchangeRate save(ExchangeRate exchangeRate) {
        exchangeRates.put(exchangeRate.currencyPair().getPairCode(), exchangeRate);
        return exchangeRate;
    }

    @Override
    public List<ExchangeRate> saveAll(List<ExchangeRate> exchangeRates) {
        return exchangeRates.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }
}