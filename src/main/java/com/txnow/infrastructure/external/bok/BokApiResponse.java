package com.txnow.infrastructure.external.bok;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BokApiResponse(
    @JsonProperty("StatisticSearch")
    StatisticSearch statisticSearch,

    @JsonProperty("RESULT")
    Result result  // 에러 응답용
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
        @JsonProperty("CODE")
        String resultCode,

        @JsonProperty("MESSAGE")
        String resultMessage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExchangeRateData(
        @JsonProperty("DATA_VALUE")
        String dataValue,

        @JsonProperty("TIME")
        String time
    ) {}

    /**
     * 에러 응답인지 확인
     */
    public boolean isError() {
        return statisticSearch == null && result != null;
    }
}
