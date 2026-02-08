package com.electricip.loganalyzer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccessLogTest {

    private AccessLog createLog(Integer httpStatus) {
        return new AccessLog(
                LocalDateTime.of(2025, 1, 1, 12, 0),
                "1.2.3.4",
                "GET",
                "/api/test",
                "Mozilla/5.0",
                httpStatus,
                "1.1",
                100L,
                200L,
                0.1,
                "TLSv1.3",
                "/api/test?q=1"
        );
    }

    @Nested
    @DisplayName("isSuccessful() 검증")
    class IsSuccessfulTest {

        @ParameterizedTest(name = "상태 코드 {0}은 성공이다")
        @ValueSource(ints = {200, 201, 204, 250, 299})
        void shouldReturnTrue_whenStatusIs2xx(int status) {
            var log = createLog(status);
            assertThat(log.isSuccessful()).isTrue();
        }

        @ParameterizedTest(name = "상태 코드 {0}은 성공이 아니다")
        @ValueSource(ints = {199, 300, 301, 400, 404, 500, 503})
        void shouldReturnFalse_whenStatusIsNot2xx(int status) {
            var log = createLog(status);
            assertThat(log.isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("경계값: 200은 성공이다")
        void shouldReturnTrue_whenStatusIs200() {
            assertThat(createLog(200).isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("경계값: 299는 성공이다")
        void shouldReturnTrue_whenStatusIs299() {
            assertThat(createLog(299).isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("경계값: 199는 성공이 아니다")
        void shouldReturnFalse_whenStatusIs199() {
            assertThat(createLog(199).isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("경계값: 300은 성공이 아니다")
        void shouldReturnFalse_whenStatusIs300() {
            assertThat(createLog(300).isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("httpStatus가 null이면 성공이 아니다")
        void shouldReturnFalse_whenStatusIsNull() {
            assertThat(createLog(null).isSuccessful()).isFalse();
        }
    }

    @Nested
    @DisplayName("statusCategory() 검증")
    class StatusCategoryTest {

        @ParameterizedTest(name = "상태 코드 {0}은 2xx 카테고리이다")
        @ValueSource(ints = {200, 201, 250, 299})
        void shouldReturn2xx_whenStatusIs200to299(int status) {
            assertThat(createLog(status).statusCategory()).isEqualTo("2xx");
        }

        @ParameterizedTest(name = "상태 코드 {0}은 3xx 카테고리이다")
        @ValueSource(ints = {300, 301, 302, 399})
        void shouldReturn3xx_whenStatusIs300to399(int status) {
            assertThat(createLog(status).statusCategory()).isEqualTo("3xx");
        }

        @ParameterizedTest(name = "상태 코드 {0}은 4xx 카테고리이다")
        @ValueSource(ints = {400, 401, 403, 404, 499})
        void shouldReturn4xx_whenStatusIs400to499(int status) {
            assertThat(createLog(status).statusCategory()).isEqualTo("4xx");
        }

        @ParameterizedTest(name = "상태 코드 {0}은 5xx 카테고리이다")
        @ValueSource(ints = {500, 502, 503, 599})
        void shouldReturn5xx_whenStatusIs500to599(int status) {
            assertThat(createLog(status).statusCategory()).isEqualTo("5xx");
        }

        @Test
        @DisplayName("httpStatus가 null이면 Unknown")
        void shouldReturnUnknown_whenStatusIsNull() {
            assertThat(createLog(null).statusCategory()).isEqualTo("Unknown");
        }

        @ParameterizedTest(name = "상태 코드 {0}은 Unknown 카테고리이다")
        @ValueSource(ints = {0, 100, 199, 600, 999})
        void shouldReturnUnknown_whenStatusIsOutOfRange(int status) {
            assertThat(createLog(status).statusCategory()).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("null 필드 처리 검증")
    class NullFieldTest {

        @Test
        @DisplayName("모든 필드가 null이어도 생성 가능하다")
        void shouldAllowAllNullFields() {
            var log = new AccessLog(null, null, null, null, null, null, null, null, null, null, null, null);

            assertThat(log.timeGenerated()).isNull();
            assertThat(log.clientIp()).isNull();
            assertThat(log.httpMethod()).isNull();
            assertThat(log.requestUri()).isNull();
            assertThat(log.userAgent()).isNull();
            assertThat(log.httpStatus()).isNull();
            assertThat(log.httpVersion()).isNull();
            assertThat(log.receivedBytes()).isNull();
            assertThat(log.sentBytes()).isNull();
            assertThat(log.clientResponseTime()).isNull();
            assertThat(log.sslProtocol()).isNull();
            assertThat(log.originalRequestUriWithArgs()).isNull();
        }

        @Test
        @DisplayName("일부 필드만 null이어도 생성 가능하다")
        void shouldAllowPartialNullFields() {
            var log = new AccessLog(
                    LocalDateTime.of(2025, 1, 1, 12, 0),
                    "1.2.3.4",
                    "GET",
                    "/",
                    null,
                    200,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertThat(log.clientIp()).isEqualTo("1.2.3.4");
            assertThat(log.httpStatus()).isEqualTo(200);
            assertThat(log.userAgent()).isNull();
            assertThat(log.sslProtocol()).isNull();
        }

        @Test
        @DisplayName("모든 필드가 null인 로그의 isSuccessful은 false")
        void shouldReturnFalse_whenAllFieldsNull() {
            var log = new AccessLog(null, null, null, null, null, null, null, null, null, null, null, null);
            assertThat(log.isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("모든 필드가 null인 로그의 statusCategory는 Unknown")
        void shouldReturnUnknown_whenAllFieldsNull() {
            var log = new AccessLog(null, null, null, null, null, null, null, null, null, null, null, null);
            assertThat(log.statusCategory()).isEqualTo("Unknown");
        }
    }
}
