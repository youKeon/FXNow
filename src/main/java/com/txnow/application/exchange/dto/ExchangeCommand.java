package com.txnow.application.exchange.dto;

import com.txnow.domain.exchange.model.Currency;

import java.math.BigDecimal;

public record ExchangeCommand() {

    public record ExchangeConvertCommand(
        Currency from,
        Currency to,
        BigDecimal amount
    ) {}

    public record ExchangeChartCommand(
        Currency baseCurrency,
        Currency targetCurrency,
        String startDate,
        String endDate
    ) {}
}
