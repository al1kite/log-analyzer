package com.electricip.loganalyzer.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpInfoErrorHandlerTest {

    private IpInfoErrorHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IpInfoErrorHandler();
    }

    @Nested
    @DisplayName("hasError 검증")
    class HasErrorTest {

        @Test
        @DisplayName("2xx 응답은 에러가 아니다")
        void shouldNotHaveErrorFor2xx() throws Exception {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.OK)) {
                assertThat(handler.hasError(response)).isFalse();
            }
        }

        @Test
        @DisplayName("4xx 응답은 에러이다")
        void shouldHaveErrorFor4xx() throws Exception {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.BAD_REQUEST)) {
                assertThat(handler.hasError(response)).isTrue();
            }
        }

        @Test
        @DisplayName("5xx 응답은 에러이다")
        void shouldHaveErrorFor5xx() throws Exception {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR)) {
                assertThat(handler.hasError(response)).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("handleError 검증")
    class HandleErrorTest {

        @Test
        @DisplayName("429는 RateLimitExceededException 발생")
        void shouldThrowRateLimitExceptionFor429() {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.TOO_MANY_REQUESTS)) {
                assertThatThrownBy(() -> handler.handleError(response))
                        .isInstanceOf(RateLimitExceededException.class)
                        .hasMessageContaining("rate limit");
            }
        }

        @Test
        @DisplayName("401은 IpInfoAuthException 발생")
        void shouldThrowAuthExceptionFor401() {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.UNAUTHORIZED)) {
                assertThatThrownBy(() -> handler.handleError(response))
                        .isInstanceOf(IpInfoAuthException.class)
                        .hasMessageContaining("인증");
            }
        }

        @Test
        @DisplayName("403은 IpInfoAuthException 발생")
        void shouldThrowAuthExceptionFor403() {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.FORBIDDEN)) {
                assertThatThrownBy(() -> handler.handleError(response))
                        .isInstanceOf(IpInfoAuthException.class)
                        .hasMessageContaining("인증");
            }
        }

        @Test
        @DisplayName("500은 IpInfoServerException 발생")
        void shouldThrowServerExceptionFor500() {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR)) {
                assertThatThrownBy(() -> handler.handleError(response))
                        .isInstanceOf(IpInfoServerException.class)
                        .hasMessageContaining("서버 오류");
            }
        }

        @Test
        @DisplayName("503은 IpInfoServerException 발생")
        void shouldThrowServerExceptionFor503() {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.SERVICE_UNAVAILABLE)) {
                assertThatThrownBy(() -> handler.handleError(response))
                        .isInstanceOf(IpInfoServerException.class)
                        .hasMessageContaining("서버 오류");
            }
        }

        @Test
        @DisplayName("404는 일반 IpInfoException 발생")
        void shouldThrowIpInfoExceptionForOtherErrors() {
            try (var response = new MockClientHttpResponse(new byte[0], HttpStatus.NOT_FOUND)) {
                assertThatThrownBy(() -> handler.handleError(response))
                        .isInstanceOf(IpInfoException.class)
                        .hasMessageContaining("404");
            }
        }
    }
}
