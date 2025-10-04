package com.txnow.api.exchange;

import static com.txnow.api.exchange.dto.ExchangeResponse.ExchangeChartResponse;
import static com.txnow.api.exchange.dto.ExchangeResponse.ExchangeConvertResponse;
import static com.txnow.application.exchange.dto.ExchangeCommand.ExchangeChartCommand;
import static com.txnow.application.exchange.dto.ExchangeCommand.ExchangeConvertCommand;
import static com.txnow.application.exchange.dto.ExchangeResult.ExchangeConvertResult;

import com.txnow.api.exchange.dto.ExchangeRequest.ExchangeConvertRequest;
import com.txnow.api.support.ApiResponse;
import com.txnow.application.exchange.ExchangeRateService;
import com.txnow.application.exchange.dto.ExchangeResult.ExchangeChartResult;
import com.txnow.domain.exchange.model.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "환율 조회", description = "실시간 환율 정보 조회 API")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;


    @Operation(summary = "환율 차트 데이터 조회", description = "토스 인베스트 스타일 환율 차트 데이터를 조회합니다. (대상 통화는 KRW 고정)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "차트 데이터 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/chart/{baseCurrency}")
    public ApiResponse<ExchangeChartResponse> getExchangeRateChart(
        @Parameter(description = "기준 통화 (1단위당 KRW)", example = "USD")
        @PathVariable Currency baseCurrency,

        @Parameter(description = "시작일자 (yyyy-MM-dd)", example = "2024-01-01")
        @RequestParam String startDate,

        @Parameter(description = "종료일자 (yyyy-MM-dd)", example = "2024-01-31")
        @RequestParam String endDate
    ) {
        ExchangeChartCommand command = new ExchangeChartCommand(
            baseCurrency,
            Currency.KRW,
            startDate,
            endDate
        );

        ExchangeChartResult result = exchangeRateService.getExchangeRateChart(command);
        ExchangeChartResponse response = ExchangeChartResponse.from(result);
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
    public ApiResponse<ExchangeConvertResponse> convertExchangeRate(
        @Valid @RequestBody ExchangeConvertRequest request
    ) {
        ExchangeConvertCommand command = new ExchangeConvertCommand(
            request.from(),
            request.to(),
            request.amount()
        );

        ExchangeConvertResult result = exchangeRateService.convertExchangeRate(command);
        ExchangeConvertResponse response = ExchangeConvertResponse.from(result);
        return ApiResponse.success(response);
    }
}