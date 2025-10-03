package com.txnow.infrastructure.external.bok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BokApiResponse(
    @JsonProperty("StatisticSearch")
    StatisticSearch statisticSearch
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatisticSearch(
        @JsonProperty("list_total_count")
        int listTotalCount,

        @JsonProperty("RESULT")
        Result result,

        @JsonProperty("row")
        List<ExchangeRateData> rows
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
        @JsonProperty("RESULT_CODE")
        String resultCode,

        @JsonProperty("RESULT_MESSAGE")
        String resultMessage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExchangeRateData(
        @JsonProperty("DATA_VALUE")
        String dataValue,

        @JsonProperty("TIME")
        String time
    ) {}
}
