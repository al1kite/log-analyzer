package com.electricip.loganalyzer.infrastructure.client;

import com.electricip.loganalyzer.domain.exception.LogAnalyzerException;

/**
 * ipinfo API 호출 시 발생하는 기본 예외
 */
public class IpInfoException extends LogAnalyzerException {

    public IpInfoException(String message) {
        super("IPINFO_ERROR", message);
    }

    public IpInfoException(String message, Throwable cause) {
        super("IPINFO_ERROR", message, cause);
    }

    protected IpInfoException(String errorCode, String message) {
        super(errorCode, message);
    }
}
