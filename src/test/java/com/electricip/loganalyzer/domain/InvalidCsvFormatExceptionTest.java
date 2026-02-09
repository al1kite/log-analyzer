package com.electricip.loganalyzer.domain;

import com.electricip.loganalyzer.domain.exception.LogAnalyzerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvalidCsvFormatExceptionTest {

    @Nested
    @DisplayName("생성자 검증")
    class ConstructorTest {

        @Test
        @DisplayName("메시지와 누락 헤더 리스트로 생성된다")
        void shouldCreateWithMessageAndMissingHeaders() {
            var headers = List.of("ClientIp", "HttpMethod");
            var ex = new InvalidCsvFormatException("헤더 누락", headers);

            assertThat(ex.getMessage()).isEqualTo("헤더 누락");
            assertThat(ex.getMissingHeaders()).containsExactly("ClientIp", "HttpMethod");
        }

        @Test
        @DisplayName("메시지만으로 생성 시 빈 missingHeaders")
        void shouldCreateWithMessageOnly() {
            var ex = new InvalidCsvFormatException("파싱 실패");

            assertThat(ex.getMessage()).isEqualTo("파싱 실패");
            assertThat(ex.getMissingHeaders()).isEmpty();
        }

        @Test
        @DisplayName("null missingHeaders는 빈 리스트로 처리된다")
        void shouldHandleNullMissingHeaders() {
            var ex = new InvalidCsvFormatException("message", null);

            assertThat(ex.getMissingHeaders()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("불변성 검증")
    class ImmutabilityTest {

        @Test
        @DisplayName("원본 리스트 수정이 missingHeaders에 영향 없다")
        void shouldDefensivelyCopyMissingHeaders() {
            var mutableList = new ArrayList<>(List.of("ClientIp"));
            var ex = new InvalidCsvFormatException("msg", mutableList);

            mutableList.clear();

            assertThat(ex.getMissingHeaders()).hasSize(1);
            assertThat(ex.getMissingHeaders().get(0)).isEqualTo("ClientIp");
        }

        @Test
        @DisplayName("반환된 missingHeaders 리스트는 수정할 수 없다")
        void shouldReturnUnmodifiableMissingHeaders() {
            var ex = new InvalidCsvFormatException("msg", List.of("ClientIp"));

            assertThatThrownBy(() -> ex.getMissingHeaders().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    @DisplayName("LogAnalyzerException을 상속한다")
    void shouldExtendLogAnalyzerException() {
        var ex = new InvalidCsvFormatException("test");
        assertThat(ex).isInstanceOf(LogAnalyzerException.class);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("errorCode가 INVALID_CSV_FORMAT이다")
    void shouldHaveCorrectErrorCode() {
        var ex = new InvalidCsvFormatException("test");
        assertThat(ex.getErrorCode()).isEqualTo("INVALID_CSV_FORMAT");
    }
}
