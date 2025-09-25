package com.txnow.api.exchange;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.txnow.api.exchange.dto.ConvertExchangeRateRequest;
import com.txnow.application.exchange.ExchangeRateService;
import com.txnow.application.exchange.dto.ConvertExchangeRateCommand;
import com.txnow.application.exchange.dto.ConvertExchangeRateResult;
import com.txnow.domain.exchange.model.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@WebMvcTest(ExchangeRateController.class)
@DisplayName("환율 변환 계산 API 컨트롤러 테스트")
class ConvertExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("환율 변환 계산 API 성공")
    void convertExchangeRate_Success() throws Exception {
        // given
        ConvertExchangeRateRequest request = new ConvertExchangeRateRequest(
            Currency.USD, Currency.KRW, new BigDecimal("100")
        );

        ConvertExchangeRateResult mockResult = new ConvertExchangeRateResult(
            Currency.USD,
            Currency.KRW,
            new BigDecimal("100"),
            new BigDecimal("132050.00"),
            new BigDecimal("1320.50"),
            LocalDateTime.of(2024, 1, 15, 10, 30, 0)
        );

        given(exchangeRateService.convertExchangeRate(
            new ConvertExchangeRateCommand(Currency.USD, Currency.KRW, new BigDecimal("100"))
        )).willReturn(mockResult);

        // when & then
        mockMvc.perform(post("/api/exchange-rates/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.from").value("USD"))
            .andExpect(jsonPath("$.data.to").value("KRW"))
            .andExpect(jsonPath("$.data.amount").value(100))
            .andExpect(jsonPath("$.data.converted_amount").value(132050.00))
            .andExpect(jsonPath("$.data.rate").value(1320.50))
            .andExpect(jsonPath("$.data.timestamp").value("2024-01-15T10:30:00"));
    }

    @Test
    @DisplayName("필수 파라미터 누락시 400 에러")
    void convertExchangeRate_MissingRequiredFields() throws Exception {
        // given - from 필드 누락
        String invalidRequest = """
            {
                "to": "KRW",
                "amount": 100
            }
            """;

        // when & then
        mockMvc.perform(post("/api/exchange-rates/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("음수 금액 전달시 400 에러")
    void convertExchangeRate_NegativeAmount() throws Exception {
        // given
        ConvertExchangeRateRequest request = new ConvertExchangeRateRequest(
            Currency.USD, Currency.KRW, new BigDecimal("-100")
        );

        // when & then
        mockMvc.perform(post("/api/exchange-rates/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("잘못된 JSON 형식 전달시 400 에러")
    void convertExchangeRate_InvalidJsonFormat() throws Exception {
        // given
        String invalidJson = "{ invalid json }";

        // when & then
        mockMvc.perform(post("/api/exchange-rates/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지원하지 않는 통화 코드 전달시 400 에러")
    void convertExchangeRate_UnsupportedCurrency() throws Exception {
        // given
        String requestWithInvalidCurrency = """
            {
                "from": "INVALID",
                "to": "KRW",
                "amount": 100
            }
            """;

        // when & then
        mockMvc.perform(post("/api/exchange-rates/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithInvalidCurrency))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("서비스 예외 발생시 500 에러")
    void convertExchangeRate_ServiceException() throws Exception {
        // given
        ConvertExchangeRateRequest request = new ConvertExchangeRateRequest(
            Currency.USD, Currency.KRW, new BigDecimal("100")
        );

        given(exchangeRateService.convertExchangeRate(
            new ConvertExchangeRateCommand(Currency.USD, Currency.KRW, new BigDecimal("100"))
        )).willThrow(new RuntimeException("Exchange rate data not available"));

        // when & then
        mockMvc.perform(post("/api/exchange-rates/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("동일 통화간 변환 요청 성공")
    void convertExchangeRate_SameCurrency() throws Exception {
        // given
        ConvertExchangeRateRequest request = new ConvertExchangeRateRequest(
            Currency.USD, Currency.USD, new BigDecimal("100")
        );

        ConvertExchangeRateResult mockResult = new ConvertExchangeRateResult(
            Currency.USD,
            Currency.USD,
            new BigDecimal("100"),
            new BigDecimal("100"),
            BigDecimal.ONE,
            LocalDateTime.of(2024, 1, 15, 10, 30, 0)
        );

        given(exchangeRateService.convertExchangeRate(
            new ConvertExchangeRateCommand(Currency.USD, Currency.USD, new BigDecimal("100"))
        )).willReturn(mockResult);

        // when & then
        mockMvc.perform(post("/api/exchange-rates/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.from").value("USD"))
            .andExpect(jsonPath("$.data.to").value("USD"))
            .andExpect(jsonPath("$.data.amount").value(100))
            .andExpect(jsonPath("$.data.converted_amount").value(100))
            .andExpect(jsonPath("$.data.rate").value(1.0));
    }
}