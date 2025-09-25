package com.txnow.application.exchange.dto;

import com.txnow.domain.exchange.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConvertExchangeRateResult(
    Currency from,
    Currency to,
    BigDecimal amount,
    BigDecimal convertedAmount,
    BigDecimal rate,
    LocalDateTime timestamp
) {
}