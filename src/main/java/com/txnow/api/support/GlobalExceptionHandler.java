package com.txnow.api.support;

import com.txnow.domain.exchange.exception.ExchangeRateException;
import com.txnow.domain.exchange.exception.ExchangeRateNotFoundException;
import com.txnow.domain.exchange.exception.ExchangeRateUnavailableException;
import com.txnow.domain.exchange.exception.InvalidAmountException;
import com.txnow.domain.exchange.exception.InvalidCurrencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCurrencyException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidCurrencyException(InvalidCurrencyException e) {
        log.warn("Invalid currency: {}", e.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidAmountException(InvalidAmountException e) {
        log.warn("Invalid amount: {}", e.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(ExchangeRateNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleExchangeRateNotFoundException(ExchangeRateNotFoundException e) {
        log.warn("Exchange rate not found: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public ResponseEntity<ApiResponse<Object>> handleExchangeRateUnavailableException(ExchangeRateUnavailableException e) {
        log.error("Exchange rate service unavailable: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    /**
     * Base exception handler for all ExchangeRateException subclasses
     * Catches any custom exceptions not handled by more specific handlers above
     */
    @ExceptionHandler(ExchangeRateException.class)
    public ResponseEntity<ApiResponse<Object>> handleExchangeRateException(ExchangeRateException e) {
        log.error("Exchange rate error: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("INVALID_ARGUMENT", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errorMessage);
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("VALIDATION_FAILED", "Validation failed: " + errorMessage));
    }

    /**
     * 정적 리소스 404 에러 처리
     * 로그 레벨을 debug로 낮춰서 불필요한 에러 로그 방지
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("Static resource not found: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("RESOURCE_NOT_FOUND", "Resource not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}