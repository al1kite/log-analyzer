package com.electricip.loganalyzer.infrastructure.client;

/**
 * ipinfo API 인증 실패 시 발생하는 예외 (401)
 */
public class IpInfoAuthException extends IpInfoException {

    public IpInfoAuthException(String message) {
        super(message);
    }
}
