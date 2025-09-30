package com.txnow.application.exchange.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConvertExchangeRateResult(
    BigDecimal convertedAmount,
    BigDecimal rate,
    LocalDateTime timestamp
) {
}