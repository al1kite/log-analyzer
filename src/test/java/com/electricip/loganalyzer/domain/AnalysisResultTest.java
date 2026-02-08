package com.electricip.loganalyzer.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisResultTest {

    private static AnalysisResult.Statistics validStatistics() {
        return AnalysisResult.Statistics.builder()
                .totalRequests(100)
                .successCount(80)
                .redirectCount(5)
                .clientErrorCount(10)
                .serverErrorCount(5)
                .avgResponseTime(150.0)
                .avgSentBytes(1024.0)
                .totalTraffic(102400)
                .build();
    }

    @Nested
    @DisplayName("AnalysisResult 빌더 검증")
    class AnalysisResultBuilderTest {

        @Test
        @DisplayName("모든 필수 필드가 설정되면 정상 생성된다")
        void shouldBuildWithAllRequiredFields() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .statistics(validStatistics())
                    .build();

            assertThat(result.getAnalysisId()).isEqualTo("test-id");
            assertThat(result.getCompletedAt()).isNotNull();
            assertThat(result.getStatistics()).isNotNull();
        }

        @Test
        @DisplayName("analysisId 누락 시 NullPointerException 발생")
        void shouldThrowWhenAnalysisIdIsNull() {
            var builder = AnalysisResult.builder()
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics());

            assertThatThrownBy(builder::build)
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("completedAt 누락 시 NullPointerException 발생")
        void shouldThrowWhenCompletedAtIsNull() {
            var builder = AnalysisResult.builder()
                    .analysisId("test-id")
                    .statistics(validStatistics());

            assertThatThrownBy(builder::build)
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("statistics 누락 시 NullPointerException 발생")
        void shouldThrowWhenStatisticsIsNull() {
            var builder = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now());

            assertThatThrownBy(builder::build)
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("선택 필드 processingTimeMs는 null 허용")
        void shouldAllowNullProcessingTimeMs() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .build();

            assertThat(result.getProcessingTimeMs()).isNull();
        }

        @Test
        @DisplayName("processingTimeMs 설정 시 정상 반환")
        void shouldReturnProcessingTimeMs() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .processingTimeMs(1500L)
                    .build();

            assertThat(result.getProcessingTimeMs()).isEqualTo(1500L);
        }

        @Test
        @DisplayName("ipDetails 미설정 시 빈 맵이 기본값")
        void shouldDefaultIpDetailsToEmptyMap() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .build();

            assertThat(result.getIpDetails()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("parseErrors 미설정 시 empty가 기본값")
        void shouldDefaultParseErrorsToEmpty() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .build();

            assertThat(result.getParseErrors()).isNotNull();
            assertThat(result.getParseErrors().errorCount()).isZero();
            assertThat(result.getParseErrors().errorSamples()).isEmpty();
        }

        @Test
        @DisplayName("ipDetails에 @Singular로 개별 항목 추가 가능")
        void shouldAddIndividualIpDetail() {
            var info = IpInfo.of("1.2.3.4", "KR", "Seoul", "Gangnam", "ISP");
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .ipDetail("1.2.3.4", info)
                    .build();

            assertThat(result.getIpDetails()).hasSize(1);
            assertThat(result.getIpDetails().get("1.2.3.4")).isEqualTo(info);
        }
    }

    @Nested
    @DisplayName("AnalysisResult 불변성 검증")
    class AnalysisResultImmutabilityTest {

        @Test
        @DisplayName("ipDetails 맵은 수정할 수 없다")
        void shouldReturnUnmodifiableIpDetails() {
            var result = AnalysisResult.builder()
                    .analysisId("test-id")
                    .completedAt(LocalDateTime.now())
                    .statistics(validStatistics())
                    .ipDetail("1.2.3.4", IpInfo.unknown("1.2.3.4"))
                    .build();

            assertThatThrownBy(() -> result.getIpDetails().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("원본 맵 수정이 AnalysisResult에 영향을 주지 않는다")
        void shouldDefensivelyCopyIpDetails() {
            var mutableMap = new HashMap<String, IpInfo>();
            mutableMap.put("1.2.3.4", IpInfo.unknown("1.2.3.4"));

            var result = new AnalysisResult(
                    "test-id",
                    LocalDateTime.now(),
                    null,
                    validStatistics(),
                    mutableMap,
                    null
            );

            mutableMap.clear();

            assertThat(result.getIpDetails()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Statistics 빌더 검증")
    class StatisticsBuilderTest {

        @Test
        @DisplayName("컬렉션 필드 미설정 시 빈 컬렉션이 기본값")
        void shouldDefaultCollectionsToEmpty() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(0)
                    .build();

            assertThat(stats.topPaths()).isNotNull().isEmpty();
            assertThat(stats.topStatusCodes()).isNotNull().isEmpty();
            assertThat(stats.topIps()).isNotNull().isEmpty();
            assertThat(stats.methodStats()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("비율 계산이 정확하다")
        void shouldCalculateRatesCorrectly() {
            var stats = validStatistics();

            assertThat(stats.successRate()).isEqualTo(80.0);
            assertThat(stats.redirectRate()).isEqualTo(5.0);
            assertThat(stats.clientErrorRate()).isEqualTo(10.0);
            assertThat(stats.serverErrorRate()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("totalRequests가 0이면 비율은 0.0")
        void shouldReturnZeroRateWhenNoRequests() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(0)
                    .build();

            assertThat(stats.successRate()).isEqualTo(0.0);
            assertThat(stats.redirectRate()).isEqualTo(0.0);
            assertThat(stats.clientErrorRate()).isEqualTo(0.0);
            assertThat(stats.serverErrorRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("비율 계산 시 소수점 2자리까지 반올림된다")
        void shouldRoundRateToTwoDecimalPlaces() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(3)
                    .successCount(1)
                    .build();

            // 1/3 * 100 = 33.333... → 33.33
            assertThat(stats.successRate()).isEqualTo(33.33);
        }

        @Test
        @DisplayName("모든 요청이 성공이면 100%")
        void shouldReturn100PercentWhenAllSuccess() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(50)
                    .successCount(50)
                    .build();

            assertThat(stats.successRate()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Statistics 불변성 검증")
    class StatisticsImmutabilityTest {

        @Test
        @DisplayName("빌더에 전달한 원본 리스트를 수정해도 Statistics 내부 상태는 변하지 않는다")
        void shouldNotBeAffectedByOriginalListModification() {
            var mutableList = new ArrayList<>(List.of(
                    new AnalysisResult.TopItem("/api", 100)));

            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .topPaths(mutableList)
                    .build();

            mutableList.clear();

            assertThat(stats.topPaths()).hasSize(1);
            assertThat(stats.topPaths().get(0).item()).isEqualTo("/api");
        }

        @Test
        @DisplayName("빌더에 전달한 원본 맵을 수정해도 Statistics 내부 상태는 변하지 않는다")
        void shouldNotBeAffectedByOriginalMapModification() {
            var mutableMap = new HashMap<String, Long>();
            mutableMap.put("GET", 50L);

            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .methodStats(mutableMap)
                    .build();

            mutableMap.clear();

            assertThat(stats.methodStats()).hasSize(1);
            assertThat(stats.methodStats().get("GET")).isEqualTo(50L);
        }

        @Test
        @DisplayName("accessor로 반환된 리스트는 수정할 수 없다")
        void shouldReturnUnmodifiableList() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .topPaths(List.of(new AnalysisResult.TopItem("/api", 100)))
                    .build();

            assertThatThrownBy(() -> stats.topPaths().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("accessor로 반환된 맵은 수정할 수 없다")
        void shouldReturnUnmodifiableMap() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .methodStats(Map.of("GET", 50L))
                    .build();

            assertThatThrownBy(() -> stats.methodStats().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("topStatusCodes 리스트도 수정할 수 없다")
        void shouldReturnUnmodifiableStatusCodeList() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .topStatusCodes(List.of(new AnalysisResult.TopItem("200", 80)))
                    .build();

            assertThatThrownBy(() -> stats.topStatusCodes().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("topIps 리스트도 수정할 수 없다")
        void shouldReturnUnmodifiableIpList() {
            var stats = AnalysisResult.Statistics.builder()
                    .totalRequests(100)
                    .topIps(List.of(new AnalysisResult.TopItem("1.2.3.4", 50)))
                    .build();

            assertThatThrownBy(() -> stats.topIps().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("TopItem 검증")
    class TopItemTest {

        @Test
        @DisplayName("정상 값으로 생성된다")
        void shouldCreateWithValidValues() {
            var item = new AnalysisResult.TopItem("/api/users", 100);

            assertThat(item.item()).isEqualTo("/api/users");
            assertThat(item.count()).isEqualTo(100);
        }

        @Test
        @DisplayName("count가 0이면 정상 생성된다")
        void shouldAllowZeroCount() {
            var item = new AnalysisResult.TopItem("/api", 0);

            assertThat(item.count()).isZero();
        }

        @Test
        @DisplayName("item이 null이면 IllegalArgumentException 발생")
        void shouldThrowWhenItemIsNull() {
            assertThatThrownBy(() -> new AnalysisResult.TopItem(null, 100))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("item");
        }

        @Test
        @DisplayName("count가 음수이면 IllegalArgumentException 발생")
        void shouldThrowWhenCountIsNegative() {
            assertThatThrownBy(() -> new AnalysisResult.TopItem("/api", -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("count");
        }

        @Test
        @DisplayName("빈 문자열 item은 허용된다")
        void shouldAllowEmptyStringItem() {
            var item = new AnalysisResult.TopItem("", 10);
            assertThat(item.item()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ParseErrors 검증")
    class ParseErrorsTest {

        @Test
        @DisplayName("empty()는 errorCount 0, 빈 리스트를 반환한다")
        void shouldCreateEmptyParseErrors() {
            var errors = AnalysisResult.ParseErrors.empty();

            assertThat(errors.errorCount()).isZero();
            assertThat(errors.errorSamples()).isEmpty();
        }

        @Test
        @DisplayName("정상 값으로 생성된다")
        void shouldCreateWithValues() {
            var samples = List.of("error1", "error2");
            var errors = new AnalysisResult.ParseErrors(2, samples);

            assertThat(errors.errorCount()).isEqualTo(2);
            assertThat(errors.errorSamples()).containsExactly("error1", "error2");
        }

        @Test
        @DisplayName("errorSamples는 방어적 복사로 불변이다")
        void shouldDefensivelyCopyErrorSamples() {
            var mutableList = new ArrayList<>(List.of("error1"));
            var errors = new AnalysisResult.ParseErrors(1, mutableList);

            mutableList.clear();

            assertThat(errors.errorSamples()).hasSize(1);
            assertThat(errors.errorSamples().get(0)).isEqualTo("error1");
        }

        @Test
        @DisplayName("errorSamples 반환 리스트는 수정할 수 없다")
        void shouldReturnUnmodifiableErrorSamples() {
            var errors = new AnalysisResult.ParseErrors(1, List.of("error1"));

            assertThatThrownBy(() -> errors.errorSamples().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("empty()의 errorSamples도 수정할 수 없다")
        void shouldReturnUnmodifiableEmptyErrorSamples() {
            var errors = AnalysisResult.ParseErrors.empty();

            assertThatThrownBy(() -> errors.errorSamples().add("hack"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
