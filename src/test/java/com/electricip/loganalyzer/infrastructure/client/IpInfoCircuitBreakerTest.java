package com.electricip.loganalyzer.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
}
