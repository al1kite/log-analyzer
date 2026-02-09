package com.electricip.loganalyzer.domain;

import com.electricip.loganalyzer.domain.exception.LogAnalyzerException;

import java.util.List;

/**
 * CSV 형식이 올바르지 않을 때 발생하는 도메인 예외
 */
public class InvalidCsvFormatException extends LogAnalyzerException {

    private final List<String> missingHeaders;

    public InvalidCsvFormatException(String message, List<String> missingHeaders) {
        super("INVALID_CSV_FORMAT", message);
        this.missingHeaders = (missingHeaders != null) ? List.copyOf(missingHeaders) : List.of();
    }

    public InvalidCsvFormatException(String message) {
        this(message, List.of());
    }

    public List<String> getMissingHeaders() {
        return missingHeaders;
    }
}
