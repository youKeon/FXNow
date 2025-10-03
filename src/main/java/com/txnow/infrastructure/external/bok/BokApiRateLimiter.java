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
    private static final long MIN_DELAY_BETWEEN_CALLS_MS = 6000; // 6초 (안전 마진)
    private static final long MAX_WAIT_TIME_SECONDS = 300; // 5분 이상 대기 시 예외

    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyGenerator cacheKeyGenerator;

    /**
     * API 호출 전 rate limit 확인 및 대기 (분산 환경 지원)
     * @throws InterruptedException Thread.sleep 중단 시
     * @throws ExchangeRateUnavailableException 대기 시간이 5분 초과 시
     */
    public void acquirePermit() throws InterruptedException {
        String rateLimitKey = cacheKeyGenerator.bokApiRateLimitKey();
        String lastCallKey = cacheKeyGenerator.bokApiLastCallKey();

        // 1. 오래된 기록 제거 (30분 이전)
        cleanupOldRecords(rateLimitKey);

        // 2. 현재 window 내 호출 횟수 확인
        Long callCount = redisTemplate.opsForZSet().zCard(rateLimitKey);
        int count = callCount != null ? callCount.intValue() : 0;

        // 3. Rate limit 체크
        if (count >= MAX_CALLS_PER_WINDOW) {
            Set<String> oldestCalls = redisTemplate.opsForZSet().range(rateLimitKey, 0, 0);

            if (oldestCalls != null && !oldestCalls.isEmpty()) {
                String oldestCallId = oldestCalls.iterator().next();
                Double oldestScore = redisTemplate.opsForZSet().score(rateLimitKey, oldestCallId);

                if (oldestScore != null) {
                    long oldestTimestamp = oldestScore.longValue();
                    long now = Instant.now().getEpochSecond();
                    long waitTimeSeconds = WINDOW_SIZE_SECONDS - (now - oldestTimestamp);

                    if (waitTimeSeconds > 0) {
                        // 5분 이상 대기해야 하는 경우 예외 발생
                        if (waitTimeSeconds > MAX_WAIT_TIME_SECONDS) {
                            log.error("BOK API rate limit exceeded. Required wait time: {} seconds (max: {})",
                                waitTimeSeconds, MAX_WAIT_TIME_SECONDS);
                            throw new ExchangeRateUnavailableException(
                                count,
                                MAX_CALLS_PER_WINDOW,
                                waitTimeSeconds
                            );
                        }

                        log.warn("BOK API rate limit reached (across all instances). Waiting {} seconds...",
                            waitTimeSeconds);
                        Thread.sleep(waitTimeSeconds * 1000);
                        cleanupOldRecords(rateLimitKey);
                    }
                }
            }
        }

        // 4. 최소 대기 시간 확보 (6초) - 전역 마지막 호출 시각 확인
        String lastCallStr = redisTemplate.opsForValue().get(lastCallKey);
        if (lastCallStr != null) {
            long lastCallTime = Long.parseLong(lastCallStr);
            long timeSinceLastCall = Instant.now().toEpochMilli() - lastCallTime;

            if (timeSinceLastCall < MIN_DELAY_BETWEEN_CALLS_MS) {
                long waitTime = MIN_DELAY_BETWEEN_CALLS_MS - timeSinceLastCall;
                log.debug("Waiting {} ms before next BOK API call (global rate limit)", waitTime);
                Thread.sleep(waitTime);
            }
        }

        // 5. 호출 기록 추가
        long now = Instant.now().getEpochSecond();
        String callId = UUID.randomUUID().toString();
        redisTemplate.opsForZSet().add(rateLimitKey, callId, now);

        // 마지막 호출 시각 기록
        redisTemplate.opsForValue().set(
            lastCallKey,
            String.valueOf(Instant.now().toEpochMilli()),
            WINDOW_SIZE_SECONDS * 2,
            TimeUnit.SECONDS
        );

        // TTL 설정 (자동 만료)
        redisTemplate.expire(rateLimitKey, WINDOW_SIZE_SECONDS * 2, TimeUnit.SECONDS);

        log.debug("BOK API call permitted. Total calls across all instances: {}", count + 1);
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
