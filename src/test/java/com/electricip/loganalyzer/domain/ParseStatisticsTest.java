package com.electricip.loganalyzer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParseStatisticsTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 1, 0, 0);

    @Nested
    @DisplayName("ParseStatistics 생성 검증")
    class ConstructorTest {

        @Test
        @DisplayName("정상 값으로 생성된다")
        void shouldCreateWithValidValues() {
            var errors = List.of(new ParseError(1, "err", ParseError.ErrorType.PARSING, NOW));
            var stats = new ParseStatistics(10, 9, 1, Map.of("PARSING", 1), errors);

            assertThat(stats.totalLines()).isEqualTo(10);
            assertThat(stats.successCount()).isEqualTo(9);
            assertThat(stats.errorCount()).isEqualTo(1);
            assertThat(stats.errorsByType()).containsEntry("PARSING", 1);
            assertThat(stats.errorSamples()).hasSize(1);
        }

        @Test
        @DisplayName("totalLines가 음수이면 IllegalArgumentException 발생")
        void shouldThrowWhenTotalLinesIsNegative() {
            assertThatThrownBy(() -> new ParseStatistics(-1, 0, 0, Map.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("totalLines");
        }

        @Test
        @DisplayName("successCount가 음수이면 IllegalArgumentException 발생")
        void shouldThrowWhenSuccessCountIsNegative() {
            assertThatThrownBy(() -> new ParseStatistics(0, -1, 0, Map.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("successCount");
        }

        @Test
        @DisplayName("errorCount가 음수이면 IllegalArgumentException 발생")
        void shouldThrowWhenErrorCountIsNegative() {
            assertThatThrownBy(() -> new ParseStatistics(0, 0, -1, Map.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("errorCount");
        }

        @Test
        @DisplayName("errorsByType가 null이면 빈 맵이 기본값")
        void shouldDefaultErrorsByTypeToEmptyMap() {
            var stats = new ParseStatistics(0, 0, 0, null, List.of());
            assertThat(stats.errorsByType()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("errorSamples가 null이면 빈 리스트가 기본값")
        void shouldDefaultErrorSamplesToEmptyList() {
            var stats = new ParseStatistics(0, 0, 0, Map.of(), null);
            assertThat(stats.errorSamples()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("ParseStatistics 불변성 검증")
    class ImmutabilityTest {

        @Test
        @DisplayName("원본 맵 수정이 내부 상태에 영향 없다")
        void shouldDefensivelyCopyErrorsByType() {
            var mutableMap = new HashMap<String, Integer>();
            mutableMap.put("PARSING", 1);

            var stats = new ParseStatistics(1, 0, 1, mutableMap, List.of());
            mutableMap.clear();

            assertThat(stats.errorsByType()).hasSize(1);
            assertThat(stats.errorsByType().get("PARSING")).isEqualTo(1);
        }

        @Test
        @DisplayName("원본 리스트 수정이 내부 상태에 영향 없다")
        void shouldDefensivelyCopyErrorSamples() {
            var mutableList = new ArrayList<>(List.of(
                    new ParseError(1, "err", ParseError.ErrorType.PARSING, NOW)));

            var stats = new ParseStatistics(1, 0, 1, Map.of(), mutableList);
            mutableList.clear();

            assertThat(stats.errorSamples()).hasSize(1);
        }

        @Test
        @DisplayName("errorsByType 반환 맵은 수정할 수 없다")
        void shouldReturnUnmodifiableErrorsByType() {
            var stats = new ParseStatistics(1, 0, 1, Map.of("PARSING", 1), List.of());

            assertThatThrownBy(() -> stats.errorsByType().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("errorSamples 반환 리스트는 수정할 수 없다")
        void shouldReturnUnmodifiableErrorSamples() {
            var error = new ParseError(1, "err", ParseError.ErrorType.PARSING, NOW);
            var stats = new ParseStatistics(1, 0, 1, Map.of(), List.of(error));

            assertThatThrownBy(() -> stats.errorSamples().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("empty() 팩터리 메서드 검증")
    class EmptyFactoryTest {

        @Test
        @DisplayName("empty()는 모든 값이 0/빈 상태이다")
        void shouldCreateEmptyParseStatistics() {
            var stats = ParseStatistics.empty();

            assertThat(stats.totalLines()).isZero();
            assertThat(stats.successCount()).isZero();
            assertThat(stats.errorCount()).isZero();
            assertThat(stats.errorsByType()).isEmpty();
            assertThat(stats.errorSamples()).isEmpty();
        }

        @Test
        @DisplayName("empty()의 컬렉션도 수정할 수 없다")
        void shouldReturnUnmodifiableCollectionsFromEmpty() {
            var stats = ParseStatistics.empty();

            assertThatThrownBy(() -> stats.errorSamples().add(
                    new ParseError(1, "err", ParseError.ErrorType.PARSING, NOW)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    @DisplayName("MAX_ERROR_SAMPLES 상수는 10이다")
    void shouldHaveMaxErrorSamplesOf10() {
        assertThat(ParseStatistics.MAX_ERROR_SAMPLES).isEqualTo(10);
    }
}
