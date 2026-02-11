package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.config.LogAnalysisProperties;
import com.electricip.loganalyzer.domain.*;
import com.electricip.loganalyzer.infrastructure.client.IpInfoClient;
import com.electricip.loganalyzer.infrastructure.parser.CsvLogParser;
import com.electricip.loganalyzer.infrastructure.repository.AnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock private CsvLogParser logParser;
    @Mock private IpInfoClient ipInfoClient;
    @Mock private AnalysisRepository repository;
    @Mock private StatisticsCalculator statisticsCalculator;

    private AnalysisService service;

    @BeforeEach
    void setUp() {
        var properties = new LogAnalysisProperties(200_000, 10, 50, 5);
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        service = new AnalysisService(
                logParser, ipInfoClient, repository,
                statisticsCalculator, properties, executor);
    }

    private CsvLogParser.ParseResult createParseResult(int logCount) {
        var logs = Collections.nCopies(logCount, new AccessLog(
                null, "1.1.1.1", "GET", "/api/test", "Mozilla",
                200, "HTTP/1.1", 100L, 200L, 0.5, "TLSv1.2", "/api/test"));
        return new CsvLogParser.ParseResult(logs,
                new ParseStatistics(logCount, logCount, 0, Map.of(), List.of()));
    }

    private AnalysisResult.Statistics createStats(List<AnalysisResult.TopItem> topIps) {
        return AnalysisResult.Statistics.builder()
                .totalRequests(100)
                .successCount(90)
                .redirectCount(5)
                .clientErrorCount(3)
                .serverErrorCount(2)
                .topPaths(List.of())
                .topStatusCodes(List.of())
                .topIps(topIps)
                .methodStats(Map.of())
                .avgResponseTime(0.5)
                .avgSentBytes(200.0)
                .totalTraffic(20000)
                .build();
    }

    private MultipartFile mockFile() throws Exception {
        var file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L);
        when(file.getOriginalFilename()).thenReturn("test.csv");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        return file;
    }

    @Nested
    @DisplayName("IP enrichment 병렬 처리")
    class IpEnrichmentTest {

        @Test
        @DisplayName("여러 IP가 병렬로 조회되어 결과에 포함된다")
        void shouldEnrichMultipleIpsInParallel() throws Exception {
            var file = mockFile();
            var topIps = List.of(
                    new AnalysisResult.TopItem("1.1.1.1", 100),
                    new AnalysisResult.TopItem("2.2.2.2", 50),
                    new AnalysisResult.TopItem("3.3.3.3", 25));

            when(logParser.parse(any())).thenReturn(createParseResult(3));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(topIps));
            when(ipInfoClient.getIpInfo(anyString())).thenAnswer(inv -> {
                String ip = inv.getArgument(0);
                return IpInfo.of(ip, "KR", "Seoul", "Seoul", "ISP");
            });

            var result = service.analyze(file);

            assertThat(result.getIpDetails()).hasSize(3);
            assertThat(result.getIpDetails()).containsKeys("1.1.1.1", "2.2.2.2", "3.3.3.3");
            assertThat(result.getIpDetails().get("1.1.1.1").isValid()).isTrue();
        }

        @Test
        @DisplayName("병렬 처리가 순차보다 빠르다")
        void shouldBeFasterThanSequential() throws Exception {
            var file = mockFile();
            var topIps = List.of(
                    new AnalysisResult.TopItem("1.1.1.1", 100),
                    new AnalysisResult.TopItem("2.2.2.2", 50),
                    new AnalysisResult.TopItem("3.3.3.3", 25));

            when(logParser.parse(any())).thenReturn(createParseResult(3));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(topIps));
            when(ipInfoClient.getIpInfo(anyString())).thenAnswer(inv -> {
                Thread.sleep(200);
                String ip = inv.getArgument(0);
                return IpInfo.of(ip, "KR", "Seoul", "Seoul", "ISP");
            });

            long start = System.currentTimeMillis();
            var result = service.analyze(file);
            long elapsed = System.currentTimeMillis() - start;

            assertThat(result.getIpDetails()).hasSize(3);
            // 순차: 3 × 200ms = 600ms, 병렬: ~200ms
            assertThat(elapsed).isLessThan(500);
        }
    }

    @Nested
    @DisplayName("Blank IP 필터링")
    class BlankIpFilteringTest {

        @Test
        @DisplayName("blank IP가 TopItem에 포함되어도 크래시 없이 필터링된다")
        void shouldFilterBlankIps() throws Exception {
            var file = mockFile();
            var topIps = List.of(
                    new AnalysisResult.TopItem("1.1.1.1", 100),
                    new AnalysisResult.TopItem("", 50));  // blank IP

            when(logParser.parse(any())).thenReturn(createParseResult(2));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(topIps));
            when(ipInfoClient.getIpInfo("1.1.1.1"))
                    .thenReturn(IpInfo.of("1.1.1.1", "KR", "Seoul", "Seoul", "ISP"));

            var result = service.analyze(file);

            assertThat(result.getIpDetails()).hasSize(1);
            assertThat(result.getIpDetails()).containsKey("1.1.1.1");
            // blank IP에 대해 getIpInfo가 호출되지 않아야 함
            verify(ipInfoClient, never()).getIpInfo("");
        }
    }

    @Nested
    @DisplayName("실패 복원력")
    class FailureResilienceTest {

        @Test
        @DisplayName("일부 IP 조회 실패 시 나머지 결과는 정상 반환된다")
        void shouldReturnPartialResultsOnPartialFailure() throws Exception {
            var file = mockFile();
            var topIps = List.of(
                    new AnalysisResult.TopItem("1.1.1.1", 100),
                    new AnalysisResult.TopItem("2.2.2.2", 50));

            when(logParser.parse(any())).thenReturn(createParseResult(2));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(topIps));
            when(ipInfoClient.getIpInfo("1.1.1.1"))
                    .thenReturn(IpInfo.of("1.1.1.1", "KR", "Seoul", "Seoul", "ISP"));
            when(ipInfoClient.getIpInfo("2.2.2.2"))
                    .thenThrow(new RuntimeException("API 장애"));

            var result = service.analyze(file);

            assertThat(result.getIpDetails()).hasSize(2);
            assertThat(result.getIpDetails().get("1.1.1.1").isValid()).isTrue();
            // 실패한 IP는 unknown으로 fallback
            assertThat(result.getIpDetails().get("2.2.2.2").isValid()).isFalse();
        }

        @Test
        @DisplayName("전체 IP 조회 실패 시에도 분석 결과는 반환된다")
        void shouldReturnResultEvenWhenAllIpLookupsFail() throws Exception {
            var file = mockFile();
            var topIps = List.of(new AnalysisResult.TopItem("1.1.1.1", 100));

            when(logParser.parse(any())).thenReturn(createParseResult(1));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(topIps));
            when(ipInfoClient.getIpInfo(anyString()))
                    .thenThrow(new RuntimeException("전체 장애"));

            var result = service.analyze(file);

            assertThat(result).isNotNull();
            assertThat(result.getStatistics().totalRequests()).isEqualTo(100);
            assertThat(result.getIpDetails().get("1.1.1.1").isValid()).isFalse();
        }

        @Test
        @DisplayName("중복 IP는 한 번만 조회된다")
        void shouldDeduplicateIps() throws Exception {
            var file = mockFile();
            var topIps = List.of(
                    new AnalysisResult.TopItem("1.1.1.1", 100),
                    new AnalysisResult.TopItem("1.1.1.1", 50));  // 중복

            when(logParser.parse(any())).thenReturn(createParseResult(2));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(topIps));

            var callCount = new AtomicInteger(0);
            when(ipInfoClient.getIpInfo("1.1.1.1")).thenAnswer(inv -> {
                callCount.incrementAndGet();
                return IpInfo.of("1.1.1.1", "KR", "Seoul", "Seoul", "ISP");
            });

            service.analyze(file);

            assertThat(callCount.get()).isEqualTo(1);
        }
    }
}
