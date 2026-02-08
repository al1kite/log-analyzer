package com.electricip.loganalyzer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisResponseTest {

    private static AnalysisResponse.BasicStats validBasicStats() {
        return new AnalysisResponse.BasicStats(100, 80, 5, 10, 5, 80.0, 5.0, 10.0, 5.0);
    }

    private static AnalysisResponse.TopStats validTopStats() {
        return new AnalysisResponse.TopStats(
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private static AnalysisResponse.ParseStatisticsDto validParseStatistics() {
        return new AnalysisResponse.ParseStatisticsDto(0, 0, 0, Map.of(), Collections.emptyList());
    }

    private static AnalysisResponse.AdditionalStats validAdditionalStats() {
        return new AnalysisResponse.AdditionalStats(Map.of(), 0.0, 0.0, 0);
    }

    @Test
    @DisplayName("모든 필수 필드가 설정되면 정상 생성된다")
    void shouldBuildWithAllRequiredFields() {
        var response = AnalysisResponse.builder()
                .analysisId("test-id")
                .completedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .basicStats(validBasicStats())
                .topStats(validTopStats())
                .ipDetails(Collections.emptyList())
                .parseStatistics(validParseStatistics())
                .additionalStats(validAdditionalStats())
                .build();

        assertEquals("test-id", response.analysisId());
        assertNotNull(response.completedAt());
        assertNotNull(response.basicStats());
    }

    @Test
    @DisplayName("analysisId 누락 시 NullPointerException 발생")
    void shouldThrowWhenAnalysisIdIsNull() {
        assertThrows(NullPointerException.class, () ->
                AnalysisResponse.builder()
                        .completedAt(LocalDateTime.now())
                        .basicStats(validBasicStats())
                        .topStats(validTopStats())
                        .ipDetails(Collections.emptyList())
                        .parseStatistics(validParseStatistics())
                        .additionalStats(validAdditionalStats())
                        .build());
    }

    @Test
    @DisplayName("completedAt 누락 시 NullPointerException 발생")
    void shouldThrowWhenCompletedAtIsNull() {
        assertThrows(NullPointerException.class, () ->
                AnalysisResponse.builder()
                        .analysisId("test-id")
                        .basicStats(validBasicStats())
                        .topStats(validTopStats())
                        .ipDetails(Collections.emptyList())
                        .parseStatistics(validParseStatistics())
                        .additionalStats(validAdditionalStats())
                        .build());
    }

    @Test
    @DisplayName("basicStats 누락 시 NullPointerException 발생")
    void shouldThrowWhenBasicStatsIsNull() {
        assertThrows(NullPointerException.class, () ->
                AnalysisResponse.builder()
                        .analysisId("test-id")
                        .completedAt(LocalDateTime.now())
                        .topStats(validTopStats())
                        .ipDetails(Collections.emptyList())
                        .parseStatistics(validParseStatistics())
                        .additionalStats(validAdditionalStats())
                        .build());
    }

    @Test
    @DisplayName("선택 필드 processingTimeMs는 null 허용")
    void shouldAllowNullProcessingTimeMs() {
        var response = AnalysisResponse.builder()
                .analysisId("test-id")
                .completedAt(LocalDateTime.now())
                .basicStats(validBasicStats())
                .topStats(validTopStats())
                .ipDetails(Collections.emptyList())
                .parseStatistics(validParseStatistics())
                .additionalStats(validAdditionalStats())
                .build();

        assertNull(response.processingTimeMs());
    }
}
