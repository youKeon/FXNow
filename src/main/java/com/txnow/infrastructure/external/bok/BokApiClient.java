package com.txnow.infrastructure.external.bok;

import com.txnow.domain.exchange.exception.ExchangeRateNotFoundException;
import com.txnow.domain.exchange.exception.ExchangeRateUnavailableException;
import com.txnow.domain.exchange.model.ChartPeriod;
import com.txnow.domain.exchange.model.Currency;
import com.txnow.domain.exchange.model.DailyRate;
import com.txnow.domain.exchange.provider.ExchangeRateProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class BokApiClient implements ExchangeRateProvider {

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

    @Override
    public BigDecimal getCurrentExchangeRate(Currency currency) {
        if (!currency.isSupportedCurrency()) {
            throw new ExchangeRateNotFoundException(currency, "Currency not supported by BOK API");
        }

        String bokCode = currency.getBokCode();
        LocalDate today = LocalDate.now();

        // Rate limit check
        rateLimiter.acquirePermit();

        // API 호출
        String url = buildApiUrl(bokCode, today, today, 1);
        BokApiResponse response = webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(BokApiResponse.class)
            .block();

        // 응답 검증
        BokApiResponse validatedResponse = validateApiResponse(response, bokCode);
        if (validatedResponse == null) {
            // INFO-200: 공휴일 등 정상적인 데이터 부재
            log.info("BOK API has no data for {} (holiday). Returning null for DB fallback.", currency);
            return null;
        }

        // 데이터 파싱
        var row = validatedResponse.statisticSearch().rows().getFirst();
        BigDecimal rate = new BigDecimal(row.dataValue());
        return currency.normalizeFromBokApi(rate);
    }

    @Override
    public List<DailyRate> getExchangeRateHistory(Currency currency, ChartPeriod period) {
        if (!currency.isSupportedCurrency()) {
            throw new ExchangeRateNotFoundException(currency, "Currency not supported by BOK API");
        }

        String bokCode = currency.getBokCode();
        log.info("Fetching history from BOK API: {} period: {}", currency, period);

        // Rate limiting
        rateLimiter.acquirePermit();

        // API 호출
        LocalDate startDate = period.getStartDate();
        LocalDate endDate = period.getEndDate();
        int count = period.getRequiredDataCount();

        String url = buildApiUrl(bokCode, startDate, endDate, count);
        BokApiResponse response = webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(BokApiResponse.class)
            .block();

        // 응답 검증
        BokApiResponse validatedResponse = validateApiResponse(response, bokCode);
        if (validatedResponse == null) {
            // INFO-200: 데이터 없음 (차트 조회에서는 예외 발생)
            throw new ExchangeRateUnavailableException(
                currency,
                "No chart data available from BOK API"
            );
        }

        // 데이터 파싱
        var statisticSearch = validatedResponse.statisticSearch();
        List<DailyRate> dailyRates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (var row : statisticSearch.rows()) {
            LocalDate date = LocalDate.parse(row.time(), formatter);
            BigDecimal rate = new BigDecimal(row.dataValue());
            rate = currency.normalizeFromBokApi(rate);
            dailyRates.add(new DailyRate(date, rate));
        }

        if (dailyRates.isEmpty()) {
            throw new ExchangeRateNotFoundException(currency, "No valid history data parsed");
        }

        dailyRates.sort(Comparator.comparing(DailyRate::date));
        return dailyRates;
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
     * @return null if INFO-200 (holiday, no data available), otherwise validated response
     */
    private BokApiResponse validateApiResponse(BokApiResponse response, String currencyCode) {
        if (response == null) {
            log.error("BOK API returned null response for {}", currencyCode);
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "No response from BOK API");
        }

        // 2. 에러 응답 체크
        if (response.isError()) {
            String errorCode = response.result().resultCode();
            String errorMessage = response.result().resultMessage();

            // INFO-200: 공휴일 등 정상적인 데이터 부재
            if ("INFO-200".equals(errorCode)) {
                log.info("BOK API has no data for {} (holiday or no data available)", currencyCode);
                return null;  // DB fallback을 위해 null 반환
            }

            // 다른 에러들은 예외 발생
            Currency currency = Currency.valueOf(currencyCode);
            switch (errorCode) {
                case "INFO-100" -> {
                    log.error("BOK API authentication failed for {}: Invalid API key", currencyCode);
                    throw new ExchangeRateUnavailableException(currency,
                        "BOK API authentication failed: " + errorMessage);
                }
                case "ERROR-100", "ERROR-101", "ERROR-200", "ERROR-300", "ERROR-301" -> {
                    log.error("BOK API validation error for {}: {} - {}", currencyCode, errorCode, errorMessage);
                    throw new ExchangeRateUnavailableException(currency,
                        "Invalid request parameters: " + errorMessage);
                }
                case "ERROR-400" -> {
                    log.error("BOK API timeout for {}: Search range too large", currencyCode);
                    throw new ExchangeRateUnavailableException(currency,
                        "Request timeout: " + errorMessage);
                }
                case "ERROR-500" -> {
                    log.error("BOK API server error for {}: {}", currencyCode, errorMessage);
                    throw new ExchangeRateUnavailableException(currency,
                        "BOK API server error: " + errorMessage);
                }
                case "ERROR-600", "ERROR-601" -> {
                    log.error("BOK API database error for {}: {} - {}", currencyCode, errorCode, errorMessage);
                    throw new ExchangeRateUnavailableException(currency,
                        "BOK API database error: " + errorMessage);
                }
                case "ERROR-602" -> {
                    log.error("BOK API rate limit exceeded for {}: {}", currencyCode, errorMessage);
                    throw new ExchangeRateUnavailableException(currency,
                        "Rate limit exceeded: " + errorMessage);
                }
                default -> {
                    log.error("BOK API unknown error for {}: {} - {}", currencyCode, errorCode, errorMessage);
                    throw new ExchangeRateUnavailableException(currency,
                        "BOK API error: " + errorMessage);
                }
            }
        }

        // 3. StatisticSearch 검증
        if (response.statisticSearch() == null) {
            log.error("BOK API statisticSearch is null for {}", currencyCode);
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "Invalid response format from BOK API");
        }

        // 4. StatisticSearch 내부 RESULT 상태 코드 체크
        var result = response.statisticSearch().result();
        if (result != null && !"INFO-000".equals(result.resultCode())) {
            log.error("BOK API unexpected status code for {}: {} - {}",
                currencyCode, result.resultCode(), result.resultMessage());
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "Unexpected status: " + result.resultMessage());
        }

        // 5. 데이터 행 체크
        if (response.statisticSearch().rows() == null || response.statisticSearch().rows().isEmpty()) {
            log.warn("BOK API returned no data for {} (holiday or no data available)", currencyCode);
            throw new ExchangeRateUnavailableException(Currency.valueOf(currencyCode),
                "No exchange rate data available from BOK API");
        }

        return response;
    }
}
