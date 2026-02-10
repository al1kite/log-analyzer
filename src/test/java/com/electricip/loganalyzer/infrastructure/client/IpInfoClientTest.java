package com.electricip.loganalyzer.infrastructure.client;

import com.electricip.loganalyzer.config.IpInfoProperties;
import com.electricip.loganalyzer.domain.IpInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
        client = new IpInfoClient(restTemplate, circuitBreaker, new IpInfoProperties("https://ipinfo.io", null));
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
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new IpInfoServerException("서버 오류"));

            client.getIpInfo("1.2.3.4");

            // 최종 실패 시 1회만 기록
            assertThat(circuitBreaker.getFailureCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Retry 동작")
    class RetryTest {

        @Test
        @DisplayName("RateLimitExceededException은 재시도하지 않는다")
        void shouldNotRetryOnRateLimit() {
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new RateLimitExceededException("rate limit"));

            var result = client.getIpInfo("1.2.3.4");

            assertThat(result.isValid()).isFalse();
            // 1번만 호출 (재시도 없음)
            verify(restTemplate, times(1)).getForObject(anyString(), any());
        }

        @Test
        @DisplayName("IpInfoAuthException은 재시도하지 않는다")
        void shouldNotRetryOnAuthError() {
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new IpInfoAuthException("auth failed"));

            var result = client.getIpInfo("1.2.3.4");

            assertThat(result.isValid()).isFalse();
            // 1번만 호출 (재시도 없음)
            verify(restTemplate, times(1)).getForObject(anyString(), any());
        }

        @Test
        @DisplayName("일반 예외는 MAX_ATTEMPTS까지 재시도한다")
        void shouldRetryOnGeneralException() {
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new IpInfoServerException("서버 오류"));

            var result = client.getIpInfo("1.2.3.4");

            assertThat(result.isValid()).isFalse();
            // 3번 호출 (최초 + 2회 재시도)
            verify(restTemplate, times(3)).getForObject(anyString(), any());
        }

        @Test
        @DisplayName("null 응답은 MAX_ATTEMPTS까지 재시도하고 실패를 기록한다")
        void shouldRetryAndRecordFailureOnNullResponse() {
            when(restTemplate.getForObject(anyString(), any()))
                    .thenReturn(null);

            var result = client.getIpInfo("1.2.3.4");

            assertThat(result.isValid()).isFalse();
            // 3번 호출 (최초 + 2회 재시도)
            verify(restTemplate, times(3)).getForObject(anyString(), any());
            // 최종 실패 시 1회 기록
            assertThat(circuitBreaker.getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("null 응답 반복 시 Circuit Breaker가 열린다")
        void shouldOpenCircuitOnRepeatedNullResponses() {
            when(restTemplate.getForObject(anyString(), any()))
                    .thenReturn(null);

            // FAILURE_THRESHOLD(5)회 호출 → 각각 1회 failure 기록
            for (int i = 0; i < IpInfoCircuitBreaker.FAILURE_THRESHOLD; i++) {
                client.getIpInfo("10.0.0." + i);
            }

            assertThat(circuitBreaker.isOpen()).isTrue();

            // 이후 호출은 API 없이 fallback 반환
            reset(restTemplate);
            var result = client.getIpInfo("10.0.0.99");
            assertThat(result.isValid()).isFalse();
            verifyNoInteractions(restTemplate);
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

    @Nested
    @DisplayName("동시성 검증")
    class ConcurrencyTest {

        @Test
        @DisplayName("여러 스레드에서 Circuit Open 상태로 동시 호출 시 API 호출 없음")
        void concurrentCallsWithCircuitOpen_noApiCalls() throws Exception {
            for (int i = 0; i < IpInfoCircuitBreaker.FAILURE_THRESHOLD; i++) {
                circuitBreaker.recordFailure();
            }

            int threadCount = 20;
            var latch = new CountDownLatch(threadCount);
            var executor = Executors.newFixedThreadPool(10);
            var tasks = new ArrayList<Future<IpInfo>>();

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                tasks.add(executor.submit(() -> {
                    try {
                        return client.getIpInfo("10.0.0." + idx);
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            for (var task : tasks) {
                assertThat(task.get().isValid()).isFalse();
            }
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("여러 스레드에서 동시 실패 시 Circuit Breaker 카운트가 정확하다")
        void concurrentFailures_circuitBreakerCountAccurate() throws InterruptedException {
            when(restTemplate.getForObject(anyString(), any()))
                    .thenThrow(new RateLimitExceededException("rate limit"));

            int threadCount = 10;
            var latch = new CountDownLatch(threadCount);
            var executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        client.getIpInfo("10.0.0." + idx);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            // 각 호출이 1번 실패 기록 → 총 10
            // 단, 일부가 circuit open 후 호출되면 더 적을 수 있음
            assertThat(circuitBreaker.getFailureCount()).isGreaterThanOrEqualTo(
                    IpInfoCircuitBreaker.FAILURE_THRESHOLD);
        }
    }
}
