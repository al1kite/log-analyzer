package com.electricip.loganalyzer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Nested
    @DisplayName("Compact Constructor 검증")
    class CompactConstructorTest {

        @Test
        @DisplayName("모든 필드가 정상이면 그대로 생성된다")
        void shouldCreateWithValidFields() {
            var response = new ErrorResponse(
                    LocalDateTime.of(2025, 1, 1, 0, 0),
                    400, "INVALID_REQUEST", "잘못된 요청", "/api/test");

            assertEquals(400, response.status());
            assertEquals("INVALID_REQUEST", response.errorCode());
            assertEquals("잘못된 요청", response.message());
            assertEquals("/api/test", response.path());
        }

        @Test
        @DisplayName("message가 null이면 '알 수 없는 오류'로 치환된다")
        void shouldReplaceNullMessageWithDefault() {
            var response = new ErrorResponse(
                    LocalDateTime.now(), 400, "CODE", null, "/api/test");

            assertEquals("알 수 없는 오류", response.message());
        }

        @Test
        @DisplayName("timestamp가 null이면 NullPointerException 발생")
        void shouldThrowWhenTimestampIsNull() {
            assertThrows(NullPointerException.class, () ->
                    new ErrorResponse(
                            null, 400, "CODE", "msg", "/api/test"));
        }

        @Test
        @DisplayName("errorCode가 null이면 NullPointerException 발생")
        void shouldThrowWhenErrorCodeIsNull() {
            assertThrows(NullPointerException.class, () ->
                    new ErrorResponse(
                            LocalDateTime.now(), 400, null, "msg", "/api/test"));
        }

        @Test
        @DisplayName("path가 null이면 NullPointerException 발생")
        void shouldThrowWhenPathIsNull() {
            assertThrows(NullPointerException.class, () ->
                    new ErrorResponse(
                            LocalDateTime.now(), 400, "CODE", "msg", null));
        }
    }

    @Nested
    @DisplayName("of() 팩터리 메서드 검증")
    class OfFactoryMethodTest {

        @Test
        @DisplayName("정상 생성")
        void shouldCreateViaOf() {
            var response = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청", "/api/test");

            assertEquals(400, response.status());
            assertEquals("INVALID_REQUEST", response.errorCode());
            assertEquals("잘못된 요청", response.message());
        }

        @Test
        @DisplayName("of()에서 message가 null이면 기본 메시지로 치환된다")
        void shouldReplaceNullMessageInOf() {
            var response = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST, "CODE", null, "/api/test");

            assertEquals("알 수 없는 오류", response.message());
        }
    }
}
