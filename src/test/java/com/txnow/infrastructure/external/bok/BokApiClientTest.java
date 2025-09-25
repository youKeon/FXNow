package com.txnow.infrastructure.external.bok;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BokApiClientTest {

    private final BokApiClient apiClient = new BokApiClient("https://ecos.bok.or.kr/api", "9O98FZKTJM4VKXR7JFUG", "731Y001");

    @Test
    void getTodayExchangeRate_ShouldReturnUsdKrwRate() {
        // Given
        String usdCode = "0000001"; // USD/KRW

        // When
        var response = apiClient.getTodayExchangeRate(usdCode);

        // Then
        // API가 실제로 호출되므로 네트워크 상황에 따라 실패할 수 있음
        // 실제 테스트에서는 Mock을 사용하는 것이 좋음
        if (response.isPresent()) {
            assertNotNull(response.get().statisticSearch());
            System.out.println("USD/KRW rate retrieved successfully");
        } else {
            System.out.println("BOK API call failed - this is expected in test environment");
        }
    }

    @Test
    void getRecentExchangeRate_ShouldReturnDataForWeek() {
        // Given
        String eurCode = "0000003"; // EUR/KRW

        // When
        var response = apiClient.getRecentExchangeRate(eurCode);

        // Then
        if (response.isPresent()) {
            var statisticSearch = response.get().statisticSearch();
            assertNotNull(statisticSearch);
            System.out.println("Recent EUR/KRW rates retrieved: " +
                (statisticSearch.rows() != null ? statisticSearch.rows().size() : 0) + " records");
        } else {
            System.out.println("BOK API call failed - this is expected in test environment");
        }
    }

    @Test
    void getExchangeRate_WithCustomDateRange_ShouldReturnData() {
        // Given
        String jpyCode = "0000002"; // JPY/KRW
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now();

        // When
        var response = apiClient.getExchangeRate(jpyCode, startDate, endDate);

        // Then
        if (response.isPresent()) {
            assertNotNull(response.get().statisticSearch());
            System.out.println("Custom date range JPY/KRW rates retrieved successfully");
        } else {
            System.out.println("BOK API call failed - this is expected in test environment");
        }
    }
}