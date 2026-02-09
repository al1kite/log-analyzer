package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.domain.AnalysisResult;
import com.electricip.loganalyzer.domain.InvalidCsvFormatException;
import com.electricip.loganalyzer.domain.IpInfo;
import com.electricip.loganalyzer.infrastructure.client.IpInfoClient;
import com.electricip.loganalyzer.infrastructure.parser.CsvLogParser;
import com.electricip.loganalyzer.infrastructure.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 로그 분석 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {
    
    private final CsvLogParser logParser;
    private final IpInfoClient ipInfoClient;
    private final AnalysisRepository repository;
    private final StatisticsCalculator statisticsCalculator;
    
    @Value("${log-analysis.max-file-size-mb:50}")
    private int maxFileSizeMb;
    
    /**
     * 로그 분석
     * 
     * @throws IllegalArgumentException 파일이 유효하지 않은 경우
     * @throws RuntimeException 분석 실패 시
     */
    public AnalysisResult analyze(MultipartFile file) {
        Objects.requireNonNull(file, "file은 null일 수 없습니다");
        validateFile(file);
        
        var startTime = System.currentTimeMillis();
        
        log.info("분석 시작: file={}, size={}bytes", file.getOriginalFilename(), file.getSize());
        
        try {
            // 1. 파싱
            var parseResult = logParser.parse(file.getInputStream());
            var logs = parseResult.logs();

            if (logs.isEmpty()) {
                throw new IllegalStateException("유효한 로그가 없습니다");
            }

            // 2. 통계 계산
            var statistics = statisticsCalculator.calculate(logs);

            // 3. IP enrichment
            var ipDetails = enrichIpInfo(statistics.topIps());

            // 4. 결과 생성
            var result = AnalysisResult.builder()
                    .analysisId(UUID.randomUUID().toString())
                    .completedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .statistics(statistics)
                    .ipDetails(ipDetails)
                    .parseStatistics(parseResult.parseStatistics())
                    .build();

            // 5. 저장
            repository.save(result);

            log.info("분석 완료: id={}, duration={}ms, total={}",
                    result.getAnalysisId(),
                    result.getProcessingTimeMs(),
                    statistics.totalRequests());

            return result;

        } catch (InvalidCsvFormatException | IllegalArgumentException | IllegalStateException e) {
            log.error("분석 실패: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("분석 실패: {}", e.getMessage(), e);
            throw new RuntimeException("로그 분석에 실패했습니다: " + e.getMessage(), e);
        }
    }

    public Optional<AnalysisResult> getById(String analysisId) {
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
        return repository.findById(analysisId);
    }

    public List<AnalysisResult> getAll() {
        return repository.findAll();
    }
    
    /**
     * 삭제
     */
    public boolean delete(String analysisId) {
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
        return repository.deleteById(analysisId);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }
        
        var maxBytes = maxFileSizeMb * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    String.format("파일 크기 초과 (최대 %dMB)", maxFileSizeMb));
        }
        
        var filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("CSV 파일만 업로드 가능합니다");
        }
    }
    
    /**
     * IP 정보 enrichment
     */
    private Map<String, IpInfo> enrichIpInfo(List<AnalysisResult.TopItem> topIps) {
        if (topIps.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return topIps.stream()
                .map(AnalysisResult.TopItem::item)
                .collect(Collectors.toMap(
                        ip -> ip,
                        ip -> {
                            try {
                                return ipInfoClient.getIpInfo(ip);
                            } catch (Exception e) {
                                log.warn("IP 정보 조회 실패: ip={}", ip);
                                return IpInfo.unknown(ip);
                            }
                        }
                ));
    }
}
