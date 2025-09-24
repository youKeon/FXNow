package com.txnow.api.exchange.dto;

import com.txnow.domain.exchange.model.Currency;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ConversionResponse {
    
    public record ConversionSingleResponse(
        BigDecimal originalAmount,
        Currency fromCurrency,
        BigDecimal convertedAmount,
        Currency toCurrency,
        BigDecimal exchangeRate,
        LocalDateTime timestamp
    ) {}
    
    public record ConversionListResponse(
        List<ConversionSingleResponse> conversions,
        int totalCount,
        LocalDateTime processedAt
    ) {}
}