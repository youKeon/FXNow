package com.txnow.domain.exchange.exception;

/**
 * 환율 관련 모든 예외의 최상위 클래스
 * 비즈니스 예외와 기술 예외를 구분하기 위한 Base Exception
 */
public abstract class ExchangeRateException extends RuntimeException {

    private final String errorCode;

    protected ExchangeRateException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ExchangeRateException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
