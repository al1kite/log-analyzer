package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.config.LogAnalysisProperties;
import com.electricip.loganalyzer.domain.AccessLog;
import com.electricip.loganalyzer.domain.AnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 통계 계산기
 */
@Component
@RequiredArgsConstructor
public class StatisticsCalculator {

    private final LogAnalysisProperties properties;
    
    /**
     * 통계 계산
     * 
     * @param logs 접속 로그 리스트 (null 불가, 비어있을 수 있음)
     * @return 통계 객체
     * @throws NullPointerException logs가 null인 경우
     */
    public AnalysisResult.Statistics calculate(List<AccessLog> logs) {
        Objects.requireNonNull(logs, "logs는 null일 수 없습니다");
        
        if (logs.isEmpty()) {
            return createEmptyStatistics();
        }
        
        // 상태 코드별 카운트
        var statusCounts = logs.stream()
                .collect(Collectors.groupingBy(
                        AccessLog::statusCategory,
                        Collectors.counting()));
        
        // 상위 N개 Path
        var topPaths = calculateTopN(logs.stream()
                .filter(log -> log.requestUri() != null)
                .collect(Collectors.groupingBy(
                        AccessLog::requestUri,
                        Collectors.counting())));
        
        // 상위 N개 상태 코드
        var topStatusCodes = calculateTopN(logs.stream()
                .filter(log -> log.httpStatus() != null)
                .collect(Collectors.groupingBy(
                        log -> String.valueOf(log.httpStatus()),
                        Collectors.counting())));
        
        // 상위 N개 IP
        var topIps = calculateTopN(logs.stream()
                .filter(log -> log.clientIp() != null)
                .collect(Collectors.groupingBy(
                        AccessLog::clientIp,
                        Collectors.counting())));
        
        // HTTP 메서드 통계
        var methodStats = logs.stream()
                .filter(log -> log.httpMethod() != null)
                .collect(Collectors.groupingBy(
                        AccessLog::httpMethod,
                        Collectors.counting()));
        
        // 평균 응답 시간
        var avgResponseTime = logs.stream()
                .filter(log -> log.clientResponseTime() != null)
                .mapToDouble(AccessLog::clientResponseTime)
                .average()
                .orElse(0.0);
        
        // 평균 송신 바이트
        var avgSentBytes = logs.stream()
                .filter(log -> log.sentBytes() != null)
                .mapToLong(AccessLog::sentBytes)
                .average()
                .orElse(0.0);
        
        // 총 트래픽
        var totalTraffic = logs.stream()
                .filter(log -> log.sentBytes() != null)
                .mapToLong(AccessLog::sentBytes)
                .sum();
        
        return AnalysisResult.Statistics.builder()
                .totalRequests(logs.size())
                .successCount(statusCounts.getOrDefault("2xx", 0L))
                .redirectCount(statusCounts.getOrDefault("3xx", 0L))
                .clientErrorCount(statusCounts.getOrDefault("4xx", 0L))
                .serverErrorCount(statusCounts.getOrDefault("5xx", 0L))
                .topPaths(topPaths)
                .topStatusCodes(topStatusCodes)
                .topIps(topIps)
                .methodStats(methodStats)
                .avgResponseTime(avgResponseTime)
                .avgSentBytes(avgSentBytes)
                .totalTraffic(totalTraffic)
                .build();
    }
    
    /**
     * 상위 N개 추출
     */
    private List<AnalysisResult.TopItem> calculateTopN(Map<String, Long> map) {
        if (map.isEmpty()) {
            return Collections.emptyList();
        }
        
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(properties.topNResults())
                .map(e -> new AnalysisResult.TopItem(e.getKey(), e.getValue()))
                .toList();
    }
    
    /**
     * 빈 통계 생성
     */
    private AnalysisResult.Statistics createEmptyStatistics() {
        return AnalysisResult.Statistics.builder()
                .totalRequests(0)
                .successCount(0)
                .redirectCount(0)
                .clientErrorCount(0)
                .serverErrorCount(0)
                .topPaths(Collections.emptyList())
                .topStatusCodes(Collections.emptyList())
                .topIps(Collections.emptyList())
                .methodStats(Collections.emptyMap())
                .avgResponseTime(0.0)
                .avgSentBytes(0.0)
                .totalTraffic(0)
                .build();
    }
}
