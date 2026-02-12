package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.config.LogAnalysisProperties;
import com.electricip.loganalyzer.domain.*;
import com.electricip.loganalyzer.infrastructure.client.IpInfoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.electricip.loganalyzer.domain.exception.InvalidFileException;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock private LogParser logParser;
    @Mock private IpInfoClient ipInfoClient;
    @Mock private AnalysisResultRepository repository;
    @Mock private StatisticsCalculator statisticsCalculator;

    private AnalysisService service;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        var properties = new LogAnalysisProperties(200_000, 10, 50, 5, null);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        service = new AnalysisService(
                logParser, ipInfoClient, repository,
                statisticsCalculator, properties, executor);
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    private LogParser.ParseResult createParseResult(int logCount) {
        var logs = Collections.nCopies(logCount, new AccessLog(
                null, "1.1.1.1", "GET", "/api/test", "Mozilla",
                200, "HTTP/1.1", 100L, 200L, 0.5, "TLSv1.2", "/api/test"));
        return new LogParser.ParseResult(logs,
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
        return mockFile("test.csv", "text/csv", "header1,header2\nval1,val2".getBytes());
    }

    private MultipartFile mockFile(String filename, String contentType, byte[] content) throws Exception {
        var file = mock(MultipartFile.class);
        lenient().when(file.isEmpty()).thenReturn(false);
        lenient().when(file.getSize()).thenReturn((long) content.length);
        lenient().when(file.getOriginalFilename()).thenReturn(filename);
        lenient().when(file.getContentType()).thenReturn(contentType);
        lenient().when(file.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(content));
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
        @DisplayName("IP 조회가 병렬로 실행된다")
        void shouldRunConcurrently() throws Exception {
            var file = mockFile();
            var topIps = List.of(
                    new AnalysisResult.TopItem("1.1.1.1", 100),
                    new AnalysisResult.TopItem("2.2.2.2", 50),
                    new AnalysisResult.TopItem("3.3.3.3", 25));

            // 3개 스레드가 모두 도착해야 통과하는 배리어
            var barrier = new CountDownLatch(3);

            when(logParser.parse(any())).thenReturn(createParseResult(3));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(topIps));
            when(ipInfoClient.getIpInfo(anyString())).thenAnswer(inv -> {
                barrier.countDown();
                // 순차 실행이면 여기서 영원히 대기 (3개가 동시에 도착해야 통과)
                assertThat(barrier.await(3, TimeUnit.SECONDS)).isTrue();
                String ip = inv.getArgument(0);
                return IpInfo.of(ip, "KR", "Seoul", "Seoul", "ISP");
            });

            var result = service.analyze(file);

            assertThat(result.getIpDetails()).hasSize(3);
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
    @DisplayName("파일 업로드 보안 검증")
    class FileUploadSecurityTest {

        @Test
        @DisplayName("Path Traversal 파일명은 거부된다")
        void shouldRejectPathTraversal() throws Exception {
            var file = mockFile("../../etc/passwd.csv", "text/csv", "data".getBytes());
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("잘못된 파일 이름");
        }

        @Test
        @DisplayName("백슬래시 Path Traversal도 거부된다")
        void shouldRejectBackslashTraversal() throws Exception {
            var file = mockFile("..\\..\\etc\\passwd.csv", "text/csv", "data".getBytes());
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("잘못된 파일 이름");
        }

        @Test
        @DisplayName("CRLF 인젝션 파일명은 거부된다")
        void shouldRejectCrlfInjection() throws Exception {
            var file = mockFile("test\r\ninjected.csv", "text/csv", "data".getBytes());
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("잘못된 파일 이름");
        }

        @Test
        @DisplayName("255자 초과 파일명은 거부된다")
        void shouldRejectLongFileName() throws Exception {
            var longName = "a".repeat(252) + ".csv";
            var file = mockFile(longName, "text/csv", "data".getBytes());
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("너무 깁니다");
        }

        @Test
        @DisplayName("허용되지 않은 Content-Type은 거부된다")
        void shouldRejectDisallowedContentType() throws Exception {
            var file = mockFile("test.csv", "application/javascript", "data".getBytes());
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("허용되지 않은 파일 타입");
        }

        @Test
        @DisplayName("null Content-Type은 허용된다 (확장자 검증 + 콘텐츠 스니핑으로 충분)")
        void shouldAllowNullContentType() throws Exception {
            var file = mockFile("test.csv", null, "data".getBytes());
            when(logParser.parse(any())).thenReturn(createParseResult(1));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(List.of()));

            var result = service.analyze(file);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Content-Type에 파라미터가 포함되어도 base type으로 판별된다")
        void shouldParseContentTypeWithParameters() throws Exception {
            var file = mockFile("test.csv", "text/csv; charset=UTF-8", "data".getBytes());
            when(logParser.parse(any())).thenReturn(createParseResult(1));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(List.of()));

            var result = service.analyze(file);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("ZIP 파일은 거부된다")
        void shouldRejectZipFile() throws Exception {
            var zipContent = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00};
            var file = mockFile("data.csv", "text/csv", zipContent);
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("압축 파일");
        }

        @Test
        @DisplayName("GZIP 파일은 거부된다")
        void shouldRejectGzipFile() throws Exception {
            var gzipContent = new byte[]{0x1F, (byte) 0x8B, 0x08, 0x00, 0x00};
            var file = mockFile("data.csv", "text/csv", gzipContent);
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("압축 파일");
        }

        @Test
        @DisplayName("바이너리 파일(NULL 바이트 포함)은 거부된다")
        void shouldRejectBinaryFile() throws Exception {
            var binaryContent = new byte[]{0x48, 0x65, 0x6C, 0x00, 0x6F};
            var file = mockFile("data.csv", "text/csv", binaryContent);
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("바이너리");
        }

        @Test
        @DisplayName("CSV 확장자가 아닌 파일은 거부된다")
        void shouldRejectNonCsvExtension() throws Exception {
            var file = mockFile("malware.exe", "text/csv", "data".getBytes());
            assertThatThrownBy(() -> service.analyze(file))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("CSV");
        }

        @Test
        @DisplayName("이중 확장자여도 .csv로 끝나면 통과한다")
        void shouldAllowDoubleExtensionEndingWithCsv() throws Exception {
            var file = mockFile("data.txt.csv", "text/csv", "data".getBytes());
            when(logParser.parse(any())).thenReturn(createParseResult(1));
            when(statisticsCalculator.calculate(any())).thenReturn(createStats(List.of()));

            var result = service.analyze(file);
            assertThat(result).isNotNull();
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
