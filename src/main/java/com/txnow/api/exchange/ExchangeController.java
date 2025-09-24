package com.txnow.api.exchange;

import static com.txnow.api.exchange.dto.ConversionResponse.*;

import com.txnow.api.exchange.dto.ConversionRequest;
import com.txnow.api.exchange.dto.ConversionResponse;
import com.txnow.api.exchange.dto.ExchangeRateResponse;
import com.txnow.api.support.ApiResponse;
import com.txnow.application.exchange.ExchangeApplicationService;
import com.txnow.domain.exchange.model.Currency;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchange")
public class ExchangeController {

    private final ExchangeApplicationService exchangeApplicationService;

    public ExchangeController(ExchangeApplicationService exchangeApplicationService) {
        this.exchangeApplicationService = exchangeApplicationService;
    }

    @PostMapping("/convert")
    public ApiResponse<ConversionSingleResponse> convert(
        @Valid @RequestBody ConversionRequest.ConversionSingleRequest requestDto
    ) {
        var response = exchangeApplicationService.convert(requestDto);
        return ApiResponse.success(response);
    }


    @PostMapping("/convert/list")
    public ApiResponse<ConversionListResponse> convertList(
        @Valid @RequestBody ConversionRequest.ConversionListRequest requestDto
    ) {
        var response = exchangeApplicationService.convertList(requestDto);
        return ApiResponse.success(response);
    }

    @PostMapping("/convert/batch")
    public ApiResponse<ConversionBatchResponse> convertBatch(
        @Valid @RequestBody ConversionRequest.ConversionBatchRequest requestDto
    ) {
        var response = exchangeApplicationService.convertBatch(requestDto);
        return ApiResponse.success(response);
    }

    @GetMapping("/rates")
    public ApiResponse<ExchangeRateResponse.ExchangeRateSingleResponse> getRate(
        @RequestParam Currency base,
        @RequestParam Currency target
    ) {
        var response = exchangeApplicationService.getLatestRate(base, target);
        return ApiResponse.success(response);
    }

    @GetMapping("/rates/list")
    public ApiResponse<ExchangeRateResponse.ExchangeRateListResponse> getRates(
        @RequestParam Currency base,
        @RequestParam(required = false) List<Currency> targets
    ) {
        var response = exchangeApplicationService.getRates(base, targets);
        return ApiResponse.success(response);
    }

    @GetMapping("/rates/history")
    public ApiResponse<ExchangeRateResponse.ExchangeRateHistoryResponse> getRateHistory(
        @RequestParam Currency base,
        @RequestParam Currency target,
        @RequestParam String period
    ) {
        var response = exchangeApplicationService.getRateHistory(base, target, period);
        return ApiResponse.success(response);
    }

    @GetMapping("/currencies")
    public ApiResponse<Currency[]> getSupportedCurrencies() {
        var currencies = exchangeApplicationService.getSupportedCurrencies();
        return ApiResponse.success(currencies);
    }}