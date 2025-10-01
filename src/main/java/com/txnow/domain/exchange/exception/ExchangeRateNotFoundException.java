package com.txnow.domain.exchange.exception;

import com.txnow.domain.exchange.model.Currency;

/**
 * 환율 데이터를 찾을 수 없을 때 발생하는 예외
 * Unchecked Exception으로 설계하여 호출부에서 선택적으로 처리 가능
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    private final Currency currency;

    public ExchangeRateNotFoundException(Currency currency) {
        super("Exchange rate not found for currency: " + currency);
        this.currency = currency;
    }

    public ExchangeRateNotFoundException(Currency currency, String reason) {
        super("Exchange rate not found for currency: " + currency + ". Reason: " + reason);
        this.currency = currency;
    }

    public ExchangeRateNotFoundException(Currency currency, Throwable cause) {
        super("Exchange rate not found for currency: " + currency, cause);
        this.currency = currency;
    }

    public Currency getCurrency() {
        return currency;
    }
}
