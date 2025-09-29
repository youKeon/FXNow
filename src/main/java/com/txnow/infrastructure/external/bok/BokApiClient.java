package com.txnow.infrastructure.external.bok;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Component
public class BokApiClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String apiKey;
    private final String statCode;
    private final WebClient webClient;

    public BokApiClient(
        @Value("${bok.api.base-url}") String baseUrl,
        @Value("${bok.api.key}") String apiKey,
        @Value("${bok.api.stat-code}") String statCode
    ) {
        this.apiKey = apiKey;
        this.statCode = statCode;
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public Optional<BokApiResponse> getExchangeRate(
        String currencyCode,
        LocalDate startDate,
        LocalDate endDate
    ) {
        String url = buildApiUrl(currencyCode, startDate, endDate);
        log.info("Calling BOK API: {}", url);

        BokApiResponse response = webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(BokApiResponse.class)
            .block();

        if (response != null && response.statisticSearch() != null) {
            var result = response.statisticSearch().result();
            if (result != null && !"200".equals(result.resultCode())) {
                return Optional.empty();
            }
        }

        return Optional.ofNullable(response);
    }

    /**
     * 오늘 날짜의 환율 데이터를 조회합니다.
     */
    public Optional<BokApiResponse> getTodayExchangeRate(String currencyCode) {
        LocalDate today = LocalDate.now();
        return getExchangeRate(currencyCode, today, today);
    }

    /**
     * 최근 7일간의 환율 데이터
     */
    public Optional<BokApiResponse> getRecentExchangeRate(String currencyCode) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        return getExchangeRate(currencyCode, weekAgo, today);
    }

    /**
     * 기간별 환율 히스토리 데이터
     */
    public Optional<BokApiResponse> getExchangeRateHistory(String currencyCode,
        ChartPeriod period) {
        LocalDate startDate = period.getStartDate();
        LocalDate endDate = period.getEndDate();
        int requiredDataCount = period.getRequiredDataCount();

        log.info("Requesting {} exchange rate history for period: {} ({} to {})",
            currencyCode, period.getCode(), startDate, endDate);

        try {
            String url = buildApiUrl(currencyCode, startDate, endDate, requiredDataCount);
            log.info("Calling BOK API for history: {}", url);

            BokApiResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(BokApiResponse.class)
                .block();

            if (response != null && response.statisticSearch() != null) {
                var result = response.statisticSearch().result();
                if (result != null && !"200".equals(result.resultCode())) {
                    log.warn("BOK API returned error for history: {} - {}", result.resultCode(),
                        result.resultMessage());
                    return Optional.empty();
                }
            }

            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Error calling BOK API for currency history: {} period: {}", currencyCode,
                period.getCode(), e);
            return Optional.empty();
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