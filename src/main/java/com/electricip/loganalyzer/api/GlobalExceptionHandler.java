package com.electricip.loganalyzer.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

/**
 * 전역 예외 처리기
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException e, HttpServletRequest request) {
        
        log.error("잘못된 요청: {}", e.getMessage());
        
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_REQUEST",
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }
    
    /**
     * NullPointerException 처리
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(
            NullPointerException e, HttpServletRequest request) {
        
        log.error("Null 참조: {}", e.getMessage());
        
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "NULL_REFERENCE",
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }
    
    /**
     * 파일 크기 초과
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        
        log.error("파일 크기 초과");
        
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "FILE_TOO_LARGE",
                        "파일 크기 초과 (최대 50MB)",
                        request.getRequestURI()
                ));
    }
    
    /**
     * 일반 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {
        
        log.error("서버 오류: {}", e.getMessage(), e);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_ERROR",
                        "서버 오류가 발생했습니다",
                        request.getRequestURI()
                ));
    }
    
    /**
     * 에러 응답 (Record)
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String errorCode,
            String message,
            String path
    ) {
        public static ErrorResponse of(HttpStatus httpStatus, String errorCode, 
                                      String message, String path) {
            return new ErrorResponse(
                    LocalDateTime.now(),
                    httpStatus.value(),
                    errorCode,
                    message,
                    path
            );
        }
    }
}
