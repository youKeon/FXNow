package com.txnow.domain.exchange.model;

import com.txnow.application.exchange.dto.ExchangeRateChartResult;
import com.txnow.infrastructure.external.bok.BokApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 환율 차트 데이터 처리 전담 도메인 객체
 * 차트 데이터 구성, 통계 계산, 차트 검증 로직을 담당합니다.
 */
@Component
@RequiredArgsConstructor
public class ExchangeRateChart {

    private final ExchangeRateCalculator calculator;

    /**
     * 차트 대상 통화가 유효한지 검증합니다.
     */
    public boolean isValidChartTargetCurrency(Currency targetCurrency) {
        return targetCurrency == Currency.KRW;
    }

    /**
     * BOK API 응답을 기반으로 환율 차트 결과를 생성합니다.
     */
    public ExchangeRateChartResult createChartResult(Currency baseCurrency, Currency targetCurrency,
                                                   String periodCode, BokApiResponse response) {
        var statisticSearch = response.statisticSearch();
        var rows = statisticSearch.rows();

        // 환율 데이터 처리
        List<ExchangeRateChartResult.ChartDataPoint> chartData = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();

        for (var row : rows) {
            BigDecimal rate = new BigDecimal(row.dataValue());

            // JPY는 100엔 기준이므로 100으로 나누어 1엔당 원화로 변환
            if (baseCurrency == Currency.JPY) {
                rate = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            }

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
            calculator.calculateChangePercentage(currentRate, rates.get(1))
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

    /**
     * 날짜 포맷팅 (YYYYMMDD → YYYY-MM-DD)
     */
    private String formatDate(String bokDate) {
        if (bokDate.length() == 8) {
            return bokDate.substring(0, 4) + "-" +
                bokDate.substring(4, 6) + "-" +
                bokDate.substring(6, 8);
        }
        return bokDate;
    }

    /**
     * 일별 변동률 계산
     */
    private BigDecimal calculateDayChange(List<BigDecimal> rates, int index) {
        if (index == 0 || rates.size() <= 1) {
            return BigDecimal.ZERO;
        }

        BigDecimal current = rates.get(index);
        BigDecimal previous = rates.get(index - 1);

        return calculator.calculateChangePercentage(current, previous);
    }

    /**
     * 차트 통계 계산
     */
    private ExchangeRateChartResult.ChartStatistics calculateChartStatistics(List<BigDecimal> rates) {
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

            periodChangePercent = calculator.calculateChangePercentage(last, first);
        }

        return new ExchangeRateChartResult.ChartStatistics(
            high,
            low,
            average,
            volatility,
            periodChangePercent
        );
    }

    /**
     * 표준편차 계산
     */
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

    /**
     * 제곱근 계산 (Newton's method)
     */
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