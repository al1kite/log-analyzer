package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.application.AnalysisService;
import com.electricip.loganalyzer.application.RateLimitService;
import com.electricip.loganalyzer.domain.AnalysisResult;
import com.electricip.loganalyzer.domain.ParseStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisController")
class AnalysisControllerTest {

    @Mock
    private AnalysisService analysisService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private AnalysisController controller;

    private static final String TEST_ANALYSIS_ID = "test-id-123";
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 2, 10, 14, 30, 0);

    private static AnalysisResult createTestResult() {
        return AnalysisResult.builder()
                .analysisId(TEST_ANALYSIS_ID)
                .completedAt(COMPLETED_AT)
                .processingTimeMs(1000L)
                .statistics(AnalysisResult.Statistics.builder()
                        .totalRequests(100)
                        .successCount(80)
                        .redirectCount(5)
                        .clientErrorCount(10)
                        .serverErrorCount(5)
                        .topPaths(List.of(new AnalysisResult.TopItem("/api/users", 50)))
                        .topStatusCodes(List.of(new AnalysisResult.TopItem("200", 80)))
                        .topIps(List.of(new AnalysisResult.TopItem("192.168.1.1", 30)))
                        .methodStats(Map.of("GET", 70L, "POST", 30L))
                        .avgResponseTime(125.5)
                        .avgSentBytes(2048.0)
                        .totalTraffic(204800)
                        .build())
                .ipDetails(Map.of())
                .warnings(List.of())
                .parseStatistics(ParseStatistics.empty())
                .build();
    }

    @Nested
    @DisplayName("POST /api/analysis")
    class AnalyzeTest {

        @Test
        @DisplayName("201 Created를 반환하고 Location 헤더가 포함된다")
        void shouldReturn201WithLocationHeader() {
            var mockRequest = new MockHttpServletRequest("POST", "/api/analysis");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

            var file = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());
            var result = createTestResult();
            given(analysisService.analyze(any())).willReturn(result);

            var response = controller.analyze(file, mockRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getHeaders().getLocation()).isNotNull();
            assertThat(response.getHeaders().getLocation().toString()).contains(TEST_ANALYSIS_ID);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().analysisId()).isEqualTo(TEST_ANALYSIS_ID);
            then(rateLimitService).should().checkRateLimit(mockRequest.getRemoteAddr());

            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Nested
    @DisplayName("GET /api/analysis/{analysisId}")
    class GetResultTest {

        @Test
        @DisplayName("200 OK와 분석 결과를 반환한다")
        void shouldReturn200WithResult() {
            var result = createTestResult();
            given(analysisService.getById(TEST_ANALYSIS_ID)).willReturn(Optional.of(result));

            var response = controller.getResult(TEST_ANALYSIS_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().analysisId()).isEqualTo(TEST_ANALYSIS_ID);
        }
    }

    @Nested
    @DisplayName("DELETE /api/analysis/{analysisId}")
    class DeleteResultTest {

        @Test
        @DisplayName("삭제 성공 시 204 No Content를 반환한다")
        void shouldReturn204OnSuccess() {
            given(analysisService.delete(TEST_ANALYSIS_ID)).willReturn(true);

            var response = controller.deleteResult(TEST_ANALYSIS_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("미존재 리소스 삭제 시 404 Not Found를 반환한다")
        void shouldReturn404WhenNotFound() {
            given(analysisService.delete("nonexistent")).willReturn(false);

            var response = controller.deleteResult("nonexistent");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("OPTIONS /api/analysis")
    class OptionsTest {

        @Test
        @DisplayName("Allow 헤더에 지원 메서드가 포함된다")
        void shouldReturnAllowHeader() {
            var response = controller.options();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var allowedMethods = response.getHeaders().getAllow();
            assertThat(allowedMethods).contains(
                    HttpMethod.GET,
                    HttpMethod.POST,
                    HttpMethod.DELETE,
                    HttpMethod.OPTIONS
            );
        }
    }
}
