package com.electricip.loganalyzer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                    .build();

            assertNotNull(stats.topPaths());
            assertTrue(stats.topPaths().isEmpty());
            assertNotNull(stats.topStatusCodes());
            assertTrue(stats.topStatusCodes().isEmpty());
            assertNotNull(stats.topIps());
            assertTrue(stats.topIps().isEmpty());
            assertNotNull(stats.methodStats());
            assertTrue(stats.methodStats().isEmpty());
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

    @Nested
    @DisplayName("Statistics 불변성 검증")
    class StatisticsImmutabilityTest {

        @Test
        @DisplayName("빌더에 전달한 원본 리스트를 수정해도 Statistics 내부 상태는 변하지 않는다")
        void shouldNotBeAffectedByOriginalListModification() {
            var mutableList = new ArrayList<>(List.of(
                    new AnalysisResult.TopItem("/api", 100)));

            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .topPaths(mutableList)
                    .build();

            mutableList.clear();

            assertEquals(1, stats.topPaths().size());
            assertEquals("/api", stats.topPaths().get(0).item());
        }

        @Test
        @DisplayName("빌더에 전달한 원본 맵을 수정해도 Statistics 내부 상태는 변하지 않는다")
        void shouldNotBeAffectedByOriginalMapModification() {
            var mutableMap = new HashMap<String, Long>();
            mutableMap.put("GET", 50L);

            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .methodStats(mutableMap)
                    .build();

            mutableMap.clear();

            assertEquals(1, stats.methodStats().size());
            assertEquals(50L, stats.methodStats().get("GET"));
        }

        @Test
        @DisplayName("accessor로 반환된 리스트는 수정할 수 없다")
        void shouldReturnUnmodifiableList() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .topPaths(List.of(new AnalysisResult.TopItem("/api", 100)))
                    .build();

            assertThrows(UnsupportedOperationException.class, () ->
                    stats.topPaths().clear());
        }

        @Test
        @DisplayName("accessor로 반환된 맵은 수정할 수 없다")
        void shouldReturnUnmodifiableMap() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .methodStats(java.util.Map.of("GET", 50L))
                    .build();

            assertThrows(UnsupportedOperationException.class, () ->
                    stats.methodStats().clear());
        }
    }
}
