package com.electricip.loganalyzer.domain.exception;

/**
 * 유효하지 않은 파일 업로드 시 발생하는 예외
 * (빈 파일, 지원하지 않는 확장자 등)
 */
public class InvalidFileException extends LogAnalyzerException {

    public InvalidFileException(String message) {
        super("INVALID_FILE", message);
    }

    protected InvalidFileException(String errorCode, String message) {
        super(errorCode, message);
    }
}
