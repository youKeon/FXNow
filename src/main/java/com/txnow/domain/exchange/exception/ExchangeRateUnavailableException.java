package com.txnow.domain.exchange.exception;

import com.txnow.domain.exchange.model.Currency;

/**
 * 환율 서비스가 일시적으로 사용 불가능할 때 발생하는 예외
 * API 호출 실패, 네트워크 오류 등
 */
public class ExchangeRateUnavailableException extends RuntimeException {

    private final Currency currency;

    public ExchangeRateUnavailableException(Currency currency, String reason) {
        super("Exchange rate service unavailable for currency: " + currency + ". Reason: " + reason);
        this.currency = currency;
    }

    public ExchangeRateUnavailableException(Currency currency, Throwable cause) {
        super("Exchange rate service unavailable for currency: " + currency, cause);
        this.currency = currency;
    }

    /**
     * Rate limit 초과 시 사용하는 생성자
     */
    public ExchangeRateUnavailableException(int currentCalls, int maxCalls, long waitTimeSeconds) {
        super(String.format("BOK API rate limit exceeded. Current calls: %d/%d, Required wait time: %d seconds",
            currentCalls, maxCalls, waitTimeSeconds));
        this.currency = null;
    }

    public Currency getCurrency() {
        return currency;
    }
}
