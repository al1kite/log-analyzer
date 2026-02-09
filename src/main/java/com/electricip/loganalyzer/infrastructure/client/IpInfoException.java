package com.electricip.loganalyzer.infrastructure.client;

/**
 * ipinfo API 호출 시 발생하는 기본 예외
 */
public class IpInfoException extends RuntimeException {

    public IpInfoException(String message) {
        super(message);
    }

    public IpInfoException(String message, Throwable cause) {
        super(message, cause);
    }
}
