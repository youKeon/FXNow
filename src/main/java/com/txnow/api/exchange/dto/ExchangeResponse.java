package com.txnow.api.exchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.txnow.application.exchange.dto.ExchangeResult;
import com.txnow.domain.exchange.model.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "환율 API 응답 DTO")
public record ExchangeResponse() {

    @Schema(description = "환율 변환 계산 응답")
    public record ExchangeConvertResponse(
        @JsonProperty("convertedAmount")
        @Schema(description = "변환된 금액", example = "132050.00")
        BigDecimal convertedAmount,

        @Schema(description = "환율", example = "1320.50")
        BigDecimal rate,

        @Schema(description = "환율 적용 시점", example = "2024-01-15T10:30:00")
        LocalDateTime timestamp
    ) {
        public static ExchangeConvertResponse from(ExchangeResult.ExchangeConvertResult result) {
            return new ExchangeConvertResponse(
                result.convertedAmount(),
                result.rate(),
                result.timestamp()
            );
        }
    }

    @Schema(description = "환율 차트 데이터 응답")
    public record ExchangeChartResponse(
        @Schema(description = "기준 통화", example = "USD")
        @NotNull
        Currency baseCurrency,

        @Schema(description = "대상 통화", example = "KRW")
        @NotNull
        Currency targetCurrency,

        @Schema(description = "조회 기간", example = "1w")
        @NotNull
        String period,

        @Schema(description = "현재 환율", example = "1400.20")
        @NotNull
        BigDecimal currentRate,

        @Schema(description = "전일 대비 변동", example = "5.30")
        @NotNull
        BigDecimal change,

        @Schema(description = "전일 대비 변동률(%)", example = "0.38")
        @NotNull
        BigDecimal changePercent,

        @Schema(description = "마지막 업데이트 시간", example = "2024-03-15T10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @NotNull
        LocalDateTime lastUpdated,

        @Schema(description = "차트 데이터 포인트")
        @NotNull
        List<ExchangeChartDataPoint> chartData,

        @Schema(description = "기간 통계")
        @NotNull
        ExchangeChartStatistics statistics
    ) {
        public static ExchangeChartResponse from(ExchangeResult.ExchangeChartResult result) {
            List<ExchangeChartDataPoint> chartData = result.chartData().stream()
                .map(dataPoint -> new ExchangeChartDataPoint(
                    dataPoint.date(),
                    dataPoint.time(),
                    dataPoint.rate(),
                    dataPoint.dayChange()
                ))
                .collect(Collectors.toList());

            ExchangeChartStatistics statistics = new ExchangeChartStatistics(
                result.statistics().high(),
                result.statistics().low(),
                result.statistics().average()
            );

            var currentRate = result.currentRate();

            return new ExchangeChartResponse(
                result.baseCurrency(),
                result.targetCurrency(),
                result.period(),
                currentRate.rate(),
                currentRate.change(),
                currentRate.changePercent(),
                result.lastUpdated(),
                chartData,
                statistics
            );
        }
    }

    @Schema(description = "차트 데이터 포인트")
    public record ExchangeChartDataPoint(
        @Schema(description = "날짜", example = "2024-03-15")
        @NotNull
        String date,

        @Schema(description = "시간 (일내 차트용)", example = "10:30")
        String time,

        @Schema(description = "환율", example = "1399.80")
        @NotNull
        BigDecimal rate,

        @Schema(description = "해당 일자 변동률", example = "0.15")
        @NotNull
        BigDecimal dayChange
    ) {}

    @Schema(description = "차트 통계 정보")
    public record ExchangeChartStatistics(
        @Schema(description = "기간 내 최고값", example = "1405.50")
        @NotNull
        BigDecimal high,

        @Schema(description = "기간 내 최저값", example = "1380.20")
        @NotNull
        BigDecimal low,

        @Schema(description = "기간 평균", example = "1392.85")
        @NotNull
        BigDecimal average
    ) {}
}
