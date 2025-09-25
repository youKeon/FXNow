package com.txnow.application.exchange.dto;

import com.txnow.domain.exchange.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CurrentExchangeRateResult(
    Currency baseCurrency,
    LocalDateTime lastUpdated,
    Map<String, RateInfo> rates
) {

    /**
     * 환율 정보 (환율 + 변동률)
     */
    public record RateInfo(
        BigDecimal rate,
        BigDecimal change
    ) {}
}