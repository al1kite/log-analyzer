package com.electricip.loganalyzer.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

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

    @Builder.Default
    Map<String, IpInfo> ipDetails = Collections.emptyMap();
    @Builder.Default
    ParseErrors parseErrors = ParseErrors.empty();

    /**
     * 통계 Value Object
     */
    @Value
    @Builder
    public static class Statistics {
        long totalRequests;
        long successCount;
        long redirectCount;
        long clientErrorCount;
        long serverErrorCount;

        @NonNull @Builder.Default
        List<TopItem> topPaths = Collections.emptyList();
        @NonNull @Builder.Default
        List<TopItem> topStatusCodes = Collections.emptyList();
        @NonNull @Builder.Default
        List<TopItem> topIps = Collections.emptyList();
        @NonNull @Builder.Default
        Map<String, Long> methodStats = Collections.emptyMap();

        double avgResponseTime;
        double avgSentBytes;
        long totalTraffic;

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

        /**
         * 가변 컬렉션 반환 시 불변 뷰 제공
         */
        public List<TopItem> getTopPaths() {
            return Collections.unmodifiableList(topPaths);
        }

        public List<TopItem> getTopIps() {
            return Collections.unmodifiableList(topIps);
        }

        public Map<String, Long> getMethodStats() {
            return Collections.unmodifiableMap(methodStats);
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

    public Map<String, IpInfo> getIpDetails() {
        return Collections.unmodifiableMap(ipDetails);
    }
}
