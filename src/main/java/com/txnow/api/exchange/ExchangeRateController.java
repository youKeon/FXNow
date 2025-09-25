package com.txnow.api.exchange;

import com.txnow.api.exchange.dto.CurrentExchangeRateResponse;
import com.txnow.api.exchange.dto.CurrencyPairRateResponse;
import com.txnow.api.exchange.dto.ExchangeRateChartResponse;
import com.txnow.api.exchange.dto.ConvertExchangeRateRequest;
import com.txnow.api.exchange.dto.ConvertExchangeRateResponse;
import com.txnow.api.support.ApiResponse;
import com.txnow.application.exchange.ExchangeRateService;
import com.txnow.application.exchange.dto.CurrentExchangeRateResult;
import com.txnow.application.exchange.dto.CurrencyPairRateResult;
import com.txnow.application.exchange.dto.ExchangeRateChartResult;
import com.txnow.application.exchange.dto.ConvertExchangeRateCommand;
import com.txnow.application.exchange.dto.ConvertExchangeRateResult;
import com.txnow.domain.exchange.model.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "환율 조회", description = "실시간 환율 정보 조회 API")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "현재 환율 조회", description = "KRW 기준 실시간 환율 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "환율 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/current")
    public ApiResponse<CurrentExchangeRateResponse> getCurrentRates() {
        CurrentExchangeRateResult result = exchangeRateService.getCurrentRates();
        CurrentExchangeRateResponse response = CurrentExchangeRateResponse.from(result);
        return ApiResponse.success(response);
    }

    @Operation(summary = "지원 통화 목록 조회", description = "시스템에서 지원하는 통화 목록을 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "통화 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/currencies")
    public ApiResponse<Currency[]> getSupportedCurrencies() {
        var currencies = exchangeRateService.getSupportedCurrencies();
        return ApiResponse.success(currencies);
    }

    @Operation(summary = "특정 통화쌍 환율 조회", description = "특정 통화쌍의 실시간 환율 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "환율 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 통화쌍"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환율 데이터 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{from}/{to}")
    public ApiResponse<CurrencyPairRateResponse> getCurrencyPairRate(
        @Parameter(description = "기준 통화 코드", example = "USD")
        @PathVariable Currency from,

        @Parameter(description = "대상 통화 코드", example = "KRW")
        @PathVariable Currency to
    ) {
        CurrencyPairRateResult result = exchangeRateService.getCurrencyPairRate(from, to);
        CurrencyPairRateResponse response = CurrencyPairRateResponse.from(result);
        return ApiResponse.success(response);
    }

    @Operation(summary = "환율 차트 데이터 조회", description = "토스 인베스트 스타일 환율 차트 데이터를 조회합니다. (대상 통화는 KRW 고정)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "차트 데이터 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/chart/{baseCurrency}")
    public ApiResponse<ExchangeRateChartResponse> getExchangeRateChart(
        @Parameter(description = "기준 통화 (1단위당 KRW)", example = "USD")
        @PathVariable Currency baseCurrency,

        @Parameter(description = "조회 기간 (1d, 1w, 1m, 3m, 1y)", example = "1w")
        @RequestParam(defaultValue = "1w")
        @Pattern(regexp = "^(1d|1w|1m|3m|1y)$", message = "Period must be one of: 1d, 1w, 1m, 3m, 1y")
        String period
    ) {
        // 대상 통화를 KRW로 고정
        ExchangeRateChartResult result = exchangeRateService.getExchangeRateChart(baseCurrency, Currency.KRW, period);
        ExchangeRateChartResponse response = ExchangeRateChartResponse.from(result);
        return ApiResponse.success(response);
    }

    @Operation(summary = "환율 변환 계산", description = "두 통화 간 금액 변환을 수행합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "환율 변환 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "환율 데이터 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/convert")
    public ApiResponse<ConvertExchangeRateResponse> convertExchangeRate(
        @Valid @RequestBody ConvertExchangeRateRequest request
    ) {
        // API 요청 DTO를 Application Command로 변환
        ConvertExchangeRateCommand command = new ConvertExchangeRateCommand(
            request.from(),
            request.to(),
            request.amount()
        );

        ConvertExchangeRateResult result = exchangeRateService.convertExchangeRate(command);
        ConvertExchangeRateResponse response = ConvertExchangeRateResponse.from(result);
        return ApiResponse.success(response);
    }
}