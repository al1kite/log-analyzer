package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.config.LogAnalysisProperties;
import com.electricip.loganalyzer.domain.AccessLog;
import com.electricip.loganalyzer.domain.AnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        var statusCounts = new HashMap<String, Long>();
        var pathCounts = new HashMap<String, Long>();
        var statusCodeCounts = new HashMap<String, Long>();
        var ipCounts = new HashMap<String, Long>();
        var methodCounts = new HashMap<String, Long>();

        var responseTimeSum = 0.0;
        var responseTimeCount = 0;
        var totalTraffic = 0L;
        var sentBytesCount = 0;

        for (var log : logs) {
            statusCounts.merge(log.statusCategory(), 1L, Long::sum);

            if (log.requestUri() != null) {
                pathCounts.merge(log.requestUri(), 1L, Long::sum);
            }
            if (log.httpStatus() != null) {
                statusCodeCounts.merge(String.valueOf(log.httpStatus()), 1L, Long::sum);
            }
            if (log.clientIp() != null) {
                ipCounts.merge(log.clientIp(), 1L, Long::sum);
            }
            if (log.httpMethod() != null) {
                methodCounts.merge(log.httpMethod(), 1L, Long::sum);
            }
            if (log.clientResponseTime() != null) {
                responseTimeSum += log.clientResponseTime();
                responseTimeCount++;
            }
            if (log.sentBytes() != null) {
                totalTraffic += log.sentBytes();
                sentBytesCount++;
            }
        }

        var avgResponseTime = (responseTimeCount > 0) ? responseTimeSum / responseTimeCount : 0.0;
        var avgSentBytes = (sentBytesCount > 0) ? (double) totalTraffic / sentBytesCount : 0.0;

        return AnalysisResult.Statistics.builder()
                .totalRequests(logs.size())
                .successCount(statusCounts.getOrDefault("2xx", 0L))
                .redirectCount(statusCounts.getOrDefault("3xx", 0L))
                .clientErrorCount(statusCounts.getOrDefault("4xx", 0L))
                .serverErrorCount(statusCounts.getOrDefault("5xx", 0L))
                .topPaths(calculateTopN(pathCounts))
                .topStatusCodes(calculateTopN(statusCodeCounts))
                .topIps(calculateTopN(ipCounts))
                .methodStats(methodCounts)
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
