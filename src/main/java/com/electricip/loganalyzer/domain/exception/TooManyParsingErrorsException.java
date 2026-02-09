package com.electricip.loganalyzer.domain.exception;

import com.electricip.loganalyzer.domain.ParseError;

import java.util.List;

/**
 * 파싱 에러가 과다하여 유효한 로그가 없을 때 발생하는 예외
 */
public class TooManyParsingErrorsException extends LogAnalyzerException {

    private final long totalLines;
    private final long errorCount;
    private final List<ParseError> errors;

    public TooManyParsingErrorsException(String message, long totalLines, long errorCount) {
        this(message, totalLines, errorCount, List.of());
    }

    public TooManyParsingErrorsException(String message, long totalLines, long errorCount,
                                         List<ParseError> errors) {
        super("TOO_MANY_PARSING_ERRORS", message);
        this.totalLines = totalLines;
        this.errorCount = errorCount;
        this.errors = (errors != null) ? List.copyOf(errors) : List.of();
    }

    public long getTotalLines() {
        return totalLines;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public List<ParseError> getErrors() {
        return errors;
    }
}
