package com.electricip.loganalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Rate Limit 설정 프로퍼티
 *
 * @param maxRequestsPerMinute IP당 윈도우당 최대 요청 수
 * @param enabled              Rate Limit 활성화 여부
 * @param windowSeconds        Rate Limit 윈도우 크기(초)
 */
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        @DefaultValue("10") int maxRequestsPerMinute,
        @DefaultValue("true") boolean enabled,
        @DefaultValue("60") int windowSeconds
) {

    public RateLimitProperties {
        if (maxRequestsPerMinute <= 0) {
            throw new IllegalArgumentException(
                    "maxRequestsPerMinute는 양수여야 합니다: " + maxRequestsPerMinute);
        }
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException(
                    "windowSeconds는 양수여야 합니다: " + windowSeconds);
        }
    }
}
