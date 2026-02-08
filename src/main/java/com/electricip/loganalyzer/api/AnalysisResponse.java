package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.domain.AnalysisResult;
import lombok.Builder;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 분석 결과 응답 DTO
 */
@Builder
public record AnalysisResponse(
        @NonNull String analysisId,
        @NonNull LocalDateTime completedAt,
        Long processingTimeMs,
        @NonNull BasicStats basicStats,
        @NonNull TopStats topStats,
        @NonNull List<IpDetail> ipDetails,
        @NonNull ParseErrors parseErrors,
        @NonNull AdditionalStats additionalStats
) {

    public AnalysisResponse {
        ipDetails = List.copyOf(ipDetails);
    }

    /**
     * 도메인 모델을 DTO로 변환
     */
    public static AnalysisResponse from(AnalysisResult result) {
        Objects.requireNonNull(result, "result는 null일 수 없습니다");

        var stats = result.getStatistics();

        return AnalysisResponse.builder()
                .analysisId(result.getAnalysisId())
                .completedAt(result.getCompletedAt())
                .processingTimeMs(result.getProcessingTimeMs())
                .basicStats(new BasicStats(
                        stats.totalRequests(),
                        stats.successCount(),
                        stats.redirectCount(),
                        stats.clientErrorCount(),
                        stats.serverErrorCount(),
                        stats.successRate(),
                        stats.redirectRate(),
                        stats.clientErrorRate(),
                        stats.serverErrorRate()))
                .topStats(new TopStats(
                        convertTopItems(stats.topPaths()),
                        convertTopItemsForStatusCodes(stats.topStatusCodes()),
                        convertTopItemsForIps(stats.topIps())))
                .ipDetails(convertIpDetails(result.getIpDetails()))
                .parseErrors(new ParseErrors(
                        result.getParseErrors().errorCount(),
                        result.getParseErrors().errorSamples()))
                .additionalStats(new AdditionalStats(
                        stats.methodStats(),
                        stats.avgResponseTime(),
                        stats.avgSentBytes(),
                        stats.totalTraffic()))
                .build();
    }

    private static List<PathStat> convertTopItems(List<AnalysisResult.TopItem> items) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(t -> new PathStat(t.item(), t.count()))
                .toList();
    }

    private static List<StatusCodeStat> convertTopItemsForStatusCodes(List<AnalysisResult.TopItem> items) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(t -> new StatusCodeStat(t.item(), t.count()))
                .toList();
    }

    private static List<IpStat> convertTopItemsForIps(List<AnalysisResult.TopItem> items) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(t -> new IpStat(t.item(), t.count()))
                .toList();
    }

    private static List<IpDetail> convertIpDetails(Map<String, com.electricip.loganalyzer.domain.IpInfo> ipInfoMap) {
        if (ipInfoMap.isEmpty()) {
            return Collections.emptyList();
        }
        return ipInfoMap.entrySet().stream()
                .map(e -> new IpDetail(
                        e.getKey(),
                        e.getValue().country(),
                        e.getValue().region(),
                        e.getValue().city(),
                        e.getValue().organization()))
                .toList();
    }

    // 내부 Record들

    public record BasicStats(
            long totalRequests,
            long successCount,
            long redirectCount,
            long clientErrorCount,
            long serverErrorCount,
            double successRate,
            double redirectRate,
            double clientErrorRate,
            double serverErrorRate) {}

    public record TopStats(
            List<PathStat> topPaths,
            List<StatusCodeStat> topStatusCodes,
            List<IpStat> topIps) {

        public TopStats {
            topPaths = List.copyOf(topPaths);
            topStatusCodes = List.copyOf(topStatusCodes);
            topIps = List.copyOf(topIps);
        }
    }

    public record PathStat(String path, long count) {}
    public record StatusCodeStat(String statusCode, long count) {}
    public record IpStat(String ip, long count) {}

    public record IpDetail(
            String ip,
            String country,
            String region,
            String city,
            String organization) {}

    public record ParseErrors(
            int errorCount,
            List<String> errorSamples) {
        public ParseErrors {
            errorSamples = List.copyOf(errorSamples);
        }
    }

    public record AdditionalStats(
            Map<String, Long> methodStats,
            double avgResponseTime,
            double avgSentBytes,
            long totalTraffic) {

        public AdditionalStats {
            methodStats = Map.copyOf(methodStats);
        }
    }
}
