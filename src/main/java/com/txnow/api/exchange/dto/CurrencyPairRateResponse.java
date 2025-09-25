package com.txnow.api.exchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.txnow.application.exchange.dto.CurrencyPairRateResult;
import com.txnow.domain.exchange.model.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "특정 통화쌍 환율 조회 응답")
public record CurrencyPairRateResponse(
    @Schema(description = "기준 통화", example = "USD")
    @NotNull
    Currency from,

    @Schema(description = "대상 통화", example = "KRW")
    @NotNull
    Currency to,

    @Schema(description = "환율", example = "1320.50")
    @NotNull
    BigDecimal rate,

    @Schema(description = "전일 대비 변동", example = "-0.8")
    @NotNull
    BigDecimal change,

    @Schema(description = "전일 대비 변동률(%)", example = "-0.06")
    @NotNull
    BigDecimal changePercent,

    @Schema(description = "마지막 업데이트 시간", example = "2024-01-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @NotNull
    LocalDateTime lastUpdated
) {

    public static CurrencyPairRateResponse from(CurrencyPairRateResult result) {
        return new CurrencyPairRateResponse(
            result.from(),
            result.to(),
            result.rate(),
            result.change(),
            result.changePercent(),
            result.lastUpdated()
        );
    }
}