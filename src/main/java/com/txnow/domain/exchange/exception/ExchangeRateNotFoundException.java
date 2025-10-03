package com.txnow.domain.exchange.exception;

import com.txnow.domain.exchange.model.Currency;

/**
 * 환율 데이터를 찾을 수 없을 때 발생하는 예외
 */
public class ExchangeRateNotFoundException extends ExchangeRateException {

    private final Currency currency;

    public ExchangeRateNotFoundException(Currency currency) {
        super("EXCHANGE_RATE_NOT_FOUND",
              "Exchange rate not found for currency: " + currency);
        this.currency = currency;
    }

    public ExchangeRateNotFoundException(Currency currency, String reason) {
        super("EXCHANGE_RATE_NOT_FOUND",
              "Exchange rate not found for currency: " + currency + ". Reason: " + reason);
        this.currency = currency;
    }

    public ExchangeRateNotFoundException(Currency currency, Throwable cause) {
        super("EXCHANGE_RATE_NOT_FOUND",
              "Exchange rate not found for currency: " + currency, cause);
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }
}
