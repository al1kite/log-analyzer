package com.electricip.loganalyzer.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class IpInfoCircuitBreakerTest {

    private IpInfoCircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = new IpInfoCircuitBreaker();
    }

    @Nested
    @DisplayName("Closed 상태 (정상)")
    class ClosedStateTest {

        @Test
        @DisplayName("초기 상태에서 circuit은 닫혀있다")
        void shouldBeClosedInitially() {
            assertThat(circuitBreaker.isOpen()).isFalse();
        }

        @Test
        @DisplayName("임계치 미만 실패 시 circuit은 닫혀있다")
        void shouldRemainClosedBelowThreshold() {
            for (int i = 0; i < IpInfoCircuitBreaker.FAILURE_THRESHOLD - 1; i++) {
                circuitBreaker.recordFailure();
            }

            assertThat(circuitBreaker.isOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("Open 상태 (차단)")
    class OpenStateTest {

        @Test
        @DisplayName("임계치 도달 시 circuit이 열린다")
        void shouldOpenAtThreshold() {
            for (int i = 0; i < IpInfoCircuitBreaker.FAILURE_THRESHOLD; i++) {
                circuitBreaker.recordFailure();
            }

            assertThat(circuitBreaker.isOpen()).isTrue();
        }

        @Test
        @DisplayName("임계치 초과 실패 시에도 circuit은 열려있다")
        void shouldRemainOpenAboveThreshold() {
            for (int i = 0; i < IpInfoCircuitBreaker.FAILURE_THRESHOLD + 5; i++) {
                circuitBreaker.recordFailure();
            }

            assertThat(circuitBreaker.isOpen()).isTrue();
        }
    }

    @Nested
    @DisplayName("복구 동작")
    class RecoveryTest {

        @Test
        @DisplayName("성공 기록 시 카운터가 리셋된다")
        void shouldResetOnSuccess() {
            for (int i = 0; i < IpInfoCircuitBreaker.FAILURE_THRESHOLD; i++) {
                circuitBreaker.recordFailure();
            }
            assertThat(circuitBreaker.isOpen()).isTrue();

            circuitBreaker.recordSuccess();

            assertThat(circuitBreaker.isOpen()).isFalse();
            assertThat(circuitBreaker.getFailureCount()).isZero();
        }

        @Test
        @DisplayName("실패 후 성공하면 카운터가 리셋된다")
        void shouldResetCounterAfterSuccess() {
            circuitBreaker.recordFailure();
            circuitBreaker.recordFailure();
            circuitBreaker.recordSuccess();

            assertThat(circuitBreaker.getFailureCount()).isZero();
        }
    }

    @Nested
    @DisplayName("상수값 검증")
    class ConstantsTest {

        @Test
        @DisplayName("FAILURE_THRESHOLD는 5이다")
        void shouldHaveFailureThresholdOf5() {
            assertThat(IpInfoCircuitBreaker.FAILURE_THRESHOLD).isEqualTo(5);
        }

        @Test
        @DisplayName("TIMEOUT_MS는 60초이다")
        void shouldHaveTimeoutOf60Seconds() {
            assertThat(IpInfoCircuitBreaker.TIMEOUT_MS).isEqualTo(60_000);
        }
    }

    @Nested
    @DisplayName("동시성 검증")
    class ConcurrencyTest {

        @Test
        @DisplayName("여러 스레드에서 동시에 recordFailure 해도 카운트 손실 없음")
        void concurrentRecordFailure_noLostUpdates() throws InterruptedException {
            int threadCount = 50;
            var latch = new CountDownLatch(threadCount);
            var executor = Executors.newFixedThreadPool(10);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        circuitBreaker.recordFailure();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            assertThat(circuitBreaker.getFailureCount()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("recordFailure와 recordSuccess 동시 수행 시 예외 없음")
        void concurrentFailureAndSuccess_noException() throws InterruptedException {
            int totalOps = 100;
            var latch = new CountDownLatch(totalOps);
            var executor = Executors.newFixedThreadPool(10);

            for (int i = 0; i < totalOps; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        if (idx % 3 == 0) {
                            circuitBreaker.recordSuccess();
                        } else {
                            circuitBreaker.recordFailure();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            // 예외 없이 완료 + 카운트가 비음수
            assertThat(circuitBreaker.getFailureCount()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("여러 스레드에서 isOpen 호출 시 데드락 없음")
        void concurrentIsOpen_noDeadlock() throws InterruptedException {
            // 임계치 도달
            for (int i = 0; i < IpInfoCircuitBreaker.FAILURE_THRESHOLD; i++) {
                circuitBreaker.recordFailure();
            }

            int threadCount = 50;
            var latch = new CountDownLatch(threadCount);
            var executor = Executors.newFixedThreadPool(10);
            var openCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        if (circuitBreaker.isOpen()) {
                            openCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            // 모든 스레드가 open 상태를 확인해야 함
            assertThat(openCount.get()).isEqualTo(threadCount);
        }
    }
}
