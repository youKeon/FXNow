package com.txnow.application.exchange.dto;

import com.txnow.domain.exchange.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CurrencyPairRateResult(
    Currency from,
    Currency to,
    BigDecimal rate,
    BigDecimal change,
    BigDecimal changePercent,
    LocalDateTime lastUpdated
) {}