package com.electricip.loganalyzer.domain;

import java.util.List;
import java.util.Map;

/**
 * 파싱 통계 (Value Object)
 */
public record ParseStatistics(
        long totalLines,
        long successCount,
        long errorCount,
        Map<String, Integer> errorsByType,
        List<ParseError> errorSamples
) {

    public static final int MAX_ERROR_SAMPLES = 10;

    /**
     * Compact Constructor: 음수 검증 + 방어적 복사
     */
    public ParseStatistics {
        if (totalLines < 0) {
            throw new IllegalArgumentException("totalLines는 음수일 수 없습니다");
        }
        if (successCount < 0) {
            throw new IllegalArgumentException("successCount는 음수일 수 없습니다");
        }
        if (errorCount < 0) {
            throw new IllegalArgumentException("errorCount는 음수일 수 없습니다");
        }
        errorsByType = (errorsByType != null) ? Map.copyOf(errorsByType) : Map.of();
        errorSamples = (errorSamples != null) ? List.copyOf(errorSamples) : List.of();
    }

    /**
     * 방어적 복사 — 내부 컬렉션 노출 방지
     */
    public Map<String, Integer> errorsByType() {
        return Map.copyOf(errorsByType);
    }

    public List<ParseError> errorSamples() {
        return List.copyOf(errorSamples);
    }

    /**
     * 빈 ParseStatistics 팩터리 메서드
     */
    public static ParseStatistics empty() {
        return new ParseStatistics(0, 0, 0, Map.of(), List.of());
    }
}
