package com.txnow.api.exchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.txnow.application.exchange.dto.CurrentExchangeRateResult;
import com.txnow.domain.exchange.model.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Schema(description = "현재 환율 조회 응답")
public record CurrentExchangeRateResponse(
    @Schema(description = "기준 통화", example = "KRW")
    @NotNull
    Currency baseCurrency,

    @Schema(description = "마지막 업데이트 시간", example = "2024-03-15T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @NotNull
    LocalDateTime lastUpdated,

    @Schema(description = "통화별 환율 정보")
    @NotNull
    Map<String, RateInfo> rates
) {

    @Schema(description = "환율 정보")
    public record RateInfo(
        @Schema(description = "환율", example = "1320.50")
        @NotNull
        BigDecimal rate,

        @Schema(description = "전일 대비 변동률(%)", example = "0.15")
        @NotNull
        BigDecimal change
    ) {}

    public static CurrentExchangeRateResponse from(CurrentExchangeRateResult result) {
        Map<String, RateInfo> rates = result.rates().entrySet().stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> new RateInfo(
                    entry.getValue().rate(),
                    entry.getValue().change()
                )
            ));

        return new CurrentExchangeRateResponse(
            result.baseCurrency(),
            result.lastUpdated(),
            rates
        );
    }
}