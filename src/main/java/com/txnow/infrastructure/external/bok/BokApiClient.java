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

    public BokApiResponse getExchangeRate(
        String currencyCode,
        LocalDate startDate,
        LocalDate endDate
    ) {
        try {
            // Rate limit 확인 및 대기
            rateLimiter.acquirePermit();

            String url = buildApiUrl(currencyCode, startDate, endDate);
            log.info("Calling BOK API: {} (Recent calls: {})",
                url, rateLimiter.getCurrentCallCount());

            BokApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(BokApiResponse.class)
                .block();

            if (response != null && response.statisticSearch() != null) {
                var result = response.statisticSearch().result();
                if (result != null && !"200".equals(result.resultCode())) {
                    throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                        "No data received from BOK API");
                }
            }

            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "BOK API rate limit exceeded");
        }
    }

    /**
     * 오늘 날짜의 환율 데이터를 조회합니다.
     */
    public BokApiResponse getTodayExchangeRate(String currencyCode) {
        LocalDate today = LocalDate.now();
        return getExchangeRate(currencyCode, today, today);
    }

    /**
     * 최근 7일간의 환율 데이터
     */
    public BokApiResponse getRecentExchangeRate(String currencyCode) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        return getExchangeRate(currencyCode, weekAgo, today);
    }

    /**
     * 기간별 환율 히스토리 데이터
     */
    public BokApiResponse getExchangeRateHistory(
        String currencyCode,
        ChartPeriod period
    ) {
        LocalDate startDate = period.getStartDate();
        LocalDate endDate = period.getEndDate();
        int requiredDataCount = period.getRequiredDataCount();

        try {
            // Rate limit 확인 및 대기
            rateLimiter.acquirePermit();

            String url = buildApiUrl(currencyCode, startDate, endDate, requiredDataCount);

            BokApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(BokApiResponse.class)
                .block();

            if (response != null && response.statisticSearch() != null) {
                var result = response.statisticSearch().result();
                if (result != null && !"200".equals(result.resultCode())) {
                    throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                        "BOK API rate limit exceeded");
                }
            }

            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "BOK API rate limit exceeded");
        }
    }

    private String buildApiUrl(String currencyCode, LocalDate startDate, LocalDate endDate) {
        if (startDate.equals(endDate)) {
            return buildApiUrl(currencyCode, startDate, endDate, 1);
        }

        return buildApiUrl(currencyCode, startDate, endDate, 100);
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
}