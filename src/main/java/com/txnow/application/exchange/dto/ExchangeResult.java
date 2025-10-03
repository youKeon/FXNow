package com.txnow.application.exchange.dto;

import com.txnow.domain.exchange.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ExchangeResult() {

    public record ExchangeConvertResult(
        BigDecimal convertedAmount,
        BigDecimal rate,
        LocalDateTime timestamp
    ) {}

    public record ExchangeChartResult(
        Currency baseCurrency,
        Currency targetCurrency,
        String period,
        ExchangeCurrentRate currentRate,
        LocalDateTime lastUpdated,
        List<ExchangeChartDataPoint> chartData,
        ExchangeChartStatistics statistics
    ) {
        /**
         * 현재 환율 정보
         */
        public record ExchangeCurrentRate(
            BigDecimal rate,
            BigDecimal change,
            BigDecimal changePercent
        ) {}

        /**
         * 차트 데이터 포인트
         */
        public record ExchangeChartDataPoint(
            String date,
            String time,
            BigDecimal rate,
            BigDecimal dayChange
        ) {}

        /**
         * 차트 통계 정보
         */
        public record ExchangeChartStatistics(
            BigDecimal high,
            BigDecimal low,
            BigDecimal average
        ) {}
    }
}
