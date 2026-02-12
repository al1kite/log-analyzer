package com.electricip.loganalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Rate Limit 설정 프로퍼티
 *
 * @param maxRequestsPerMinute IP당 분당 최대 요청 수
 * @param enabled              Rate Limit 활성화 여부
 */
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        @DefaultValue("10") int maxRequestsPerMinute,
        @DefaultValue("true") boolean enabled
) {

    public RateLimitProperties {
        if (maxRequestsPerMinute <= 0) {
            throw new IllegalArgumentException(
                    "maxRequestsPerMinute는 양수여야 합니다: " + maxRequestsPerMinute);
        }
    }
}
