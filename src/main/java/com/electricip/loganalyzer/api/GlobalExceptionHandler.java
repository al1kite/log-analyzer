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
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 전역 예외 처리기
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

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
    public ResponseEntity<ParsingErrorResponse> handleTooManyParsingErrors(
            TooManyParsingErrorsException e, HttpServletRequest request) {

        log.error("파싱 에러 과다: {}", e.getMessage());

        var errorSamples = e.getErrors().stream()
                .map(err -> new ParsingErrorResponse.ParseErrorSample(
                        err.lineNumber(), err.errorMessage(), err.errorType().name()))
                .toList();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ParsingErrorResponse.of(
                        e.getErrorCode(),
                        e.getMessage(),
                        request.getRequestURI(),
                        e.getTotalLines(),
                        e.getErrorCount(),
                        errorSamples
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

        var maxBytes = e.getMaxUploadSize();
        var message = (maxBytes > 0)
                ? String.format("파일 크기 초과 (최대 %dMB)", maxBytes / (1024 * 1024))
                : "파일 크기 초과";

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "FILE_TOO_LARGE",
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * 요청 본문 검증 실패 (@Valid) → 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        var fieldError = e.getBindingResult().getFieldError();
        var message = (fieldError != null)
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : e.getMessage();

        log.error("요청 검증 실패: {}", message);

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * 요청 본문 파싱 실패 (잘못된 JSON 등) → 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {

        log.error("요청 본문 파싱 실패: {}", e.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_REQUEST_BODY",
                        "요청 본문을 읽을 수 없습니다",
                        request.getRequestURI()
                ));
    }

    /**
     * 필수 요청 파라미터 누락 → 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e, HttpServletRequest request) {

        log.error("필수 파라미터 누락: {}", e.getParameterName());

        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_PARAMETER",
                        e.getMessage(),
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

}
