package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.config.RateLimitProperties;
import com.electricip.loganalyzer.domain.exception.ApiRateLimitExceededException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP별 분당 요청 수를 제한하는 서비스
 */
@Component
public class RateLimitService {

    private final RateLimitProperties properties;
    private final Cache<String, AtomicInteger> requestCounts;

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
        this.requestCounts = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    /**
     * 요청 한도를 확인하고 카운트를 증가시킨다.
     * 한도 초과 시 {@link ApiRateLimitExceededException}을 던진다.
     */
    public void checkRateLimit(String clientIp) {
        if (!properties.enabled()) {
            return;
        }

        AtomicInteger counter = requestCounts.get(clientIp, key -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        if (current > properties.maxRequestsPerMinute()) {
            throw new ApiRateLimitExceededException(
                    String.format("IP %s의 요청이 분당 %d회 한도를 초과했습니다",
                            clientIp, properties.maxRequestsPerMinute()));
        }
    }

    /**
     * 해당 IP의 잔여 요청 수를 반환한다.
     */
    public int getRemainingRequests(String clientIp) {
        if (!properties.enabled()) {
            return properties.maxRequestsPerMinute();
        }

        AtomicInteger counter = requestCounts.getIfPresent(clientIp);
        int used = (counter != null) ? counter.get() : 0;
        return Math.max(0, properties.maxRequestsPerMinute() - used);
    }
}
