package com.electricip.loganalyzer.domain.exception;

/**
 * 로그 파싱 중 발생하는 일반 예외
 */
public class LogParsingException extends LogAnalyzerException {

    private final long lineNumber;

    public LogParsingException(String message) {
        super("LOG_PARSING_ERROR", message);
        this.lineNumber = -1;
    }

    public LogParsingException(String message, long lineNumber) {
        super("LOG_PARSING_ERROR", message);
        this.lineNumber = lineNumber;
    }

    public LogParsingException(String message, Throwable cause) {
        super("LOG_PARSING_ERROR", message, cause);
        this.lineNumber = -1;
    }

    public long getLineNumber() {
        return lineNumber;
    }
}
