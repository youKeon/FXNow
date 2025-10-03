package com.txnow.infrastructure.external.bok;

import com.txnow.domain.exchange.exception.ExchangeRateUnavailableException;
import com.txnow.domain.exchange.model.ChartPeriod;
import com.txnow.domain.exchange.model.Currency;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class BokApiClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String apiKey;
    private final String statCode;
    private final WebClient webClient;
    private final BokApiRateLimiter rateLimiter;

    public BokApiClient(
        @Value("${bok.api.base-url}") String baseUrl,
        @Value("${bok.api.key}") String apiKey,
        @Value("${bok.api.stat-code}") String statCode,
        BokApiRateLimiter rateLimiter
    ) {
        this.apiKey = apiKey;
        this.statCode = statCode;
        this.rateLimiter = rateLimiter;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public BokApiResponse getTodayExchangeRate(String currencyCode) {
        LocalDate today = LocalDate.now();

        try {
            rateLimiter.acquirePermit();

            String url = buildApiUrl(currencyCode, today, today, 1);

            BokApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(BokApiResponse.class)
                .block();

            validateApiResponse(response, currencyCode);
            return response;
        } catch (ExchangeRateUnavailableException e) {
            // 오늘 데이터가 없으면 최근 7일 범위에서 조회
            return getExchangeRate(currencyCode, ChartPeriod.ONE_WEEK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateUnavailableException(
                Currency.valueOf(currencyCode),
                "BOK API rate limit exceeded"
            );
        }
    }

    /**
     * 기간별 환율 조회
     */
    public BokApiResponse getExchangeRate(String currencyCode, ChartPeriod period) {
        try {
            rateLimiter.acquirePermit();

            LocalDate startDate = period.getStartDate();
            LocalDate endDate = period.getEndDate();
            int count = period.getRequiredDataCount();

            String url = buildApiUrl(currencyCode, startDate, endDate, count);

            BokApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(BokApiResponse.class)
                .block();

            validateApiResponse(response, currencyCode);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateUnavailableException(
                Currency.valueOf(currencyCode),
                "BOK API rate limit exceeded"
            );
        }
    }

    private String buildApiUrl(String currencyCode, LocalDate startDate, LocalDate endDate,
        int count) {
        return String.format("/StatisticSearch/%s/json/kr/%d/%d/%s/D/%s/%s/%s",
            apiKey,
            1,
            count,
            statCode,
            startDate.format(DATE_FORMATTER),
            endDate.format(DATE_FORMATTER),
            currencyCode
        );
    }

    /**
     * BOK API 응답 검증
     */
    private void validateApiResponse(BokApiResponse response, String currencyCode) {
        if (response == null) {
            log.error("BOK API returned null response for {}", currencyCode);
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "No response from BOK API");
        }

        if (response.statisticSearch() == null) {
            log.error("BOK API statisticSearch is null for {}. Response might contain error.",
                currencyCode);
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "Invalid response format from BOK API (statisticSearch is null)");
        }

        var result = response.statisticSearch().result();
        if (result != null && !"200".equals(result.resultCode())) {
            log.error("BOK API error for {}: {} - {}", currencyCode,
                result.resultCode(), result.resultMessage());
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "BOK API error: " + result.resultMessage());
        }

        if (response.statisticSearch().rows() == null || response.statisticSearch().rows()
            .isEmpty()) {
            log.error("BOK API returned no data for {}", currencyCode);
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "No exchange rate data available from BOK API");
        }
    }
}
