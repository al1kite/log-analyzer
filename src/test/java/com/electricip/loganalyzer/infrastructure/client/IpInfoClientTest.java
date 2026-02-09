package com.electricip.loganalyzer.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpInfoClientTest {

    @Mock
    private RestTemplate restTemplate;

    private IpInfoCircuitBreaker circuitBreaker;
    private IpInfoClient client;

    @BeforeEach
    void setUp() {
        circuitBreaker = new IpInfoCircuitBreaker();
        client = new IpInfoClient(restTemplate, circuitBreaker);
        ReflectionTestUtils.setField(client, "baseUrl", "https://ipinfo.io");
        ReflectionTestUtils.setField(client, "token", null);
    }

    @Nested
    @DisplayName("정상 호출")
    class SuccessTest {

        @Test
        @DisplayName("null IP는 NullPointerException 발생")
        void shouldThrowForNullIp() {
            assertThatThrownBy(() -> client.getIpInfo(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("빈 IP는 IllegalArgumentException 발생")
        void shouldThrowForBlankIp() {
            assertThatThrownBy(() -> client.getIpInfo("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker 통합")
    class CircuitBreakerTest {

        @Test
        @DisplayName("Circuit Open 시 API 호출 없이 fallback 반환")
        void shouldReturnFallbackWhenCircuitOpen() {
            // Circuit을 열기
            for (int i = 0; i < IpInfoCircuitBreaker.FAILURE_THRESHOLD; i++) {
                circuitBreaker.recordFailure();
            }

            var result = client.getIpInfo("1.2.3.4");

            assertThat(result).isNotNull();
            assertThat(result.isValid()).isFalse();
            // RestTemplate은 호출되지 않아야 함
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("API 실패 시 Circuit Breaker에 실패가 기록된다")
        void shouldRecordFailureOnApiError() {
            when(restTemplate.getForObject(anyString(), any(Class.class)))
                    .thenThrow(new IpInfoServerException("서버 오류"));

            client.getIpInfo("1.2.3.4");

            // MAX_ATTEMPTS(3)만큼 실패 기록
            assertThat(circuitBreaker.getFailureCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Retry 동작")
    class RetryTest {

        @Test
        @DisplayName("RateLimitExceededException은 재시도하지 않는다")
        void shouldNotRetryOnRateLimit() {
            when(restTemplate.getForObject(anyString(), any(Class.class)))
                    .thenThrow(new RateLimitExceededException("rate limit"));

            var result = client.getIpInfo("1.2.3.4");

            assertThat(result.isValid()).isFalse();
            // 1번만 호출 (재시도 없음)
            verify(restTemplate, times(1)).getForObject(anyString(), any(Class.class));
        }

        @Test
        @DisplayName("IpInfoAuthException은 재시도하지 않는다")
        void shouldNotRetryOnAuthError() {
            when(restTemplate.getForObject(anyString(), any(Class.class)))
                    .thenThrow(new IpInfoAuthException("auth failed"));

            var result = client.getIpInfo("1.2.3.4");

            assertThat(result.isValid()).isFalse();
            // 1번만 호출 (재시도 없음)
            verify(restTemplate, times(1)).getForObject(anyString(), any(Class.class));
        }

        @Test
        @DisplayName("일반 예외는 MAX_ATTEMPTS까지 재시도한다")
        void shouldRetryOnGeneralException() {
            when(restTemplate.getForObject(anyString(), any(Class.class)))
                    .thenThrow(new IpInfoServerException("서버 오류"));

            var result = client.getIpInfo("1.2.3.4");

            assertThat(result.isValid()).isFalse();
            // 3번 호출 (최초 + 2회 재시도)
            verify(restTemplate, times(3)).getForObject(anyString(), any(Class.class));
        }
    }

    @Nested
    @DisplayName("에러 타입 분류")
    class ErrorClassificationTest {

        @Test
        @DisplayName("RateLimitExceededException은 IpInfoException의 하위 타입이다")
        void rateLimitShouldExtendIpInfoException() {
            var ex = new RateLimitExceededException("test");
            assertThat(ex).isInstanceOf(IpInfoException.class);
        }

        @Test
        @DisplayName("IpInfoAuthException은 IpInfoException의 하위 타입이다")
        void authShouldExtendIpInfoException() {
            var ex = new IpInfoAuthException("test");
            assertThat(ex).isInstanceOf(IpInfoException.class);
        }

        @Test
        @DisplayName("IpInfoServerException은 IpInfoException의 하위 타입이다")
        void serverShouldExtendIpInfoException() {
            var ex = new IpInfoServerException("test");
            assertThat(ex).isInstanceOf(IpInfoException.class);
        }
    }
}
