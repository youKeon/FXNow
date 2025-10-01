package com.txnow.infrastructure.provider;

import static java.math.RoundingMode.*;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import com.txnow.domain.exchange.model.HistoricalRate;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import com.txnow.domain.exchange.model.ChartPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터베이스 기반 환율 제공 (L2 캐시)
 */
@Slf4j
@RequiredArgsConstructor
public class DatabaseExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final ExchangeRateHistoryRepository historyRepository;

    @Override
    public BigDecimal getCurrentExchangeRate(Currency currency) {
        if (currency == Currency.KRW) {
            return BigDecimal.ONE;
        }

        BigDecimal rateFromDb = getFromDatabase(currency);
        if (rateFromDb != null) {
            log.debug("Cache HIT (DB): {}", currency);
            return rateFromDb;
        }

        log.debug("Cache MISS (DB): {}", currency);

        // DB에 없으면 delegate
        BigDecimal rate = delegate.getCurrentExchangeRate(currency);

        // API에서 받아온 데이터를 DB에 저장
        saveToDatabase(currency, rate);

        return rate;
    }

    @Override
    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        // 통화 간 환율은 항상 현재 환율 기반으로 계산
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
        return fromRate.divide(toRate, 6, HALF_UP);
    }

    @Override
    public List<HistoricalRate> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        // 히스토리 데이터는 항상 API에서 조회 (DB에는 30분 단위 데이터만 있음)
        return delegate.getExchangeRateHistory(currency, period);
    }

    /**
     * DB에서 오늘 환율 조회
     */
    private BigDecimal getFromDatabase(Currency currency) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        ExchangeRateHistory latest = historyRepository
            .findLatestByCurrencyAndTimestampBetween(currency, startOfDay, endOfDay);

        return latest != null ? latest.getRate() : null;
    }

    /**
     * DB에 환율 저장 (히스토리 테이블)
     */
    private void saveToDatabase(Currency currency, BigDecimal rate) {
        ExchangeRateHistory recentHistory = historyRepository
            .findLatestByCurrencyAndTimestampBetween(
                currency,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now()
            );

        // 최근 5분 내 데이터가 없으면 저장
        if (recentHistory == null) {
            var newHistory = ExchangeRateHistory.builder()
                .currency(currency)
                .rate(rate)
                .change(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .build();
            historyRepository.save(newHistory);
            log.debug("Saved to DB: {} = {}", currency, rate);
        }
    }
}
