package com.txnow.domain.exchange.model;

import com.txnow.application.exchange.dto.ExchangeRateChartResult;
import com.txnow.application.exchange.dto.ExchangeRateChartResult.ChartStatistics;
import com.txnow.infrastructure.external.bok.BokApiResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public ExchangeRateChartResult createChartResult(
        Currency baseCurrency,
        Currency targetCurrency,
        String periodCode,
        BokApiResponse response
    ) {

        var statisticSearch = response.statisticSearch();
        var rows = statisticSearch.rows();

        List<RawPoint> rawPoints = new ArrayList<>();

        for (var row : rows) {
            BigDecimal rate = new BigDecimal(row.dataValue());

            if (baseCurrency == Currency.JPY) {
                rate = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            }

            rawPoints.add(new RawPoint(formatDate(row.time()), rate));
        }

        rawPoints.sort(Comparator.comparing(RawPoint::date));

        List<ExchangeRateChartResult.ChartDataPoint> chartData = new ArrayList<>();
        List<BigDecimal> orderedRates = new ArrayList<>();

        for (int i = 0; i < rawPoints.size(); i++) {
            RawPoint point = rawPoints.get(i);
            orderedRates.add(point.rate());

            BigDecimal dayChange = BigDecimal.ZERO;
            if (i > 0) {
                dayChange = calculator.calculateChangePercentage(point.rate(), rawPoints.get(i - 1).rate());
            }

            chartData.add(new ExchangeRateChartResult.ChartDataPoint(
                point.date(),
                null,
                point.rate(),
                dayChange
            ));
        }

        ChartStatistics statistics = calculateChartStatistics(orderedRates);

        BigDecimal currentRate = orderedRates.getLast();
        BigDecimal change = orderedRates.size() > 1
            ? currentRate.subtract(orderedRates.get(orderedRates.size() - 2))
            : BigDecimal.ZERO;
        BigDecimal changePercent = orderedRates.size() > 1
            ? calculator.calculateChangePercentage(currentRate, orderedRates.get(orderedRates.size() - 2))
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
    private ChartStatistics calculateChartStatistics(List<BigDecimal> rates) {
        if (rates.isEmpty()) {
            return new ChartStatistics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
        }

        BigDecimal high = rates.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal low = rates.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        BigDecimal sum = rates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(new BigDecimal(rates.size()), 4, RoundingMode.HALF_UP);

        return new ChartStatistics(
            high,
            low,
            average
        );
    }

    private record RawPoint(String date, BigDecimal rate) {}
}
