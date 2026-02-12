package com.electricip.loganalyzer.domain.exception;

import lombok.Getter;

/**
 * 애플리케이션 수준의 API rate limit 초과 예외
 */
@Getter
public class ApiRateLimitExceededException extends LogAnalyzerException {

    private final int retryAfterSeconds;

    public ApiRateLimitExceededException(String message, int retryAfterSeconds) {
        super("API_RATE_LIMIT_EXCEEDED", message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

}
