package com.electricip.loganalyzer.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 파싱 에러 정보 (Value Object)
 */
public record ParseError(long lineNumber, String errorMessage, ErrorType errorType, LocalDateTime occurredAt) {

    public enum ErrorType {
        PARSING,
        VALIDATION,
        FORMAT
    }

    /**
     * Compact Constructor: 필수 필드 검증
     */
    public ParseError {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber는 1 이상이어야 합니다");
        }
        Objects.requireNonNull(errorMessage, "errorMessage는 null일 수 없습니다");
        Objects.requireNonNull(errorType, "errorType은 null일 수 없습니다");
        Objects.requireNonNull(occurredAt, "occurredAt은 null일 수 없습니다");
    }
}
