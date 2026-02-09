package com.electricip.loganalyzer.domain.exception;

import java.util.Objects;

/**
 * 로그 분석 애플리케이션의 최상위 예외
 * — 모든 도메인/인프라 예외가 이 클래스를 상속하여 errorCode를 제공한다
 */
public abstract class LogAnalyzerException extends RuntimeException {

    private final String errorCode;

    protected LogAnalyzerException(String errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode는 null일 수 없습니다");
    }

    protected LogAnalyzerException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode는 null일 수 없습니다");
    }

    public String getErrorCode() {
        return errorCode;
    }
}
