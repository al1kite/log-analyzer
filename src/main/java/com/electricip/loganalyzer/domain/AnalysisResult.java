package com.electricip.loganalyzer.domain;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 분석 결과 Aggregate
 */
@Value
@Builder
public class AnalysisResult {
    @NonNull String analysisId;
    @NonNull LocalDateTime completedAt;
    Long processingTimeMs;
    @NonNull Statistics statistics;

    @Singular("ipDetail")
    Map<String, IpInfo> ipDetails;

    ParseErrors parseErrors;

    /**
     * 명시적 생성자
     */
    public AnalysisResult(
            @NonNull String analysisId,
            @NonNull LocalDateTime completedAt,
            Long processingTimeMs,
            @NonNull Statistics statistics,
            Map<String, IpInfo> ipDetails,
            ParseErrors parseErrors
    ) {
        this.analysisId = analysisId;
        this.completedAt = completedAt;
        this.processingTimeMs = processingTimeMs;
        this.statistics = statistics;

        this.ipDetails = (ipDetails == null) ? Map.of() : Map.copyOf(ipDetails);

        // 기본값 보장
        this.parseErrors = (parseErrors == null) ? ParseErrors.empty() : parseErrors;
    }


    /**
     * 통계 Value Object
     */
    @Builder
    public record Statistics(
            long totalRequests,
            long successCount,
            long redirectCount,
            long clientErrorCount,
            long serverErrorCount,
            List<TopItem> topPaths,
            List<TopItem> topStatusCodes,
            List<TopItem> topIps,
            Map<String, Long> methodStats,
            double avgResponseTime,
            double avgSentBytes,
            long totalTraffic
    ) {
        /**
         * Compact Constructor: 방어적 복사
         */
        public Statistics {
            topPaths = (topPaths != null) ? List.copyOf(topPaths) : List.of();
            topStatusCodes = (topStatusCodes != null) ? List.copyOf(topStatusCodes) : List.of();
            topIps = (topIps != null) ? List.copyOf(topIps) : List.of();
            methodStats = (methodStats != null) ? Map.copyOf(methodStats) : Map.of();
        }

        /**
         * 도메인 로직: 비율 계산
         */
        public double successRate() {
            return calculateRate(successCount);
        }

        public double redirectRate() {
            return calculateRate(redirectCount);
        }

        public double clientErrorRate() {
            return calculateRate(clientErrorCount);
        }

        public double serverErrorRate() {
            return calculateRate(serverErrorCount);
        }

        private double calculateRate(long count) {
            if (totalRequests == 0) return 0.0;
            return Math.round((count * 100.0 / totalRequests) * 100.0) / 100.0;
        }
    }

    /**
     * Top Item (Record)
     */
    public record TopItem(String item, long count) {
        public TopItem {
            if (item == null) {
                throw new IllegalArgumentException("item은 null일 수 없습니다");
            }
            if (count < 0) {
                throw new IllegalArgumentException("count는 음수일 수 없습니다");
            }
        }
    }

    /**
     * 파싱 오류 (Record)
     */
    public record ParseErrors(int errorCount, List<String> errorSamples) {
        public static ParseErrors empty() {
            return new ParseErrors(0, Collections.emptyList());
        }

        public ParseErrors {
            errorSamples = List.copyOf(errorSamples);
        }

        public List<String> errorSamples() {
            return errorSamples; // 이미 불변
        }
    }
}
