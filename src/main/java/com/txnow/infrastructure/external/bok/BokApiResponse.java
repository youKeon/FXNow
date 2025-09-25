package com.txnow.infrastructure.external.bok;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BokApiResponse(
    @JsonProperty("StatisticSearch")
    StatisticSearch statisticSearch
) {

    public record StatisticSearch(
        @JsonProperty("list_total_count")
        int listTotalCount,

        @JsonProperty("RESULT")
        Result result,

        @JsonProperty("row")
        List<ExchangeRateData> rows
    ) {}

    public record Result(
        @JsonProperty("RESULT_CODE")
        String resultCode,

        @JsonProperty("RESULT_MESSAGE")
        String resultMessage
    ) {}

    public record ExchangeRateData(
        @JsonProperty("STAT_NAME")
        String statName,

        @JsonProperty("STAT_CODE")
        String statCode,

        @JsonProperty("ITEM_CODE1")
        String itemCode1,

        @JsonProperty("ITEM_NAME1")
        String itemName1,

        @JsonProperty("DATA_VALUE")
        String dataValue,

        @JsonProperty("TIME")
        String time
    ) {}
}