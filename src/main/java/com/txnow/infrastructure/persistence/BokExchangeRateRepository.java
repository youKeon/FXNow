package com.txnow.infrastructure.persistence;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.CurrentExchangeRate;
import com.txnow.domain.exchange.model.CurrentExchangeRateData;
import com.txnow.domain.exchange.repository.ExchangeRateRepository;
import com.txnow.infrastructure.external.bok.BokApiClient;
import com.txnow.infrastructure.external.bok.BokCurrencyMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class BokExchangeRateRepository implements ExchangeRateRepository {

    private final BokApiClient bokApiClient;

    @Override
    public Optional<CurrentExchangeRateData> findCurrentRates(Currency baseCurrency) {
        LocalDateTime now = LocalDateTime.now();

        Map<Currency, CurrentExchangeRate> rates = Arrays.stream(BokCurrencyMapping.getSupportedCurrencies())
            .filter(currency -> currency != Currency.KRW)
            .collect(Collectors.toMap(
                currency -> currency,
                currency -> createCurrentExchangeRate(currency, now)
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));

        if (rates.isEmpty()) {
            log.warn("No exchange rate data available from BOK API");
            return Optional.empty();
        }

        return Optional.of(new CurrentExchangeRateData(Currency.KRW, now, rates));
    }

    private CurrentExchangeRate createCurrentExchangeRate(Currency currency, LocalDateTime timestamp) {
        String bokCode = BokCurrencyMapping.getBokCode(currency);

        if (bokCode == null) {
            log.warn("Currency {} not supported by BOK API", currency);
            return null;
        }

        try {
            var response = bokApiClient.getRecentExchangeRate(bokCode);
            if (response.isEmpty()) {
                log.warn("No data returned from BOK API for currency: {}", currency);
                return createFallbackRate(currency, timestamp);
            }

            var statisticSearch = response.get().statisticSearch();
            if (statisticSearch == null || statisticSearch.rows() == null || statisticSearch.rows().isEmpty()) {
                log.warn("Empty data from BOK API for currency: {}", currency);
                return createFallbackRate(currency, timestamp);
            }

            var rows = statisticSearch.rows();

            var latestData = rows.getFirst();
            BigDecimal rate = new BigDecimal(latestData.dataValue());

            // JPY는 100엔 기준이므로 100으로 나눔
            if (currency == Currency.JPY) {
                rate = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            }

            // 실제 변동률 계산 (전일 대비)
            BigDecimal change = calculateRealChange(rows, rate, currency);

            return new CurrentExchangeRate(currency, rate, change, timestamp);

        } catch (Exception e) {
            log.error("Error creating exchange rate for currency: {}", currency, e);
            return createFallbackRate(currency, timestamp);
        }
    }

    private CurrentExchangeRate createFallbackRate(Currency currency, LocalDateTime timestamp) {
        // BOK API 실패 시 fallback 데이터
        BigDecimal rate = switch (currency) {
            case USD -> new BigDecimal("1320.50");
            case EUR -> new BigDecimal("1430.20");
            case JPY -> new BigDecimal("8.85");
            case CNY -> new BigDecimal("182.30");
            case GBP -> new BigDecimal("1675.80");
            default -> new BigDecimal("1.00");
        };

        BigDecimal change = BigDecimal.ZERO; // Fallback 시에는 변동률을 0으로 설정
        return new CurrentExchangeRate(currency, rate, change, timestamp);
    }

    /**
     * 실제 전일 대비 변동률을 계산합니다.
     *
     * @param rows BOK API에서 받은 환율 데이터 목록 (최신순)
     * @param currentRate 현재 환율
     * @param currency 통화
     * @return 전일 대비 변동률 (퍼센트)
     */
    private BigDecimal calculateRealChange(java.util.List<com.txnow.infrastructure.external.bok.BokApiResponse.ExchangeRateData> rows,
                                          BigDecimal currentRate, Currency currency) {
        try {
            // 최소 2개 이상의 데이터가 있어야 변동률 계산 가능
            if (rows.size() < 2) {
                log.debug("Insufficient data points for change calculation for currency: {}", currency);
                return BigDecimal.ZERO;
            }

            // 전일 데이터 (두 번째 항목)
            var previousData = rows.get(1);
            BigDecimal previousRate = new BigDecimal(previousData.dataValue());

            // JPY는 100엔 기준이므로 100으로 나눔
            if (currency == Currency.JPY) {
                previousRate = previousRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            }

            // 변동률 계산: ((현재값 - 이전값) / 이전값) * 100
            if (previousRate.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("Previous rate is zero for currency: {}, cannot calculate change", currency);
                return BigDecimal.ZERO;
            }

            BigDecimal change = currentRate.subtract(previousRate)
                .divide(previousRate, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);

            log.debug("Change calculated for {}: current={}, previous={}, change={}%",
                    currency, currentRate, previousRate, change);

            return change;

        } catch (Exception e) {
            log.error("Error calculating change for currency: {}", currency, e);
            return BigDecimal.ZERO;
        }
    }
}