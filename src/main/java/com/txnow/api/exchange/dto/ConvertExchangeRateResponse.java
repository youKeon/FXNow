package com.txnow.api.exchange.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.txnow.application.exchange.dto.ConvertExchangeRateResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "환율 변환 계산 응답")
public record ConvertExchangeRateResponse(

    @JsonProperty("converted_amount")
    @Schema(description = "변환된 금액", example = "132050.00")
    BigDecimal convertedAmount,

    @Schema(description = "환율 적용 시점", example = "2024-01-15T10:30:00")
    LocalDateTime timestamp
) {

    public static ConvertExchangeRateResponse from(ConvertExchangeRateResult result) {
        return new ConvertExchangeRateResponse(
            result.convertedAmount(),
            result.timestamp()
        );
    }
}