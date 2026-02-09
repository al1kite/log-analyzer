package com.electricip.loganalyzer.infrastructure.client;

/**
 * ipinfo 서버 오류 시 발생하는 예외 (5xx)
 */
public class IpInfoServerException extends IpInfoException {

    public IpInfoServerException(String message) {
        super(message);
    }
}
