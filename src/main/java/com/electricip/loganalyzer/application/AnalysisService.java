package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.config.LogAnalysisProperties;
import com.electricip.loganalyzer.domain.AnalysisResult;
import com.electricip.loganalyzer.domain.IpInfo;
import com.electricip.loganalyzer.domain.exception.FileTooLargeException;
import com.electricip.loganalyzer.domain.exception.InvalidFileException;
import com.electricip.loganalyzer.domain.exception.LogAnalyzerException;
import com.electricip.loganalyzer.domain.exception.LogParsingException;
import com.electricip.loganalyzer.domain.exception.TooManyParsingErrorsException;
import com.electricip.loganalyzer.infrastructure.client.IpInfoClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 로그 분석 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final LogParser logParser;
    private final IpInfoClient ipInfoClient;
    private final AnalysisResultRepository repository;
    private final StatisticsCalculator statisticsCalculator;
    private final LogAnalysisProperties properties;
    private final Executor ipEnrichmentExecutor;
    
    /**
     * 로그 분석
     *
     * @throws InvalidFileException 파일이 유효하지 않은 경우
     * @throws FileTooLargeException 파일 크기 초과 시
     * @throws TooManyParsingErrorsException 파싱 에러 과다 시
     * @throws LogParsingException 분석 실패 시
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
            var stats = parseResult.parseStatistics();

            if (logs.isEmpty()) {
                if (stats.errorCount() > 0) {
                    throw new TooManyParsingErrorsException(
                            String.format("유효한 로그가 없습니다 (전체 %d줄 중 %d줄 에러)",
                                    stats.totalLines(), stats.errorCount()),
                            stats.totalLines(), stats.errorCount(), stats.errorSamples());
                }
                throw new LogParsingException("유효한 로그가 없습니다");
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

        } catch (LogAnalyzerException e) {
            log.error("분석 실패: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("분석 실패 (내부 오류): {}", e.getMessage(), e);
            throw new LogParsingException("로그 분석에 실패했습니다", e);
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
            throw new InvalidFileException("파일이 비어있습니다");
        }

        var maxBytes = properties.maxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new FileTooLargeException(
                    String.format("파일 크기 초과 (최대 %dMB)", properties.maxFileSizeMb()),
                    file.getSize(), maxBytes);
        }

        validateFileName(file.getOriginalFilename());
        validateContentType(file.getContentType());
        validateFileContent(file);
    }

    private void validateFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new InvalidFileException("파일 이름이 없습니다");
        }

        // Path Traversal 방지
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new InvalidFileException("잘못된 파일 이름");
        }

        // CRLF 인젝션 방지
        if (filename.contains("\r") || filename.contains("\n")) {
            throw new InvalidFileException("잘못된 파일 이름");
        }

        // 파일 이름 길이 제한
        if (filename.length() > 255) {
            throw new InvalidFileException("파일 이름이 너무 깁니다 (최대 255자)");
        }

        // 확장자 검증
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidFileException("CSV 파일만 업로드 가능합니다");
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null) {
            return;
        }
        // "text/csv; charset=UTF-8" → "text/csv"
        var baseType = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!properties.allowedContentTypeSet().contains(baseType)) {
            throw new InvalidFileException("허용되지 않은 파일 타입: " + baseType);
        }
    }

    private void validateFileContent(MultipartFile file) {
        try (var is = file.getInputStream()) {
            var header = new byte[512];
            var bytesRead = is.read(header);

            if (bytesRead <= 0) {
                return;
            }

            // ZIP (PK) / GZIP 매직 넘버 차단
            if (bytesRead >= 2) {
                if (header[0] == 0x50 && header[1] == 0x4B) {
                    throw new InvalidFileException("압축 파일은 업로드할 수 없습니다");
                }
                if (header[0] == 0x1F && header[1] == (byte) 0x8B) {
                    throw new InvalidFileException("압축 파일은 업로드할 수 없습니다");
                }
            }

            // 바이너리 파일 감지 (NULL 바이트)
            for (int i = 0; i < bytesRead; i++) {
                byte b = header[i];
                if (b == 0) {
                    throw new InvalidFileException("바이너리 파일은 업로드할 수 없습니다");
                }
            }
        } catch (IOException e) {
            log.warn("파일 콘텐츠 검증 중 읽기 실패", e);
            throw new InvalidFileException("파일 읽기에 실패했습니다");
        }
    }
    
    /**
     * IP 정보 병렬 enrichment (CompletableFuture + 전용 스레드 풀)
     */
    private Map<String, IpInfo> enrichIpInfo(List<AnalysisResult.TopItem> topIps) {
        if (topIps.isEmpty()) {
            return Collections.emptyMap();
        }

        var ips = topIps.stream()
                .map(AnalysisResult.TopItem::item)
                .filter(ip -> ip != null && !ip.isBlank())
                .distinct()
                .toList();

        if (ips.isEmpty()) {
            return Collections.emptyMap();
        }

        var timeout = properties.ipEnrichmentTimeoutSeconds();

        var futures = ips.stream()
                .collect(Collectors.toMap(
                        ip -> ip,
                        ip -> CompletableFuture.supplyAsync(
                                () -> fetchIpInfoSafely(ip),
                                ipEnrichmentExecutor
                        ).completeOnTimeout(IpInfo.unknown(ip), timeout, TimeUnit.SECONDS)
                ));

        try {
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.warn("IP enrichment 중 예외 발생, 완료된 결과만 반환", e);
        }

        return futures.entrySet().stream()
                .filter(entry -> entry.getValue().isDone() && !entry.getValue().isCompletedExceptionally())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().join()
                ));
    }

    private IpInfo fetchIpInfoSafely(String ip) {
        try {
            return ipInfoClient.getIpInfo(ip);
        } catch (Exception e) {
            log.warn("IP 정보 조회 실패: ip={}, error={}", ip, e.getMessage());
            return IpInfo.unknown(ip);
        }
    }
}
