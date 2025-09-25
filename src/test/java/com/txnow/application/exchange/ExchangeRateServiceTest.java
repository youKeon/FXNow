package com.txnow.application.exchange;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.CurrentExchangeRate;
import com.txnow.domain.exchange.model.CurrentExchangeRateData;
import com.txnow.domain.exchange.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    void getCurrentRates_ShouldReturnCurrentRates() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        CurrentExchangeRate krwRate = new CurrentExchangeRate(
            Currency.KRW,
            new BigDecimal("1320.50"),
            new BigDecimal("-0.8"),
            now
        );

        Map<Currency, CurrentExchangeRate> rates = Map.of(Currency.KRW, krwRate);
        CurrentExchangeRateData mockData = new CurrentExchangeRateData(Currency.USD, now, rates);

        when(exchangeRateRepository.findCurrentRates(Currency.USD)).thenReturn(Optional.of(mockData));

        // When
        var result = exchangeRateService.getCurrentRates();

        // Then
        assertNotNull(result);
        assertEquals(Currency.USD, result.baseCurrency());
        assertEquals(1, result.rates().size());
        assertTrue(result.rates().containsKey("KRW"));

        var krwInfo = result.rates().get("KRW");
        assertEquals(new BigDecimal("1320.50"), krwInfo.rate());
        assertEquals(new BigDecimal("-0.8"), krwInfo.change());

        verify(exchangeRateRepository, times(1)).findCurrentRates(Currency.USD);
    }

    @Test
    void getCurrentRates_ShouldThrowExceptionWhenDataNotAvailable() {
        // Given
        when(exchangeRateRepository.findCurrentRates(Currency.USD)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> exchangeRateService.getCurrentRates());
        verify(exchangeRateRepository, times(1)).findCurrentRates(Currency.USD);
    }

    @Test
    void getSupportedCurrencies_ShouldReturnAllCurrencies() {
        // When
        var result = exchangeRateService.getSupportedCurrencies();

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
        assertEquals(Currency.values().length, result.length);
    }
}