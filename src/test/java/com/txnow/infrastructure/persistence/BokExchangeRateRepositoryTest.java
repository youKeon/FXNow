package com.txnow.infrastructure.persistence;

import com.txnow.domain.exchange.model.Currency;
import com.txnow.infrastructure.external.bok.BokApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BokExchangeRateRepositoryTest {

    @Mock
    private BokApiClient bokApiClient;

    @InjectMocks
    private BokExchangeRateRepository bokExchangeRateRepository;

    @Test
    void findCurrentRates_WithKrwBase_ShouldReturnRates() {
        // Given
        when(bokApiClient.getRecentExchangeRate(anyString())).thenReturn(Optional.empty());

        // When
        var result = bokExchangeRateRepository.findCurrentRates(Currency.KRW);

        // Then
        assertTrue(result.isPresent());
        assertEquals(Currency.KRW, result.get().baseCurrency());
        // Fallback 데이터가 반환되어야 함
        assertFalse(result.get().rates().isEmpty());
    }

    @Test
    void findCurrentRates_WithUsdBase_ShouldConvertFromKrw() {
        // Given
        when(bokApiClient.getRecentExchangeRate(anyString())).thenReturn(Optional.empty());

        // When
        var result = bokExchangeRateRepository.findCurrentRates(Currency.USD);

        // Then
        assertTrue(result.isPresent());
        assertEquals(Currency.USD, result.get().baseCurrency());
        assertTrue(result.get().rates().containsKey(Currency.KRW));
    }

    @Test
    void findCurrentRates_WhenApiReturnsEmpty_ShouldReturnFallbackData() {
        // Given
        when(bokApiClient.getRecentExchangeRate(anyString())).thenReturn(Optional.empty());

        // When
        var result = bokExchangeRateRepository.findCurrentRates(Currency.KRW);

        // Then
        assertTrue(result.isPresent());
        assertNotNull(result.get().rates());
        // Fallback 데이터가 있는지 확인
        assertTrue(result.get().rates().size() > 0);
    }
}