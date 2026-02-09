package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.domain.InvalidCsvFormatException;
import com.electricip.loganalyzer.domain.exception.AnalysisNotFoundException;
import com.electricip.loganalyzer.domain.exception.DuplicateAnalysisIdException;
import com.electricip.loganalyzer.domain.exception.FileTooLargeException;
import com.electricip.loganalyzer.domain.exception.InvalidFileException;
import com.electricip.loganalyzer.domain.exception.LogAnalyzerException;
import com.electricip.loganalyzer.domain.exception.LogParsingException;
import com.electricip.loganalyzer.domain.exception.TooManyParsingErrorsException;
import com.electricip.loganalyzer.infrastructure.client.IpInfoException;
import com.electricip.loganalyzer.infrastructure.client.RateLimitExceededException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final int maxFileSizeMb;

    public GlobalExceptionHandler(
            @Value("${log-analysis.max-file-size-mb:50}") int maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    /**
     * InvalidCsvFormatException 처리 → 400
     */
    @ExceptionHandler(InvalidCsvFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCsvFormat(
            InvalidCsvFormatException e, HttpServletRequest request) {

        log.error("잘못된 CSV 형식: {}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * FileTooLargeException 처리 → 413
     */
    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(
            FileTooLargeException e, HttpServletRequest request) {

        log.error("파일 크기 초과: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * InvalidFileException 처리 → 400
     */
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(
            InvalidFileException e, HttpServletRequest request) {

        log.error("유효하지 않은 파일: {}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * LogParsingException 처리 → 400
     */
    @ExceptionHandler(LogParsingException.class)
    public ResponseEntity<ErrorResponse> handleLogParsing(
            LogParsingException e, HttpServletRequest request) {

        log.error("로그 파싱 오류: {}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * TooManyParsingErrorsException 처리 → 422
     */
    @ExceptionHandler(TooManyParsingErrorsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyParsingErrors(
            TooManyParsingErrorsException e, HttpServletRequest request) {

        log.error("파싱 에러 과다: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * AnalysisNotFoundException 처리 → 404
     */
    @ExceptionHandler(AnalysisNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAnalysisNotFound(
            AnalysisNotFoundException e, HttpServletRequest request) {

        log.error("분석 결과 미발견: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        HttpStatus.NOT_FOUND,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * DuplicateAnalysisIdException 처리 → 409
     */
    @ExceptionHandler(DuplicateAnalysisIdException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateAnalysisId(
            DuplicateAnalysisIdException e, HttpServletRequest request) {

        log.error("중복 분석 ID: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * RateLimitExceededException 처리 → 429
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException e, HttpServletRequest request) {

        log.error("API rate limit 초과: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.of(
                        HttpStatus.TOO_MANY_REQUESTS,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * IpInfoException (나머지) 처리 → 502
     */
    @ExceptionHandler(IpInfoException.class)
    public ResponseEntity<ErrorResponse> handleIpInfoException(
            IpInfoException e, HttpServletRequest request) {

        log.error("IP 정보 조회 오류: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_GATEWAY,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * LogAnalyzerException catch-all → 500
     */
    @ExceptionHandler(LogAnalyzerException.class)
    public ResponseEntity<ErrorResponse> handleLogAnalyzerException(
            LogAnalyzerException e, HttpServletRequest request) {

        log.error("애플리케이션 오류: {}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * 파일 크기 초과 (Spring)
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
                        String.format("파일 크기 초과 (최대 %dMB)", maxFileSizeMb),
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
    @Schema(description = "에러 응답")
    public record ErrorResponse(
            @Schema(description = "에러 발생 시각", example = "2026-02-09T14:30:00")
            LocalDateTime timestamp,
            @Schema(description = "HTTP 상태 코드", example = "400")
            int status,
            @Schema(description = "에러 코드", example = "INVALID_FILE")
            String errorCode,
            @Schema(description = "에러 메시지", example = "파일이 비어있습니다")
            String message,
            @Schema(description = "요청 경로", example = "/api/analysis")
            String path
    ) {
        /**
         * Compact Constructor: 필수 필드 검증 + message null 치환
         */
        public ErrorResponse {
            java.util.Objects.requireNonNull(timestamp, "timestamp는 null일 수 없습니다");
            java.util.Objects.requireNonNull(errorCode, "errorCode는 null일 수 없습니다");
            java.util.Objects.requireNonNull(path, "path는 null일 수 없습니다");
            message = (message != null) ? message : "알 수 없는 오류";
        }

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
