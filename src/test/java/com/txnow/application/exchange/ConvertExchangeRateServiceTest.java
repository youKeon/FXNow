package com.txnow.application.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static com.txnow.application.exchange.dto.CurrentExchangeRateResult.RateInfo;

import com.txnow.application.exchange.dto.ConvertExchangeRateCommand;
import com.txnow.application.exchange.dto.ConvertExchangeRateResult;
import com.txnow.application.exchange.dto.CurrentExchangeRateResult;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.repository.ExchangeRateRepository;
import com.txnow.infrastructure.external.bok.BokApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("환율 변환 계산 서비스 테스트")
class ConvertExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private BokApiClient bokApiClient;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("동일 통화간 변환 - 1:1 비율로 변환됨")
    void convertSameCurrency() {
        // given
        ConvertExchangeRateCommand command = new ConvertExchangeRateCommand(
            Currency.USD, Currency.USD, new BigDecimal("100")
        );

        // when
        ConvertExchangeRateResult result = exchangeRateService.convertExchangeRate(command);

        // then
        assertThat(result.from()).isEqualTo(Currency.USD);
        assertThat(result.to()).isEqualTo(Currency.USD);
        assertThat(result.amount()).isEqualTo(new BigDecimal("100"));
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("100"));
        assertThat(result.rate()).isEqualTo(BigDecimal.ONE);
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("USD → KRW 변환 - 정확한 환율 적용")
    void convertUsdToKrw() {
        // given
        BigDecimal usdToKrwRate = new BigDecimal("1320.50");
        mockExchangeRateData(Map.of(
            Currency.KRW, new RateInfo(usdToKrwRate, new BigDecimal("0.5"))
        ));

        ConvertExchangeRateCommand command = new ConvertExchangeRateCommand(
            Currency.USD, Currency.KRW, new BigDecimal("100")
        );

        // when
        ConvertExchangeRateResult result = exchangeRateService.convertExchangeRate(command);

        // then
        assertThat(result.from()).isEqualTo(Currency.USD);
        assertThat(result.to()).isEqualTo(Currency.KRW);
        assertThat(result.amount()).isEqualTo(new BigDecimal("100"));
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("132050.00"));
        assertThat(result.rate()).isEqualTo(usdToKrwRate);
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("EUR → JPY 변환 - USD를 경유한 간접 변환")
    void convertEurToJpy() {
        // given
        BigDecimal eurToUsdRate = new BigDecimal("1.1000"); // 1 EUR = 1.1 USD
        BigDecimal jpyToUsdRate = new BigDecimal("150.00"); // 1 USD = 150 JPY

        mockExchangeRateData(Map.of(
            Currency.EUR, new RateInfo(eurToUsdRate, new BigDecimal("0.2")),
            Currency.JPY, new RateInfo(jpyToUsdRate, new BigDecimal("-0.3"))
        ));

        ConvertExchangeRateCommand command = new ConvertExchangeRateCommand(
            Currency.EUR, Currency.JPY, new BigDecimal("100")
        );

        // when
        ConvertExchangeRateResult result = exchangeRateService.convertExchangeRate(command);

        // then
        assertThat(result.from()).isEqualTo(Currency.EUR);
        assertThat(result.to()).isEqualTo(Currency.JPY);
        assertThat(result.amount()).isEqualTo(new BigDecimal("100"));

        // 1 EUR = 1.1 USD, 1 USD = 150 JPY → 1 EUR = 165 JPY
        // 100 EUR = 16500 JPY
        BigDecimal expectedConvertedAmount = new BigDecimal("16500.00");
        assertThat(result.convertedAmount()).isEqualTo(expectedConvertedAmount);

        BigDecimal expectedRate = new BigDecimal("165.0000");
        assertThat(result.rate()).isEqualTo(expectedRate);
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("KRW → USD 변환 - 역환율 적용")
    void convertKrwToUsd() {
        // given
        BigDecimal usdToKrwRate = new BigDecimal("1320.50");
        mockExchangeRateData(Map.of(
            Currency.KRW, new RateInfo(usdToKrwRate, new BigDecimal("0.5"))
        ));

        ConvertExchangeRateCommand command = new ConvertExchangeRateCommand(
            Currency.KRW, Currency.USD, new BigDecimal("132050")
        );

        // when
        ConvertExchangeRateResult result = exchangeRateService.convertExchangeRate(command);

        // then
        assertThat(result.from()).isEqualTo(Currency.KRW);
        assertThat(result.to()).isEqualTo(Currency.USD);
        assertThat(result.amount()).isEqualTo(new BigDecimal("132050"));
        assertThat(result.convertedAmount()).isEqualTo(new BigDecimal("100.00"));

        // 1 USD = 1320.50 KRW → 1 KRW = 1/1320.50 USD ≈ 0.0007574 USD
        BigDecimal expectedRate = new BigDecimal("0.0008");
        assertThat(result.rate()).isEqualByComparingTo(expectedRate);
        assertThat(result.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("null 파라미터 전달시 예외 발생")
    void convertWithNullParameters() {
        // when & then
        assertThatThrownBy(() -> exchangeRateService.convertExchangeRate(
            new ConvertExchangeRateCommand(null, Currency.KRW, new BigDecimal("100"))
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("From currency is required");

        assertThatThrownBy(() -> exchangeRateService.convertExchangeRate(
            new ConvertExchangeRateCommand(Currency.USD, null, new BigDecimal("100"))
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("To currency is required");

        assertThatThrownBy(() -> exchangeRateService.convertExchangeRate(
            new ConvertExchangeRateCommand(Currency.USD, Currency.KRW, null)
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount is required");
    }

    @Test
    @DisplayName("음수 금액 전달시 예외 발생")
    void convertWithNegativeAmount() {
        // when & then
        assertThatThrownBy(() -> exchangeRateService.convertExchangeRate(
            new ConvertExchangeRateCommand(Currency.USD, Currency.KRW, new BigDecimal("-100"))
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Amount must be positive");
    }

    @Test
    @DisplayName("환율 데이터가 없을 때 예외 발생")
    void convertWhenExchangeRateDataNotAvailable() {
        // given
        given(exchangeRateRepository.findCurrentRates(Currency.USD))
            .willReturn(Optional.empty());

        ConvertExchangeRateCommand command = new ConvertExchangeRateCommand(
            Currency.USD, Currency.KRW, new BigDecimal("100")
        );

        // when & then
        assertThatThrownBy(() -> exchangeRateService.convertExchangeRate(command))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Exchange rate data not available");
    }

    @Test
    @DisplayName("지원하지 않는 통화에 대한 환율 요청시 예외 발생")
    void convertUnsupportedCurrency() {
        // given
        mockExchangeRateData(Map.of(
            Currency.KRW, new RateInfo(new BigDecimal("1320.50"), new BigDecimal("0.5"))
        ));

        ConvertExchangeRateCommand command = new ConvertExchangeRateCommand(
            Currency.USD, Currency.EUR, new BigDecimal("100")
        );

        // when & then
        assertThatThrownBy(() -> exchangeRateService.convertExchangeRate(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Exchange rate not available for currency: EUR");
    }

    private void mockExchangeRateData(Map<Currency, RateInfo> rates) {
        CurrentExchangeRateResult mockData = new CurrentExchangeRateResult(
            Currency.USD,
            LocalDateTime.now(),
            rates.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    entry -> entry.getKey().getCode(),
                    Map.Entry::getValue
                ))
        );

        given(exchangeRateRepository.findCurrentRates(Currency.USD))
            .willReturn(Optional.of(new com.txnow.domain.exchange.model.CurrentExchangeRates(
                Currency.USD,
                LocalDateTime.now(),
                rates
            )));
    }
}