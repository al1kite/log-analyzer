package com.electricip.loganalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

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
        @DefaultValue("5") int ipEnrichmentTimeoutSeconds
) {}
