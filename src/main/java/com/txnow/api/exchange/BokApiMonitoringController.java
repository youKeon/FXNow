package com.txnow.api.exchange;

import com.txnow.api.support.ApiResponse;
import com.txnow.infrastructure.external.bok.BokApiRateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BOK API 모니터링 컨트롤러
 */
@Tag(name = "BOK API Monitoring", description = "BOK API Rate Limit 모니터링")
@RestController
@RequestMapping("/api/monitoring/bok")
@RequiredArgsConstructor
public class BokApiMonitoringController {

    private final BokApiRateLimiter rateLimiter;

    @Operation(summary = "BOK API Rate Limit 상태 조회", description = "현재 30분 window 내 호출 횟수 및 여유 확인")
    @GetMapping("/rate-limit")
    public ApiResponse<RateLimitStatus> getRateLimitStatus() {
        int currentCalls = rateLimiter.getCurrentCallCount();
        boolean hasCapacity = rateLimiter.hasCapacity();
        int remainingCalls = 300 - currentCalls;

        RateLimitStatus status = new RateLimitStatus(
            currentCalls,
            300,
            remainingCalls,
            hasCapacity
        );

        return ApiResponse.success(status);
    }

    /**
     * Rate Limit 상태 응답 DTO
     */
    public record RateLimitStatus(
        int currentCalls,
        int maxCallsPerWindow,
        int remainingCalls,
        boolean hasCapacity
    ) {}
}
