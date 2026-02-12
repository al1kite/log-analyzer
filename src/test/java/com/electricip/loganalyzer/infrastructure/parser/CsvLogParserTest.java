package com.electricip.loganalyzer.infrastructure.parser;

import com.electricip.loganalyzer.application.LogParser;
import com.electricip.loganalyzer.config.LogAnalysisProperties;
import com.electricip.loganalyzer.domain.exception.InvalidCsvFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvLogParserTest {

    private CsvLogParser parser;

    private static final String VALID_HEADERS = String.join(",",
            "TimeGenerated [UTC]", "ClientIp", "HttpMethod", "RequestUri",
            "UserAgent", "HttpStatus", "HttpVersion", "ReceivedBytes",
            "SentBytes", "ClientResponseTime", "SslProtocol",
            "OriginalRequestUriWithArgs");

    private static final String VALID_ROW =
            "\"1/1/2025, 12:00:00.000 AM\",1.2.3.4,GET,/api/test,Mozilla/5.0,200,HTTP/1.1,100,200,0.5,TLSv1.2,/api/test?q=1";

    @BeforeEach
    void setUp() {
        parser = new CsvLogParser(new LogAnalysisProperties(200_000, 10, 50, 5, null));
    }

    private ByteArrayInputStream toStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("헤더 검증")
    class HeaderValidationTest {

        @Test
        @DisplayName("유효한 헤더가 있으면 정상 파싱된다")
        void shouldParseWithValidHeaders() {
            var csv = VALID_HEADERS + "\n" + VALID_ROW;
            var result = parser.parse(toStream(csv));

            assertThat(result.logs()).hasSize(1);
            assertThat(result.parseStatistics().totalLines()).isEqualTo(1);
            assertThat(result.parseStatistics().successCount()).isEqualTo(1);
            assertThat(result.parseStatistics().errorCount()).isZero();
        }

        @Test
        @DisplayName("필수 헤더가 누락되면 InvalidCsvFormatException 발생")
        void shouldThrowWhenRequiredHeadersMissing() {
            var csv = "WrongHeader1,WrongHeader2\nval1,val2";

            assertThatThrownBy(() -> parser.parse(toStream(csv)))
                    .isInstanceOf(InvalidCsvFormatException.class)
                    .hasMessageContaining("필수 헤더가 누락되었습니다");
        }

        @Test
        @DisplayName("누락된 헤더 목록이 예외에 포함된다")
        void shouldIncludeMissingHeadersInException() {
            var csv = "TimeGenerated [UTC],ClientIp\nval1,val2";

            try {
                parser.parse(toStream(csv));
            } catch (InvalidCsvFormatException e) {
                assertThat(e.getMissingHeaders()).isNotEmpty();
                assertThat(e.getMissingHeaders()).contains("HttpMethod", "RequestUri");
            }
        }

        @Test
        @DisplayName("헤더 대소문자가 달라도 정상 파싱된다")
        void shouldParseWithCaseInsensitiveHeaders() {
            var headers = VALID_HEADERS.toUpperCase();
            var csv = headers + "\n" + VALID_ROW;

            var result = parser.parse(toStream(csv));
            assertThat(result.logs()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("에러 분류 및 ParseStatistics 생성")
    class ErrorClassificationTest {

        @Test
        @DisplayName("빈 CSV 데이터도 헤더만 있으면 빈 결과를 반환한다")
        void shouldReturnEmptyResultWithHeadersOnly() {
            var csv = VALID_HEADERS + "\n";
            var result = parser.parse(toStream(csv));

            assertThat(result.logs()).isEmpty();
            assertThat(result.parseStatistics().totalLines()).isZero();
            assertThat(result.parseStatistics().errorCount()).isZero();
        }

        @Test
        @DisplayName("파싱 통계에 성공/에러 카운트가 포함된다")
        void shouldTrackSuccessAndErrorCounts() {
            var csv = VALID_HEADERS + "\n" + VALID_ROW + "\n" + VALID_ROW;
            var result = parser.parse(toStream(csv));

            assertThat(result.parseStatistics().totalLines()).isEqualTo(2);
            assertThat(result.parseStatistics().successCount()).isEqualTo(2);
            assertThat(result.parseStatistics().errorCount()).isZero();
        }
    }

    @Nested
    @DisplayName("null 입력 검증")
    class NullInputTest {

        @Test
        @DisplayName("inputStream이 null이면 NullPointerException 발생")
        void shouldThrowWhenInputStreamIsNull() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("inputStream");
        }
    }

    @Nested
    @DisplayName("외부 예외 처리")
    class ExternalExceptionTest {

        @Test
        @DisplayName("빈 스트림은 InvalidCsvFormatException 발생")
        void shouldThrowInvalidCsvFormatOnEmptyStream() {
            var csv = "";

            assertThatThrownBy(() -> parser.parse(toStream(csv)))
                    .isInstanceOf(InvalidCsvFormatException.class);
        }
    }

    @Nested
    @DisplayName("동시성 검증 (재진입성)")
    class ConcurrencyTest {

        @Test
        @DisplayName("여러 스레드에서 동시에 파싱해도 결과가 올바르다")
        void multipleParsingSimultaneously_safe() throws Exception {
            var csvData = VALID_HEADERS + "\n" + VALID_ROW + "\n" + VALID_ROW;
            int threadCount = 10;
            var executor = Executors.newFixedThreadPool(threadCount);
            var latch = new CountDownLatch(threadCount);
            var tasks = new ArrayList<Future<LogParser.ParseResult>>();

            for (int i = 0; i < threadCount; i++) {
                tasks.add(executor.submit(() -> {
                    try {
                        return parser.parse(toStream(csvData));
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            for (var task : tasks) {
                var result = task.get();
                assertThat(result.logs()).hasSize(2);
                assertThat(result.parseStatistics().totalLines()).isEqualTo(2);
                assertThat(result.parseStatistics().successCount()).isEqualTo(2);
                assertThat(result.parseStatistics().errorCount()).isZero();
            }
        }

        @Test
        @DisplayName("서로 다른 CSV를 동시에 파싱해도 결과가 섞이지 않는다")
        void differentCsvSimultaneously_noMixUp() throws Exception {
            int threadCount = 10;
            var executor = Executors.newFixedThreadPool(threadCount);
            var latch = new CountDownLatch(threadCount);
            var tasks = new ArrayList<Future<LogParser.ParseResult>>();

            for (int i = 0; i < threadCount; i++) {
                final int rowCount = i + 1; // 각 스레드가 다른 행 수
                var sb = new StringBuilder(VALID_HEADERS).append("\n");
                for (int r = 0; r < rowCount; r++) {
                    sb.append(VALID_ROW).append("\n");
                }
                var csvData = sb.toString();

                tasks.add(executor.submit(() -> {
                    try {
                        return parser.parse(toStream(csvData));
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            for (int i = 0; i < threadCount; i++) {
                var result = tasks.get(i).get();
                int expectedRows = i + 1;
                assertThat(result.logs()).hasSize(expectedRows);
                assertThat(result.parseStatistics().successCount()).isEqualTo(expectedRows);
            }
        }
    }
}
