package com.txnow.api.exchange.dto;

import com.txnow.domain.exchange.model.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExchangeRateResponse {
    
    // 단건 환율 조회 응답
    public record ExchangeRateSingleResponse(
        Currency baseCurrency,
        Currency targetCurrency,
        BigDecimal rate,
        LocalDateTime timestamp
    ) {}
    
    // 다중 환율 조회 응답 (목록)
    public record ExchangeRateListResponse(
        List<ExchangeRateSingleResponse> rates,
        int totalCount,
        LocalDateTime fetchedAt
    ) {}
    
    // 기준 통화별 환율 조회 응답
    public record ExchangeRateByBaseResponse(
        Currency baseCurrency,
        Map<Currency, BigDecimal> rates,
        LocalDateTime timestamp
    ) {}
    
    // 환율 생성 응답 (신규 환율 등록)
    public record ExchangeRateCreateResponse(
        String rateId,
        Currency baseCurrency,
        Currency targetCurrency,
        BigDecimal rate,
        String status,
        LocalDateTime createdAt
    ) {}
    
    // 환율 수정 응답 (환율 업데이트)
    public record ExchangeRateUpdateResponse(
        String rateId,
        Currency baseCurrency,
        Currency targetCurrency,
        BigDecimal oldRate,
        BigDecimal newRate,
        String status,
        LocalDateTime updatedAt
    ) {}
    
    // 환율 삭제 응답 (환율 제거)
    public record ExchangeRateDeleteResponse(
        String rateId,
        Currency baseCurrency,
        Currency targetCurrency,
        boolean deleted,
        LocalDateTime deletedAt
    ) {}
    
    // 환율 히스토리 응답
    public record ExchangeRateHistoryResponse(
        Currency baseCurrency,
        Currency targetCurrency,
        List<RateHistoryItem> history,
        String period,
        LocalDateTime fromDate,
        LocalDateTime toDate
    ) {}
    
    // 히스토리 아이템
    public record RateHistoryItem(
        BigDecimal rate,
        LocalDateTime timestamp
    ) {}
    
    // 실시간 환율 스트림 응답
    public record ExchangeRateStreamResponse(
        Currency baseCurrency,
        Currency targetCurrency,
        BigDecimal rate,
        BigDecimal previousRate,
        BigDecimal change,
        BigDecimal changePercent,
        LocalDateTime timestamp
    ) {}
    
    // 환율 통계 응답
    public record ExchangeRateStatsResponse(
        Currency baseCurrency,
        Currency targetCurrency,
        BigDecimal currentRate,
        BigDecimal minRate,
        BigDecimal maxRate,
        BigDecimal avgRate,
        BigDecimal volatility,
        LocalDateTime periodStart,
        LocalDateTime periodEnd
    ) {}
}