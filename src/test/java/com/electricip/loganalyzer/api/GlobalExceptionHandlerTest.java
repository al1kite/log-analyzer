package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.domain.InvalidCsvFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/analysis");

    @Nested
    @DisplayName("InvalidCsvFormatException 핸들러")
    class InvalidCsvFormatExceptionTest {

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
    @DisplayName("IllegalStateException 핸들러")
    class IllegalStateExceptionTest {

        @Test
        @DisplayName("422 Unprocessable Entity로 응답한다")
        void shouldReturn422() {
            var ex = new IllegalStateException("유효한 로그가 없습니다");

            var response = handler.handleIllegalState(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INVALID_STATE");
            assertThat(response.getBody().message()).isEqualTo("유효한 로그가 없습니다");
        }

        @Test
        @DisplayName("상태 코드가 422이다")
        void shouldHaveStatusCode422() {
            var ex = new IllegalStateException("test");

            var response = handler.handleIllegalState(ex, request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(422);
        }
    }

    @Nested
    @DisplayName("기존 핸들러 정상 동작 확인")
    class ExistingHandlersTest {

        @Test
        @DisplayName("IllegalArgumentException은 400으로 응답한다")
        void shouldReturn400ForIllegalArgument() {
            var ex = new IllegalArgumentException("파일이 비어있습니다");

            var response = handler.handleIllegalArgument(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("INVALID_REQUEST");
        }

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
