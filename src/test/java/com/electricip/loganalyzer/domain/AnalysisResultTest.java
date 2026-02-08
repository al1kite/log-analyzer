package com.electricip.loganalyzer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisResultTest {

    private static AnalysisResult.Statistics validStatistics() {
        return AnalysisResult.Statistics.builder()
                .totalRequests(100)
                .successCount(80)
                .redirectCount(5)
                .clientErrorCount(10)
                .serverErrorCount(5)
                .avgResponseTime(150.0)
                .avgSentBytes(1024.0)
                .totalTraffic(102400)
                .build();
    }

    @Nested
    @DisplayName("AnalysisResult 빌더 검증")
    class AnalysisResultBuilderTest {

        @Test
        @DisplayName("모든 필수 필드가 설정되면 정상 생성된다")
        void shouldBuildWithAllRequiredFields() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .statistics(validStatistics())
                    .build();

            assertEquals("test-id", result.getAnalysisId());
            assertNotNull(result.getCompletedAt());
            assertNotNull(result.getStatistics());
        }

        @Test
        @DisplayName("analysisId 누락 시 NullPointerException 발생")
        void shouldThrowWhenAnalysisIdIsNull() {
            var builder = AnalysisResult.builder()
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics());

            assertThrows(NullPointerException.class, builder::build);
        }

        @Test
        @DisplayName("completedAt 누락 시 NullPointerException 발생")
        void shouldThrowWhenCompletedAtIsNull() {
            var builder = AnalysisResult.builder()
                    .analysisId("test-id")
                    .statistics(validStatistics());

            assertThrows(NullPointerException.class, builder::build);
        }

        @Test
        @DisplayName("statistics 누락 시 NullPointerException 발생")
        void shouldThrowWhenStatisticsIsNull() {
            var builder = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now());

            assertThrows(NullPointerException.class, builder::build);
        }

        @Test
        @DisplayName("선택 필드 processingTimeMs는 null 허용")
        void shouldAllowNullProcessingTimeMs() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .build();

            assertNull(result.getProcessingTimeMs());
        }

        @Test
        @DisplayName("ipDetails 미설정 시 빈 맵이 기본값")
        void shouldDefaultIpDetailsToEmptyMap() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .build();

            assertNotNull(result.getIpDetails());
            assertTrue(result.getIpDetails().isEmpty());
        }

        @Test
        @DisplayName("parseErrors 미설정 시 empty가 기본값")
        void shouldDefaultParseErrorsToEmpty() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .build();

            assertNotNull(result.getParseErrors());
            assertEquals(0, result.getParseErrors().errorCount());
            assertTrue(result.getParseErrors().errorSamples().isEmpty());
        }
    }

    @Nested
    @DisplayName("Statistics 빌더 검증")
    class StatisticsBuilderTest {

        @Test
        @DisplayName("컬렉션 필드 미설정 시 빈 컬렉션이 기본값")
        void shouldDefaultCollectionsToEmpty() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(0)
                    .successCount(0)
                    .redirectCount(0)
                    .clientErrorCount(0)
                    .serverErrorCount(0)
                    .avgResponseTime(0.0)
                    .avgSentBytes(0.0)
                    .totalTraffic(0)
                    .build();

            assertNotNull(stats.getTopPaths());
            assertTrue(stats.getTopPaths().isEmpty());
            assertNotNull(stats.getTopStatusCodes());
            assertTrue(stats.getTopStatusCodes().isEmpty());
            assertNotNull(stats.getTopIps());
            assertTrue(stats.getTopIps().isEmpty());
            assertNotNull(stats.getMethodStats());
            assertTrue(stats.getMethodStats().isEmpty());
        }

        @Test
        @DisplayName("topPaths에 null을 명시적으로 전달하면 NullPointerException 발생")
        void shouldThrowWhenTopPathsIsExplicitlyNull() {
            assertThrows(NullPointerException.class, () ->
                    AnalysisResult.Statistics.builder()
                            .totalRequests(0)
                            .topPaths(null)
                            .build());
        }

        @Test
        @DisplayName("비율 계산이 정확하다")
        void shouldCalculateRatesCorrectly() {
            var stats = validStatistics();

            assertEquals(80.0, stats.successRate());
            assertEquals(5.0, stats.redirectRate());
            assertEquals(10.0, stats.clientErrorRate());
            assertEquals(5.0, stats.serverErrorRate());
        }

        @Test
        @DisplayName("totalRequests가 0이면 비율은 0.0")
        void shouldReturnZeroRateWhenNoRequests() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(0)
                    .build();

            assertEquals(0.0, stats.successRate());
            assertEquals(0.0, stats.clientErrorRate());
        }
    }
}
