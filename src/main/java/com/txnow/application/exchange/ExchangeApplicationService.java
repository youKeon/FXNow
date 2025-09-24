package com.txnow.application.exchange;

import com.txnow.api.exchange.dto.ConversionRequest;
import com.txnow.api.exchange.dto.ConversionResponse;
import com.txnow.api.exchange.dto.ExchangeRateResponse;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.CurrencyPair;
import com.txnow.domain.exchange.service.ExchangeRateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExchangeApplicationService {

    private final ExchangeRateService exchangeRateService;

    public ExchangeApplicationService(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    public ConversionResponse.ConversionSingleResponse convert(ConversionRequest.ConversionSingleRequest requestDto) {
        var conversionRequest = new com.txnow.domain.exchange.model.ConversionRequest(
            requestDto.amount(),
            requestDto.fromCurrency(),
            requestDto.toCurrency()
        );

        var result = exchangeRateService.convert(conversionRequest);

        return new ConversionResponse.ConversionSingleResponse(
            result.originalAmount(),
            result.fromCurrency(),
            result.convertedAmount(),
            result.toCurrency(),
            result.exchangeRate(),
            result.timestamp()
        );
    }

    public ConversionResponse.ConversionListResponse convertList(ConversionRequest.ConversionListRequest requestDto) {
        var conversions = requestDto.conversions().stream()
            .map(this::convertSingle)
            .collect(Collectors.toList());

        return new ConversionResponse.ConversionListResponse(
            conversions,
            conversions.size(),
            LocalDateTime.now()
        );
    }

    public ConversionResponse.ConversionBatchResponse convertBatch(ConversionRequest.ConversionBatchRequest requestDto) {
        List<ConversionResponse.ConversionSingleResponse> successfulConversions = new ArrayList<>();
        List<ConversionResponse.ConversionErrorResponse> failedConversions = new ArrayList<>();

        for (var conversion : requestDto.conversions()) {
            try {
                var result = convertSingle(conversion);
                successfulConversions.add(result);
            } catch (Exception e) {
                var errorResponse = new ConversionResponse.ConversionErrorResponse(
                    "CONVERSION_FAILED",
                    e.getMessage(),
                    conversion.fromCurrency(),
                    conversion.toCurrency(),
                    conversion.amount()
                );
                failedConversions.add(errorResponse);

                if (Boolean.TRUE.equals(requestDto.failFast())) {
                    break;
                }
            }
        }

        return new ConversionResponse.ConversionBatchResponse(
            successfulConversions,
            failedConversions,
            requestDto.conversions().size(),
            successfulConversions.size(),
            failedConversions.size(),
            LocalDateTime.now()
        );
    }

    private ConversionResponse.ConversionSingleResponse convertSingle(ConversionRequest.ConversionSingleRequest requestDto) {
        var conversionRequest = new com.txnow.domain.exchange.model.ConversionRequest(
            requestDto.amount(),
            requestDto.fromCurrency(),
            requestDto.toCurrency()
        );

        var result = exchangeRateService.convert(conversionRequest);

        return new ConversionResponse.ConversionSingleResponse(
            result.originalAmount(),
            result.fromCurrency(),
            result.convertedAmount(),
            result.toCurrency(),
            result.exchangeRate(),
            result.timestamp()
        );
    }

    public ExchangeRateResponse.ExchangeRateSingleResponse getLatestRate(Currency base, Currency target) {
        var currencyPair = new CurrencyPair(base, target);
        var rate = exchangeRateService.getLatestRate(currencyPair);

        return new ExchangeRateResponse.ExchangeRateSingleResponse(
            rate.currencyPair().base(),
            rate.currencyPair().target(),
            rate.rate(),
            rate.timestamp()
        );
    }

    public ExchangeRateResponse.ExchangeRateListResponse getRates(Currency base, List<Currency> targets) {
        List<Currency> targetCurrencies = targets != null ? targets : List.of(Currency.values());
        
        var rates = targetCurrencies.stream()
            .filter(target -> !target.equals(base))
            .map(target -> {
                var currencyPair = new CurrencyPair(base, target);
                var rate = exchangeRateService.getLatestRate(currencyPair);
                return new ExchangeRateResponse.ExchangeRateSingleResponse(
                    rate.currencyPair().base(),
                    rate.currencyPair().target(),
                    rate.rate(),
                    rate.timestamp()
                );
            })
            .collect(Collectors.toList());

        return new ExchangeRateResponse.ExchangeRateListResponse(
            rates,
            rates.size(),
            LocalDateTime.now()
        );
    }

    public ExchangeRateResponse.ExchangeRateHistoryResponse getRateHistory(Currency base, Currency target, String period) {
        // TODO: 실제 히스토리 조회 로직 구현 필요
        var currencyPair = new CurrencyPair(base, target);
        var currentRate = exchangeRateService.getLatestRate(currencyPair);
        
        // 임시 히스토리 데이터 (실제로는 repository에서 조회)
        var historyItems = List.of(
            new ExchangeRateResponse.RateHistoryItem(currentRate.rate(), currentRate.timestamp())
        );

        return new ExchangeRateResponse.ExchangeRateHistoryResponse(
            base,
            target,
            historyItems,
            period,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now()
        );
    }

    public Currency[] getSupportedCurrencies() {
        return Currency.values();
    }
}