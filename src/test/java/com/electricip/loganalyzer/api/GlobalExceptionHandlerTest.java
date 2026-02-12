package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.domain.exception.ApiRateLimitExceededException;
import com.electricip.loganalyzer.domain.exception.InvalidCsvFormatException;
import com.electricip.loganalyzer.domain.ParseError;
import com.electricip.loganalyzer.domain.exception.AnalysisNotFoundException;
import com.electricip.loganalyzer.domain.exception.DuplicateAnalysisIdException;
import com.electricip.loganalyzer.domain.exception.FileTooLargeException;
import com.electricip.loganalyzer.domain.exception.InvalidFileException;
import com.electricip.loganalyzer.domain.exception.LogParsingException;
import com.electricip.loganalyzer.domain.exception.TooManyParsingErrorsException;
import com.electricip.loganalyzer.infrastructure.client.IpInfoException;
import com.electricip.loganalyzer.infrastructure.client.RateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/analysis");

    @Nested
    @DisplayName("InvalidCsvFormatException 핸들러")
    class InvalidCsvFormatTest {

        @Test
        @DisplayName("400 Bad Request로 응답한다")
        void shouldReturn400() {
            var ex = new InvalidCsvFormatException("필수 헤더가 누락되었습니다", List.of("ClientIp"));

            var response = handler.handleInvalidCsvFormat(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INVALID_CSV_FORMAT");
            assertThat(response.getBody().message()).contains("헤더가 누락");
        }

        @Test
        @DisplayName("경로 정보가 응답에 포함된다")
        void shouldIncludePathInResponse() {
            var ex = new InvalidCsvFormatException("test");

            var response = handler.handleInvalidCsvFormat(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().path()).isEqualTo("/api/analysis");
        }
    }

    @Nested
    @DisplayName("InvalidFileException 핸들러")
    class InvalidFileTest {

        @Test
        @DisplayName("400 Bad Request로 응답한다")
        void shouldReturn400() {
            var ex = new InvalidFileException("파일이 비어있습니다");

            var response = handler.handleInvalidFile(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INVALID_FILE");
        }
    }

    @Nested
    @DisplayName("FileTooLargeException 핸들러")
    class FileTooLargeTest {

        @Test
        @DisplayName("413 Payload Too Large로 응답한다")
        void shouldReturn413() {
            var ex = new FileTooLargeException("파일 크기 초과 (최대 50MB)", 100_000_000L, 52_428_800L);

            var response = handler.handleFileTooLarge(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("FILE_TOO_LARGE");
        }
    }

    @Nested
    @DisplayName("LogParsingException 핸들러")
    class LogParsingTest {

        @Test
        @DisplayName("400 Bad Request로 응답한다")
        void shouldReturn400() {
            var ex = new LogParsingException("유효한 로그가 없습니다");

            var response = handler.handleLogParsing(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("LOG_PARSING_ERROR");
        }
    }

    @Nested
    @DisplayName("TooManyParsingErrorsException 핸들러")
    class TooManyParsingErrorsTest {

        @Test
        @DisplayName("422 Unprocessable Entity로 응답하며 통계를 포함한다")
        void shouldReturn422WithStats() {
            var ex = new TooManyParsingErrorsException("파싱 에러 과다", 100, 95);

            var response = handler.handleTooManyParsingErrors(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            var body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorCode()).isEqualTo("TOO_MANY_PARSING_ERRORS");
            assertThat(body.totalLines()).isEqualTo(100);
            assertThat(body.errorCount()).isEqualTo(95);
            assertThat(body.errorSamples()).isEmpty();
        }

        @Test
        @DisplayName("에러 샘플이 응답에 포함된다")
        void shouldIncludeErrorSamples() {
            var errors = List.of(
                    new ParseError(1, "잘못된 날짜 형식", ParseError.ErrorType.PARSING, LocalDateTime.now()),
                    new ParseError(5, "필수 필드 누락", ParseError.ErrorType.VALIDATION, LocalDateTime.now())
            );
            var ex = new TooManyParsingErrorsException("파싱 에러 과다", 10, 10, errors);

            var response = handler.handleTooManyParsingErrors(ex, request);

            var body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.errorSamples()).hasSize(2);
            assertThat(body.errorSamples().get(0).lineNumber()).isEqualTo(1);
            assertThat(body.errorSamples().get(0).errorMessage()).isEqualTo("잘못된 날짜 형식");
            assertThat(body.errorSamples().get(0).errorType()).isEqualTo("PARSING");
            assertThat(body.errorSamples().get(1).lineNumber()).isEqualTo(5);
            assertThat(body.errorSamples().get(1).errorType()).isEqualTo("VALIDATION");
        }
    }

    @Nested
    @DisplayName("AnalysisNotFoundException 핸들러")
    class AnalysisNotFoundTest {

        @Test
        @DisplayName("404 Not Found로 응답한다")
        void shouldReturn404() {
            var ex = new AnalysisNotFoundException("abc-123");

            var response = handler.handleAnalysisNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("NOT_FOUND");
            assertThat(response.getBody().message()).contains("abc-123");
        }
    }

    @Nested
    @DisplayName("DuplicateAnalysisIdException 핸들러")
    class DuplicateAnalysisIdTest {

        @Test
        @DisplayName("409 Conflict로 응답한다")
        void shouldReturn409() {
            var ex = new DuplicateAnalysisIdException("abc-123");

            var response = handler.handleDuplicateAnalysisId(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("DUPLICATE_ID");
        }
    }

    @Nested
    @DisplayName("ApiRateLimitExceededException 핸들러")
    class ApiRateLimitTest {

        @Test
        @DisplayName("429 Too Many Requests로 응답하며 Retry-After 헤더를 포함한다")
        void shouldReturn429WithRetryAfter() {
            var ex = new ApiRateLimitExceededException("요청 한도 초과");

            var response = handler.handleApiRateLimitExceeded(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("API_RATE_LIMIT_EXCEEDED");
            assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
        }
    }

    @Nested
    @DisplayName("RateLimitExceededException 핸들러 (ipinfo)")
    class RateLimitTest {

        @Test
        @DisplayName("429 Too Many Requests로 응답한다")
        void shouldReturn429() {
            var ex = new RateLimitExceededException("rate limit 초과");

            var response = handler.handleRateLimitExceeded(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
        }
    }

    @Nested
    @DisplayName("IpInfoException 핸들러")
    class IpInfoTest {

        @Test
        @DisplayName("502 Bad Gateway로 응답한다")
        void shouldReturn502() {
            var ex = new IpInfoException("IP 정보 조회 실패");

            var response = handler.handleIpInfoException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("IPINFO_ERROR");
        }
    }

    @Nested
    @DisplayName("MaxUploadSizeExceededException 핸들러")
    class MaxUploadSizeTest {

        @Test
        @DisplayName("413으로 응답하며 예외의 maxUploadSize로 메시지를 구성한다")
        void shouldReturn413WithMaxSizeFromException() {
            var ex = new MaxUploadSizeExceededException(50 * 1024 * 1024L);

            var response = handler.handleMaxUploadSizeExceeded(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("FILE_TOO_LARGE");
            assertThat(response.getBody().message()).contains("50MB");
        }

        @Test
        @DisplayName("다른 maxUploadSize도 MB 변환되어 메시지에 반영된다")
        void shouldConvertDifferentMaxSizeToMb() {
            var ex = new MaxUploadSizeExceededException(100 * 1024 * 1024L);

            var response = handler.handleMaxUploadSizeExceeded(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("100MB");
        }

        @Test
        @DisplayName("maxUploadSize가 -1이면 크기 정보 없이 메시지를 구성한다")
        void shouldHandleUnknownMaxSize() {
            var ex = new MaxUploadSizeExceededException(-1);

            var response = handler.handleMaxUploadSizeExceeded(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("파일 크기 초과");
        }
    }

    @Nested
    @DisplayName("일반 예외 핸들러")
    class GeneralExceptionTest {

        @Test
        @DisplayName("일반 Exception은 500으로 응답한다")
        void shouldReturn500ForGenericException() {
            var ex = new RuntimeException("알 수 없는 오류");

            var response = handler.handleException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
        }
    }
}
