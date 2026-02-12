package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.config.RateLimitProperties;
import com.electricip.loganalyzer.domain.exception.ApiRateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitServiceTest {

    @Nested
    @DisplayName("checkRateLimit")
    class CheckRateLimit {

        @Test
        @DisplayName("clientIp가 null이면 NullPointerException이 발생한다")
        void shouldRejectNullClientIp() {
            var service = createService(5, true);

            assertThatThrownBy(() -> service.checkRateLimit(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("clientIp");
        }

        @Test
        @DisplayName("한도 내 요청은 정상 통과한다")
        void shouldPassWithinLimit() {
            var service = createService(5, true);

            for (int i = 0; i < 5; i++) {
                service.checkRateLimit("192.168.1.1");
            }
        }

        @Test
        @DisplayName("한도 초과 시 ApiRateLimitExceededException이 발생한다")
        void shouldThrowWhenLimitExceeded() {
            var service = createService(3, true);

            for (int i = 0; i < 3; i++) {
                service.checkRateLimit("192.168.1.1");
            }

            assertThatThrownBy(() -> service.checkRateLimit("192.168.1.1"))
                    .isInstanceOf(ApiRateLimitExceededException.class)
                    .hasMessageContaining("한도를 초과")
                    .satisfies(ex -> assertThat(
                            ((ApiRateLimitExceededException) ex).getRetryAfterSeconds())
                            .isEqualTo(60));
        }

        @Test
        @DisplayName("enabled=false이면 항상 통과한다")
        void shouldAlwaysPassWhenDisabled() {
            var service = createService(1, false);

            for (int i = 0; i < 100; i++) {
                service.checkRateLimit("192.168.1.1");
            }
        }

        @Test
        @DisplayName("IP별로 독립적으로 카운트된다")
        void shouldCountIndependentlyPerIp() {
            var service = createService(2, true);

            service.checkRateLimit("192.168.1.1");
            service.checkRateLimit("192.168.1.1");

            // 다른 IP는 별도 카운트
            service.checkRateLimit("192.168.1.2");
            service.checkRateLimit("192.168.1.2");

            // 첫 번째 IP는 초과
            assertThatThrownBy(() -> service.checkRateLimit("192.168.1.1"))
                    .isInstanceOf(ApiRateLimitExceededException.class);

            // 두 번째 IP도 초과
            assertThatThrownBy(() -> service.checkRateLimit("192.168.1.2"))
                    .isInstanceOf(ApiRateLimitExceededException.class);
        }
    }

    @Nested
    @DisplayName("getRemainingRequests")
    class GetRemainingRequests {

        @Test
        @DisplayName("요청 전에는 최대 한도를 반환한다")
        void shouldReturnMaxBeforeAnyRequest() {
            var service = createService(10, true);

            assertThat(service.getRemainingRequests("192.168.1.1")).isEqualTo(10);
        }

        @Test
        @DisplayName("요청 후 정확한 잔여 수를 반환한다")
        void shouldReturnCorrectRemainingAfterRequests() {
            var service = createService(5, true);

            service.checkRateLimit("192.168.1.1");
            service.checkRateLimit("192.168.1.1");

            assertThat(service.getRemainingRequests("192.168.1.1")).isEqualTo(3);
        }

        @Test
        @DisplayName("한도 초과 후 0을 반환한다")
        void shouldReturnZeroWhenExceeded() {
            var service = createService(2, true);

            service.checkRateLimit("192.168.1.1");
            service.checkRateLimit("192.168.1.1");

            assertThat(service.getRemainingRequests("192.168.1.1")).isEqualTo(0);
        }

        @Test
        @DisplayName("enabled=false이면 최대 한도를 반환한다")
        void shouldReturnMaxWhenDisabled() {
            var service = createService(10, false);

            service.checkRateLimit("192.168.1.1");

            assertThat(service.getRemainingRequests("192.168.1.1")).isEqualTo(10);
        }
    }

    private RateLimitService createService(int maxRequests, boolean enabled) {
        return new RateLimitService(new RateLimitProperties(maxRequests, enabled, 60));
    }
}
