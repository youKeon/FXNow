package com.txnow.application.exchange;

import com.txnow.application.exchange.dto.ExchangeResult.ExchangeChartResult;
import com.txnow.application.exchange.dto.ExchangeResult.ExchangeChartResult.ExchangeCurrentRate;
import com.txnow.application.exchange.dto.ExchangeResult.ExchangeChartResult.ExchangeChartDataPoint;
import com.txnow.application.exchange.dto.ExchangeResult.ExchangeChartResult.ExchangeChartStatistics;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.ExchangeRateCalculator;
import com.txnow.domain.exchange.model.DailyRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExchangeChartMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int AVERAGE_SCALE = 4;

    public ExchangeChartResult toChartResult(
        Currency baseCurrency,
        Currency targetCurrency,
        String periodCode,
        List<DailyRate> dailyRates
    ) {
        if (dailyRates.isEmpty()) {
            throw new IllegalArgumentException("Daily rates cannot be empty");
        }

        List<BigDecimal> rates = dailyRates.stream()
            .map(DailyRate::rate)
            .toList();

        List<ExchangeChartDataPoint> chartData = buildChartDataPoints(dailyRates);
        ExchangeChartStatistics statistics = calculateStatistics(rates);
        ExchangeCurrentRate currentRate = calculateCurrentRate(rates);

        return new ExchangeChartResult(
            baseCurrency,
            targetCurrency,
            periodCode,
            currentRate,
            LocalDateTime.now(),
            chartData,
            statistics
        );
    }

    private List<ExchangeChartDataPoint> buildChartDataPoints(List<DailyRate> dailyRates) {
        List<ExchangeChartDataPoint> chartData = new ArrayList<>();

        for (int i = 0; i < dailyRates.size(); i++) {
            DailyRate current = dailyRates.get(i);
            BigDecimal dayChange = (i > 0)
                ? ExchangeRateCalculator.calculateChangePercentage(
                    current.rate(),
                    dailyRates.get(i - 1).rate()
                  )
                : BigDecimal.ZERO;

            chartData.add(new ExchangeChartDataPoint(
                current.date().format(DATE_FORMATTER),
                null,
                current.rate(),
                dayChange
            ));
        }

        return chartData;
    }

    private ExchangeChartStatistics calculateStatistics(List<BigDecimal> rates) {
        if (rates.isEmpty()) {
            return new ExchangeChartStatistics(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
        }

        BigDecimal high = rates.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal low = rates.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal sum = rates.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(
            BigDecimal.valueOf(rates.size()),
            AVERAGE_SCALE,
            RoundingMode.HALF_UP
        );

        return new ExchangeChartStatistics(high, low, average);
    }

    private ExchangeCurrentRate calculateCurrentRate(List<BigDecimal> rates) {
        if (rates.size() == 1) {
            return new ExchangeCurrentRate(
                rates.getLast(),
                BigDecimal.ZERO,
                BigDecimal.ZERO
            );
        }

        BigDecimal current = rates.getLast();
        BigDecimal previous = rates.get(rates.size() - 2);
        BigDecimal change = current.subtract(previous);
        BigDecimal changePercent = ExchangeRateCalculator
            .calculateChangePercentage(current, previous)
            .setScale(2, RoundingMode.HALF_UP);

        return new ExchangeCurrentRate(current, change, changePercent);
    }
}
