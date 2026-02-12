package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.domain.AnalysisResult;
import com.electricip.loganalyzer.domain.ParseStatistics;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "분석 결과 응답")
@Builder
public record AnalysisResponse(
        @Schema(description = "분석 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NonNull String analysisId,
        @Schema(description = "분석 완료 시각", example = "2026-02-09T14:30:00")
        @NonNull LocalDateTime completedAt,
        @Schema(description = "처리 소요 시간 (ms)", example = "1250")
        Long processingTimeMs,
        @NonNull BasicStats basicStats,
        @NonNull TopStats topStats,
        @NonNull List<IpDetail> ipDetails,
        @NonNull ParseStatisticsDto parseStatistics,
        @NonNull AdditionalStats additionalStats,
        @Schema(description = "경고 메시지 목록")
        @NonNull List<String> warnings
) {

    public AnalysisResponse {
        ipDetails = List.copyOf(ipDetails);
        warnings = List.copyOf(warnings);
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
                .parseStatistics(convertParseStatistics(result.getParseStatistics()))
                .additionalStats(new AdditionalStats(
                        stats.methodStats(),
                        stats.avgResponseTime(),
                        stats.avgSentBytes(),
                        stats.totalTraffic()))
                .warnings(result.getWarnings())
                .build();
    }

    private static ParseStatisticsDto convertParseStatistics(ParseStatistics ps) {
        var errorDtos = ps.errorSamples().stream()
                .map(e -> new ParseErrorDto(
                        e.lineNumber(),
                        e.errorMessage(),
                        e.errorType().name(),
                        e.occurredAt()))
                .toList();

        return new ParseStatisticsDto(
                ps.totalLines(),
                ps.successCount(),
                ps.errorCount(),
                ps.errorsByType(),
                errorDtos);
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

    @Schema(description = "기본 통계")
    public record BasicStats(
            @Schema(description = "전체 요청 수", example = "15000")
            long totalRequests,
            @Schema(description = "성공 요청 수 (2xx)", example = "12000")
            long successCount,
            @Schema(description = "리다이렉트 수 (3xx)", example = "500")
            long redirectCount,
            @Schema(description = "클라이언트 에러 수 (4xx)", example = "2000")
            long clientErrorCount,
            @Schema(description = "서버 에러 수 (5xx)", example = "500")
            long serverErrorCount,
            @Schema(description = "성공률 (%)", example = "80.0")
            double successRate,
            @Schema(description = "리다이렉트율 (%)", example = "3.33")
            double redirectRate,
            @Schema(description = "클라이언트 에러율 (%)", example = "13.33")
            double clientErrorRate,
            @Schema(description = "서버 에러율 (%)", example = "3.33")
            double serverErrorRate) {}

    @Schema(description = "상위 항목 통계")
    public record TopStats(
            @Schema(description = "상위 요청 경로")
            List<PathStat> topPaths,
            @Schema(description = "상위 상태 코드")
            List<StatusCodeStat> topStatusCodes,
            @Schema(description = "상위 IP")
            List<IpStat> topIps) {

        public TopStats {
            topPaths = List.copyOf(topPaths);
            topStatusCodes = List.copyOf(topStatusCodes);
            topIps = List.copyOf(topIps);
        }
    }

    @Schema(description = "경로별 요청 통계")
    public record PathStat(
            @Schema(description = "요청 경로", example = "/api/users")
            String path,
            @Schema(description = "요청 횟수", example = "3500")
            long count) {}

    @Schema(description = "상태 코드별 통계")
    public record StatusCodeStat(
            @Schema(description = "HTTP 상태 코드", example = "200")
            String statusCode,
            @Schema(description = "발생 횟수", example = "12000")
            long count) {}

    @Schema(description = "IP별 요청 통계")
    public record IpStat(
            @Schema(description = "클라이언트 IP", example = "192.168.1.100")
            String ip,
            @Schema(description = "요청 횟수", example = "850")
            long count) {}

    @Schema(description = "IP 상세 정보")
    public record IpDetail(
            @Schema(description = "IP 주소", example = "8.8.8.8")
            String ip,
            @Schema(description = "국가", example = "US")
            String country,
            @Schema(description = "지역", example = "California")
            String region,
            @Schema(description = "도시", example = "Mountain View")
            String city,
            @Schema(description = "조직/ISP", example = "Google LLC")
            String organization) {}

    @Schema(description = "파싱 통계")
    public record ParseStatisticsDto(
            @Schema(description = "전체 라인 수", example = "15200")
            long totalLines,
            @Schema(description = "파싱 성공 수", example = "15000")
            long successCount,
            @Schema(description = "파싱 에러 수", example = "200")
            long errorCount,
            @Schema(description = "에러 유형별 카운트")
            Map<String, Integer> errorsByType,
            @Schema(description = "에러 샘플 목록")
            List<ParseErrorDto> errorSamples) {

        public ParseStatisticsDto {
            errorsByType = (errorsByType != null) ? Map.copyOf(errorsByType) : Map.of();
            errorSamples = (errorSamples != null) ? List.copyOf(errorSamples) : List.of();
        }
    }

    @Schema(description = "파싱 에러 상세")
    public record ParseErrorDto(
            @Schema(description = "에러 발생 라인 번호", example = "42")
            long lineNumber,
            @Schema(description = "에러 메시지", example = "잘못된 날짜 형식")
            String errorMessage,
            @Schema(description = "에러 유형", example = "PARSING")
            String errorType,
            @Schema(description = "에러 발생 시각", example = "2026-02-09T14:30:00")
            LocalDateTime occurredAt) {}

    @Schema(description = "추가 통계")
    public record AdditionalStats(
            @Schema(description = "HTTP 메서드별 요청 수")
            Map<String, Long> methodStats,
            @Schema(description = "평균 응답 시간 (ms)", example = "125.5")
            double avgResponseTime,
            @Schema(description = "평균 전송 바이트", example = "2048.0")
            double avgSentBytes,
            @Schema(description = "총 트래픽 (bytes)", example = "30720000")
            long totalTraffic) {

        public AdditionalStats {
            methodStats = Map.copyOf(methodStats);
        }
    }
}
