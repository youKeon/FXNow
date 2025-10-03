package com.txnow.domain.exchange.exception;

import com.txnow.domain.exchange.model.Currency;

/**
 * 유효하지 않은 통화 사용 시 발생하는 예외
 */
public class InvalidCurrencyException extends ExchangeRateException {

    private final Currency currency;
    private final String reason;

    public InvalidCurrencyException(Currency currency, String reason) {
        super("INVALID_CURRENCY",
              String.format("Invalid currency: %s. Reason: %s", currency, reason));
        this.currency = currency;
        this.reason = reason;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getReason() {
        return reason;
    }
}
