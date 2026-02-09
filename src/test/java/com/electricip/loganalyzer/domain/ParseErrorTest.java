package com.electricip.loganalyzer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParseErrorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 1, 1, 0, 0);

    @Nested
    @DisplayName("ParseError 생성 검증")
    class ConstructorTest {

        @Test
        @DisplayName("정상 값으로 생성된다")
        void shouldCreateWithValidValues() {
            var error = new ParseError(1, "test error", ParseError.ErrorType.PARSING, NOW);

            assertThat(error.lineNumber()).isEqualTo(1);
            assertThat(error.errorMessage()).isEqualTo("test error");
            assertThat(error.errorType()).isEqualTo(ParseError.ErrorType.PARSING);
            assertThat(error.occurredAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("lineNumber가 0이면 IllegalArgumentException 발생")
        void shouldThrowWhenLineNumberIsZero() {
            assertThatThrownBy(() -> new ParseError(0, "msg", ParseError.ErrorType.PARSING, NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lineNumber");
        }

        @Test
        @DisplayName("lineNumber가 음수이면 IllegalArgumentException 발생")
        void shouldThrowWhenLineNumberIsNegative() {
            assertThatThrownBy(() -> new ParseError(-1, "msg", ParseError.ErrorType.PARSING, NOW))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lineNumber");
        }

        @Test
        @DisplayName("errorMessage가 null이면 NullPointerException 발생")
        void shouldThrowWhenErrorMessageIsNull() {
            assertThatThrownBy(() -> new ParseError(1, null, ParseError.ErrorType.PARSING, NOW))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("errorMessage");
        }

        @Test
        @DisplayName("errorType이 null이면 NullPointerException 발생")
        void shouldThrowWhenErrorTypeIsNull() {
            assertThatThrownBy(() -> new ParseError(1, "msg", null, NOW))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("errorType");
        }

        @Test
        @DisplayName("occurredAt이 null이면 NullPointerException 발생")
        void shouldThrowWhenOccurredAtIsNull() {
            assertThatThrownBy(() -> new ParseError(1, "msg", ParseError.ErrorType.PARSING, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("occurredAt");
        }
    }

    @Nested
    @DisplayName("ErrorType 검증")
    class ErrorTypeTest {

        @Test
        @DisplayName("모든 ErrorType 값이 존재한다")
        void shouldHaveAllErrorTypes() {
            assertThat(ParseError.ErrorType.values())
                    .containsExactlyInAnyOrder(
                            ParseError.ErrorType.PARSING,
                            ParseError.ErrorType.VALIDATION,
                            ParseError.ErrorType.FORMAT
                    );
        }
    }
}
