package com.txnow.infrastructure.provider;

import com.txnow.domain.exchange.model.ChartPeriod;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateHistory;
import com.txnow.domain.exchange.model.DailyRate;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import com.txnow.domain.exchange.repository.ExchangeRateHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * L2 Cache: Database
 */
@Slf4j
@RequiredArgsConstructor
public class DatabaseExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final ExchangeRateHistoryRepository historyRepository;

    @Override
    public BigDecimal getCurrentExchangeRate(Currency currency) {

        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        ExchangeRateHistory latest = historyRepository
            .findExchangeRateByTimestamp(currency, start, end);

        if (latest != null) {
            log.debug("Cache HIT (DB): {}", currency);
            return latest.getRate();
        }

        log.debug("Cache MISS (DB): {}", currency);

        // DB에 없으면 API 호출
        BigDecimal rate = delegate.getCurrentExchangeRate(currency);

        // DB에 저장
        if (rate != null) {
            var history = ExchangeRateHistory.builder()
                .currency(currency)
                .rate(rate)
                .change(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .build();

            historyRepository.save(history);
        }

        return rate;
    }

    @Override
    public List<DailyRate> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        // 일별 데이터는 항상 API에서 조회 (DB에는 30분 단위 데이터만 있음)
        return delegate.getExchangeRateHistory(currency, period);
    }
}
