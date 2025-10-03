package com.txnow.domain.exchange.exception;

import java.math.BigDecimal;

/**
 * 유효하지 않은 금액 사용 시 발생하는 예외
 */
public class InvalidAmountException extends ExchangeRateException {

    private final BigDecimal amount;

    public InvalidAmountException(BigDecimal amount, String reason) {
        super("INVALID_AMOUNT",
              String.format("Invalid amount: %s. Reason: %s", amount, reason));
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
