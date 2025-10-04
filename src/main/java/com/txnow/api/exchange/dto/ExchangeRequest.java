package com.txnow.api.exchange.dto;

import com.txnow.domain.exchange.model.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "환율 API 요청 DTO")
public record ExchangeRequest() {

    @Schema(description = "환율 변환 계산 요청")
    public record ExchangeConvertRequest(
        @NotNull(message = "기준 통화는 필수입니다")
        @Schema(description = "기준 통화 코드", example = "USD")
        Currency from,

        @NotNull(message = "대상 통화는 필수입니다")
        @Schema(description = "대상 통화 코드", example = "KRW")
        Currency to,

        @NotNull(message = "변환 금액은 필수입니다")
        @Positive(message = "변환 금액은 양수여야 합니다")
        @Schema(description = "변환할 금액", example = "100")
        BigDecimal amount
    ) {}
}
