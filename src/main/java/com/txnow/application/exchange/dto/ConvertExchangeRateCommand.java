package com.txnow.application.exchange.dto;

import com.txnow.domain.exchange.model.Currency;

import java.math.BigDecimal;

public record ConvertExchangeRateCommand(
    Currency from,
    Currency to,
    BigDecimal amount
) {
}