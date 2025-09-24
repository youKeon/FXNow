package com.txnow.api.exchange.dto;

import com.txnow.domain.exchange.model.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class ConversionRequest {
    
    public record ConversionSingleRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "From currency is required")
        Currency fromCurrency,

        @NotNull(message = "To currency is required")
        Currency toCurrency
    ) {}
    
    public record ConversionListRequest(
        @NotEmpty(message = "At least one conversion request is required")
        @Size(max = 100, message = "Maximum 100 conversions allowed per request")
        List<ConversionSingleRequest> conversions
    ) {}
}