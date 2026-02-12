package com.electricip.loganalyzer.domain.exception;

/**
 * 애플리케이션 수준의 API rate limit 초과 예외
 */
public class ApiRateLimitExceededException extends LogAnalyzerException {

    public ApiRateLimitExceededException(String message) {
        super("API_RATE_LIMIT_EXCEEDED", message);
    }
}
