package com.txnow.application.exchange.dto;

import com.txnow.domain.exchange.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExchangeRateChartResult(
    Currency baseCurrency,
    Currency targetCurrency,
    String period,
    BigDecimal currentRate,
    BigDecimal change,
    BigDecimal changePercent,
    LocalDateTime lastUpdated,
    List<ChartDataPoint> chartData,
    ChartStatistics statistics
) {

    /**
     * 차트 데이터 포인트
     */
    public record ChartDataPoint(
        String date,
        String time,
        BigDecimal rate,
        BigDecimal dayChange
    ) {}

    /**
     * 차트 통계 정보
     */
    public record ChartStatistics(
        BigDecimal high,
        BigDecimal low,
        BigDecimal average
    ) {}
}
