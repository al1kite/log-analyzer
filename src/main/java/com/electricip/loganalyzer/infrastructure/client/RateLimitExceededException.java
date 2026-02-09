package com.electricip.loganalyzer.infrastructure.client;

/**
 * ipinfo API rate limit 초과 시 발생하는 예외 (429)
 */
public class RateLimitExceededException extends IpInfoException {

    public RateLimitExceededException(String message) {
        super("RATE_LIMIT_EXCEEDED", message);
    }
}
