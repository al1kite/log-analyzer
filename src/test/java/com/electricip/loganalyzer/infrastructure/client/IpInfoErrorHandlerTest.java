package com.electricip.loganalyzer.infrastructure.client;

import com.electricip.loganalyzer.domain.exception.LogAnalyzerException;
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

    @Nested
    @DisplayName("LogAnalyzerException 상속 검증")
    class InheritanceTest {

        @Test
        @DisplayName("IpInfoException은 LogAnalyzerException을 상속한다")
        void ipInfoExceptionShouldExtendLogAnalyzerException() {
            var ex = new IpInfoException("test");
            assertThat(ex).isInstanceOf(LogAnalyzerException.class);
            assertThat(ex.getErrorCode()).isEqualTo("IPINFO_ERROR");
        }

        @Test
        @DisplayName("RateLimitExceededException은 LogAnalyzerException을 상속한다")
        void rateLimitShouldExtendLogAnalyzerException() {
            var ex = new RateLimitExceededException("test");
            assertThat(ex).isInstanceOf(LogAnalyzerException.class);
            assertThat(ex.getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
        }

        @Test
        @DisplayName("IpInfoAuthException은 LogAnalyzerException을 상속한다")
        void authShouldExtendLogAnalyzerException() {
            var ex = new IpInfoAuthException("test");
            assertThat(ex).isInstanceOf(LogAnalyzerException.class);
            assertThat(ex.getErrorCode()).isEqualTo("IPINFO_AUTH_ERROR");
        }

        @Test
        @DisplayName("IpInfoServerException은 LogAnalyzerException을 상속한다")
        void serverShouldExtendLogAnalyzerException() {
            var ex = new IpInfoServerException("test");
            assertThat(ex).isInstanceOf(LogAnalyzerException.class);
            assertThat(ex.getErrorCode()).isEqualTo("IPINFO_SERVER_ERROR");
        }
    }
}
