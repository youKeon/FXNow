package com.txnow.infrastructure.provider;

import com.txnow.domain.exchange.exception.ExchangeRateNotFoundException;
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
        // 1. 오늘 환울 확인
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        ExchangeRateHistory todayData = historyRepository
            .findExchangeRateByTimestamp(currency, today, today.plusDays(1));

        if (todayData != null) {
            log.debug("Cache HIT (DB - Today): {}", currency);
            return todayData.getRate();
        }

        log.debug("Cache MISS (DB - Today): {}", currency);

        // 2. 한국은행 API 호출
        BigDecimal rate = delegate.getCurrentExchangeRate(currency);

        // 3. API 성공 시 DB 저장
        if (rate != null) {
            var history = ExchangeRateHistory.builder()
                .currency(currency)
                .rate(rate)
                .change(BigDecimal.ZERO)
                .timestamp(LocalDateTime.now())
                .build();

            historyRepository.save(history);
            return rate;
        }

        // 4. API가 null 반환 (공휴일, 주말 등) → 최근 7일 내 데이터 사용
        log.warn("BOK API returned null for {}. Trying to use recent data from DB.", currency);
        LocalDateTime weekAgo = today.minusDays(7);
        ExchangeRateHistory recentData = historyRepository
            .findExchangeRateByTimestamp(currency, weekAgo, today.plusDays(1));

        if (recentData != null) {
            log.info("Using recent data from DB for {} (공휴일 대응): timestamp={}",
                currency, recentData.getTimestamp());
            return recentData.getRate();
        }

        // 5. DB에도 데이터 없음 → 예외 발생
        throw new ExchangeRateNotFoundException(
            currency, "No data available from API and DB");
    }

    @Override
    public List<DailyRate> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        LocalDateTime startTime = period.getStartDate().atStartOfDay();
        LocalDateTime endTime = period.getEndDate().atTime(23, 59, 59);

        List<ExchangeRateHistory> historyList = historyRepository
            .findByCurrencyAndTimestampBetween(currency, startTime, endTime);

        if (historyList.isEmpty()) {
            // DB에 데이터 없으면 API 호출
            return delegate.getExchangeRateHistory(currency, period);
        }

        List<DailyRate> dailyRates = convertToDailyRates(historyList);
        log.debug("Cache HIT (DB - Chart): {} - {} ({} days)",
            currency, period.getCode(), dailyRates.size());
        return dailyRates;
    }

    /**
     * 30분 단위 데이터를 일별로 집계 (하루의 마지막 데이터 사용)
     */
    private List<DailyRate> convertToDailyRates(List<ExchangeRateHistory> historyList) {
        return historyList.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                h -> h.getTimestamp().toLocalDate(),
                java.util.stream.Collectors.maxBy(
                    java.util.Comparator.comparing(ExchangeRateHistory::getTimestamp)
                )
            ))
            .entrySet().stream()
            .filter(e -> e.getValue().isPresent())
            .map(e -> new DailyRate(e.getKey(), e.getValue().get().getRate()))
            .sorted(java.util.Comparator.comparing(DailyRate::date))
            .toList();
    }
}
