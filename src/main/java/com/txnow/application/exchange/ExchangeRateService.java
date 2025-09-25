package com.txnow.application.exchange;

import static com.txnow.application.exchange.dto.CurrentExchangeRateResult.RateInfo;

import com.txnow.application.exchange.dto.CurrentExchangeRateResult;
import com.txnow.application.exchange.dto.CurrencyPairRateResult;
import com.txnow.application.exchange.dto.ExchangeRateChartResult;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.repository.ExchangeRateRepository;
import com.txnow.infrastructure.external.bok.BokApiClient;
import com.txnow.infrastructure.external.bok.BokCurrencyMapping;
import com.txnow.infrastructure.external.bok.ChartPeriod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final BokApiClient bokApiClient;

    public Currency[] getSupportedCurrencies() {
        return Currency.values();
    }

    public CurrentExchangeRateResult getCurrentRates() {
        Currency baseCurrency = Currency.USD; // 기본 통화는 USD

        var currentRates = exchangeRateRepository.findCurrentRates(baseCurrency)
            .orElseThrow(
                () -> new RuntimeException("Current exchange rates not available")
            );

        Map<String, RateInfo> rates = currentRates.rates().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().getCode(),
                entry -> new RateInfo(
                    entry.getValue().rate(),
                    entry.getValue().change()
                )
            ));

        return new CurrentExchangeRateResult(
            currentRates.baseCurrency(),
            currentRates.lastUpdated(),
            rates
        );
    }

    /**
     * 특정 통화쌍의 환율 정보를 조회합니다. 현재는 외화 → KRW 형태만 지원합니다.
     */
    public CurrencyPairRateResult getCurrencyPairRate(Currency fromCurrency, Currency toCurrency) {
        // 현재는 KRW가 대상 통화인 경우만 지원
        Assert.isTrue(toCurrency == Currency.KRW, "Currently only supports conversion to KRW");

        // KRW가 기준 통화인 경우는 지원하지 않음 (외화 → KRW 전용)
        Assert.isTrue(fromCurrency != Currency.KRW, "KRW as base currency is not supported");

        // 현재 환율 데이터 조회
        var currentRates = exchangeRateRepository.findCurrentRates(Currency.KRW)
            .orElseThrow(
                () -> new RuntimeException("Exchange rate data not available")
            );

        // 특정 통화의 환율 정보 추출
        var rateData = currentRates.rates().get(fromCurrency);
        Assert.notNull(rateData, "Exchange rate not available for currency: " + fromCurrency);

        return new CurrencyPairRateResult(
            fromCurrency,
            toCurrency,
            rateData.rate(),
            rateData.change(), // 이미 %로 계산된 변동률
            rateData.change(), // changePercent와 change가 동일 (이미 %로 계산됨)
            currentRates.lastUpdated()
        );
    }

    public ExchangeRateChartResult getExchangeRateChart(Currency baseCurrency,
        Currency targetCurrency, String periodCode) {
        // targetCurrency는 항상 KRW여야 함
        Assert.isTrue(targetCurrency == Currency.KRW, "Target currency must be KRW");
        ChartPeriod period = ChartPeriod.fromCode(periodCode);

        // BOK API에서 baseCurrency에 대한 BOK 코드 조회 (예: USD → "0000001")
        String bokCode = BokCurrencyMapping.getBokCode(baseCurrency);
        Assert.notNull(bokCode, "Unsupported base currency: " + baseCurrency);

        log.info("Fetching chart data for {}/{} period: {}", baseCurrency, targetCurrency,
            periodCode);

        // BOK API에서 히스토리 데이터 조회
        var response = bokApiClient.getExchangeRateHistory(bokCode, period);
        Assert.isTrue(response.isPresent(),
            "Chart data not available for " + baseCurrency + "/" + targetCurrency);

        var statisticSearch = response.get().statisticSearch();
        Assert.isTrue(
            statisticSearch != null && statisticSearch.rows() != null && !statisticSearch.rows()
                .isEmpty(),
            "Empty chart data from BOK API");

        var rows = statisticSearch.rows();

        // 최신 데이터부터 정렬 (BOK API는 최신순으로 반환)
        List<ExchangeRateChartResult.ChartDataPoint> chartData = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();

        for (var row : rows) {
            BigDecimal rate = new BigDecimal(row.dataValue());

            // JPY는 100엔 기준이므로 100으로 나누어 1엔당 원화로 변환
            if (baseCurrency == Currency.JPY) {
                rate = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            }

            // BOK API는 이미 KRW 기준 데이터를 제공하므로 추가 변환 불필요

            rates.add(rate);

            // 차트 데이터 포인트 생성
            String dateStr = row.time(); // YYYYMMDD 형식
            String formattedDate = formatDate(dateStr);

            // 이전 데이터와 비교하여 변동률 계산
            BigDecimal dayChange = calculateDayChange(rates, rates.size() - 1);

            chartData.add(new ExchangeRateChartResult.ChartDataPoint(
                formattedDate,
                null, // 일별 데이터이므로 시간은 null
                rate,
                dayChange
            ));
        }

        // 차트 데이터를 시간 순서대로 정렬 (오래된 것부터)
        chartData = chartData.stream()
            .sorted((a, b) -> a.date().compareTo(b.date()))
            .collect(Collectors.toList());

        // 통계 계산
        ExchangeRateChartResult.ChartStatistics statistics = calculateChartStatistics(rates);

        // 현재 환율 (가장 최신 데이터)
        BigDecimal currentRate = rates.get(0);

        // 전일 대비 변동 계산
        BigDecimal change = rates.size() > 1 ?
            currentRate.subtract(rates.get(1)) : BigDecimal.ZERO;
        BigDecimal changePercent = rates.size() > 1 ?
            change.divide(rates.get(1), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        return new ExchangeRateChartResult(
            baseCurrency,
            targetCurrency,
            periodCode,
            currentRate,
            change,
            changePercent.setScale(2, RoundingMode.HALF_UP),
            LocalDateTime.now(),
            chartData,
            statistics
        );
    }

    private String formatDate(String bokDate) {
        // YYYYMMDD → YYYY-MM-DD 변환
        if (bokDate.length() == 8) {
            return bokDate.substring(0, 4) + "-" +
                bokDate.substring(4, 6) + "-" +
                bokDate.substring(6, 8);
        }
        return bokDate;
    }

    private BigDecimal calculateDayChange(List<BigDecimal> rates, int index) {
        if (index == 0 || rates.size() <= 1) {
            return BigDecimal.ZERO;
        }

        BigDecimal current = rates.get(index);
        BigDecimal previous = rates.get(index - 1);

        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return current.subtract(previous)
            .divide(previous, 4, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private ExchangeRateChartResult.ChartStatistics calculateChartStatistics(
        List<BigDecimal> rates) {
        if (rates.isEmpty()) {
            return new ExchangeRateChartResult.ChartStatistics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
        }

        // 최고값, 최저값
        BigDecimal high = rates.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal low = rates.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // 평균값
        BigDecimal sum = rates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(new BigDecimal(rates.size()), 4, RoundingMode.HALF_UP);

        // 변동성 (표준편차 계산)
        BigDecimal volatility = calculateStandardDeviation(rates, average);

        // 기간 시작 대비 변동률
        BigDecimal periodChangePercent = BigDecimal.ZERO;
        if (rates.size() > 1) {
            BigDecimal first = rates.getLast(); // 가장 오래된 데이터
            BigDecimal last = rates.getFirst(); // 가장 최신 데이터

            if (first.compareTo(BigDecimal.ZERO) > 0) {
                periodChangePercent = last.subtract(first)
                    .divide(first, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
            }
        }

        return new ExchangeRateChartResult.ChartStatistics(
            high,
            low,
            average,
            volatility,
            periodChangePercent
        );
    }

    private BigDecimal calculateStandardDeviation(List<BigDecimal> rates, BigDecimal average) {
        if (rates.size() <= 1) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumSquaredDiffs = rates.stream()
            .map(rate -> rate.subtract(average).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = sumSquaredDiffs.divide(new BigDecimal(rates.size() - 1), 4,
            RoundingMode.HALF_UP);

        // 간단한 제곱근 계산 (Newton's method)
        return sqrt(variance);
    }

    private BigDecimal sqrt(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal x = value;
        BigDecimal previous;

        // Newton's method for square root
        do {
            previous = x;
            x = x.add(value.divide(x, 4, RoundingMode.HALF_UP))
                .divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
        } while (x.subtract(previous).abs().compareTo(new BigDecimal("0.0001")) > 0);

        return x.setScale(2, RoundingMode.HALF_UP);
    }
}