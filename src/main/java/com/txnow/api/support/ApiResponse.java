package com.txnow.api.support;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "API 공통 응답 형식")
public record ApiResponse<T>(
    @Schema(description = "성공 여부", example = "true")
    boolean success,

    @Schema(description = "응답 데이터")
    T data,

    @Schema(description = "에러 코드", example = "EXCHANGE_RATE_NOT_FOUND")
    String errorCode,

    @Schema(description = "에러 메시지", example = "Exchange rate not found for currency: USD")
    String errorMessage
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return error("INTERNAL_ERROR", message);
    }
}