package com.electricip.loganalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Set;

/**
 * 로그 분석 설정 프로퍼티
 *
 * @param maxFileLines              CSV 파일 최대 파싱 라인 수
 * @param topNResults               통계 상위 N개 결과 수
 * @param maxFileSizeMb             업로드 허용 최대 파일 크기 (MB)
 * @param ipEnrichmentTimeoutSeconds IP enrichment 전체 타임아웃 (초)
 */
@ConfigurationProperties(prefix = "log-analysis")
public record LogAnalysisProperties(
        @DefaultValue("200000") int maxFileLines,
        @DefaultValue("10") int topNResults,
        @DefaultValue("50") int maxFileSizeMb,
        @DefaultValue("5") int ipEnrichmentTimeoutSeconds,
        @DefaultValue("text/csv,text/plain,application/csv,application/vnd.ms-excel,application/octet-stream")
        List<String> allowedContentTypes
) {
    private static final Set<String> DEFAULT_CONTENT_TYPES = Set.of(
            "text/csv", "text/plain", "application/csv",
            "application/vnd.ms-excel", "application/octet-stream"
    );

    public LogAnalysisProperties {
        if (maxFileLines <= 0) {
            throw new IllegalArgumentException("maxFileLines는 양수여야 합니다: " + maxFileLines);
        }
        if (topNResults <= 0) {
            throw new IllegalArgumentException("topNResults는 양수여야 합니다: " + topNResults);
        }
        if (maxFileSizeMb <= 0) {
            throw new IllegalArgumentException("maxFileSizeMb는 양수여야 합니다: " + maxFileSizeMb);
        }
        if (ipEnrichmentTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("ipEnrichmentTimeoutSeconds는 양수여야 합니다: " + ipEnrichmentTimeoutSeconds);
        }
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            allowedContentTypes = List.copyOf(DEFAULT_CONTENT_TYPES);
        }
    }

    public Set<String> allowedContentTypeSet() {
        return Set.copyOf(allowedContentTypes);
    }
}
