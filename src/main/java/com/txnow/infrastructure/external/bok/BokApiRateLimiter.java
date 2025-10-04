package com.txnow.infrastructure.external.bok;

import com.txnow.domain.exchange.exception.ExchangeRateUnavailableException;
import com.txnow.infrastructure.cache.CacheKeyGenerator;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BokApiRateLimiter {

    private static final int MAX_CALLS_PER_WINDOW = 300;
    private static final long WINDOW_SIZE_SECONDS = 30 * 60; // 30분

    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    /**
     * API 호출 기록
     */
    public void acquirePermit() {
        String rateLimitKey = cacheKeyGenerator.bokApiRateLimitKey();

        // 1. 오래된 기록 제거 (30분 이전)
        cleanupOldRecords(rateLimitKey);

        // 2. 현재 window 내 호출 횟수 확인
        Long callCount = redisTemplate.opsForZSet().zCard(rateLimitKey);
        int count = callCount != null ? callCount.intValue() : 0;

        // 3. Rate limit 초과 시 예외 발생
        if (count >= MAX_CALLS_PER_WINDOW) {
            log.error("BOK API rate limit exceeded: {}/{}", count, MAX_CALLS_PER_WINDOW);
            throw new ExchangeRateUnavailableException(
                count,
                MAX_CALLS_PER_WINDOW,
                WINDOW_SIZE_SECONDS
            );
        }

        // 4. 호출 기록 추가
        long now = Instant.now().getEpochSecond();
        String callId = UUID.randomUUID().toString();
        redisTemplate.opsForZSet().add(rateLimitKey, callId, now);

        // TTL 설정 (자동 만료)
        redisTemplate.expire(rateLimitKey, WINDOW_SIZE_SECONDS * 2, TimeUnit.SECONDS);

        log.debug("BOK API call recorded. Total calls: {}/{}", count + 1, MAX_CALLS_PER_WINDOW);
    }

    /**
     * 30분 이전의 호출 기록 제거
     */
    private void cleanupOldRecords(String rateLimitKey) {
        long cutoff = Instant.now().getEpochSecond() - WINDOW_SIZE_SECONDS;
        redisTemplate.opsForZSet().removeRangeByScore(rateLimitKey, 0, cutoff);
    }

    /**
     * 현재 window 내 호출 횟수 조회 (전체 인스턴스)
     */
    public int getCurrentCallCount() {
        String rateLimitKey = cacheKeyGenerator.bokApiRateLimitKey();
        cleanupOldRecords(rateLimitKey);
        Long count = redisTemplate.opsForZSet().zCard(rateLimitKey);
        return count != null ? count.intValue() : 0;
    }

    /**
     * Rate limit 여유 확인
     */
    public boolean hasCapacity() {
        String rateLimitKey = cacheKeyGenerator.bokApiRateLimitKey();
        cleanupOldRecords(rateLimitKey);
        Long count = redisTemplate.opsForZSet().zCard(rateLimitKey);
        return count == null || count < MAX_CALLS_PER_WINDOW;
    }
}
