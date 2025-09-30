package com.txnow.domain.exchange.model;

import com.txnow.application.exchange.dto.ExchangeRateChartResult;
import com.txnow.application.exchange.dto.ExchangeRateChartResult.ChartStatistics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
     * 히스토리 환율 데이터를 기반으로 환율 차트 결과를 생성합니다.
     */
    public ExchangeRateChartResult createChartResult(
        Currency baseCurrency,
        Currency targetCurrency,
        String periodCode,
        List<HistoricalRate> historicalRates
    ) {
        if (historicalRates.isEmpty()) {
            throw new IllegalArgumentException("Historical rates cannot be empty");
        }

        List<ExchangeRateChartResult.ChartDataPoint> chartData = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < historicalRates.size(); i++) {
            HistoricalRate histRate = historicalRates.get(i);
            rates.add(histRate.rate());

            BigDecimal dayChange = BigDecimal.ZERO;
            if (i > 0) {
                dayChange = calculator.calculateChangePercentage(
                    histRate.rate(),
                    historicalRates.get(i - 1).rate()
                );
            }

            chartData.add(new ExchangeRateChartResult.ChartDataPoint(
                histRate.date().format(dateFormatter),
                null, // time은 일별 차트에서 사용 안함
                histRate.rate(),
                dayChange
            ));
        }

        ChartStatistics statistics = calculateChartStatistics(rates);

        BigDecimal currentRate = rates.getLast();
        BigDecimal change = rates.size() > 1
            ? currentRate.subtract(rates.get(rates.size() - 2))
            : BigDecimal.ZERO;
        BigDecimal changePercent = rates.size() > 1
            ? calculator.calculateChangePercentage(currentRate, rates.get(rates.size() - 2))
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
}
