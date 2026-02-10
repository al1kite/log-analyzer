package com.electricip.loganalyzer;

import com.electricip.loganalyzer.application.AnalysisService;
import com.electricip.loganalyzer.application.StatisticsCalculator;
import com.electricip.loganalyzer.infrastructure.client.IpInfoClient;
import com.electricip.loganalyzer.infrastructure.parser.CsvLogParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * lombok.config의 copyableAnnotations 설정 검증 테스트.
 *
 * <p>Lombok이 {@code @Value} 어노테이션을 생성자 파라미터로 복사해야
 * Spring이 프로퍼티 값을 주입할 수 있다. {@code lombok.config}가 누락되거나
 * {@code copyableAnnotations} 설정이 빠지면 Spring이 {@code int}/{@code String}
 * 타입의 빈을 찾으려다 {@code UnsatisfiedDependencyException}으로 실패한다.</p>
 *
 * <p>이 테스트가 실패하면 {@code lombok.config} 파일과
 * {@code lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value}
 * 설정을 확인할 것.</p>
 */
@SpringBootTest
class LombokValueInjectionGuardTest {

    @Autowired private CsvLogParser csvLogParser;
    @Autowired private StatisticsCalculator statisticsCalculator;
    @Autowired private AnalysisService analysisService;
    @Autowired private IpInfoClient ipInfoClient;

    @Value("${log-analysis.max-file-lines}") private int maxFileLines;
    @Value("${log-analysis.top-n-results}") private int topN;
    @Value("${log-analysis.max-file-size-mb}") private int maxFileSizeMb;
    @Value("${ipinfo.base-url}") private String ipInfoBaseUrl;

    @Test
    @DisplayName("Spring 컨텍스트 로드 — lombok.config copyableAnnotations 검증")
    void contextLoads_lombokCopyableAnnotationsWork() {
        // lombok.config가 깨지면 @Value가 생성자 파라미터에 복사되지 않아
        // Spring이 int/String 빈을 찾으려다 여기 도달하기 전에 실패
        assertThat(csvLogParser).isNotNull();
        assertThat(statisticsCalculator).isNotNull();
        assertThat(analysisService).isNotNull();
        assertThat(ipInfoClient).isNotNull();
    }

    @Test
    @DisplayName("@Value 프로퍼티가 application.yml 값과 일치한다")
    void valueProperties_matchApplicationConfig() {
        assertThat(maxFileLines).isEqualTo(200_000);
        assertThat(topN).isEqualTo(10);
        assertThat(maxFileSizeMb).isEqualTo(50);
        assertThat(ipInfoBaseUrl).isEqualTo("https://ipinfo.io");
    }
}
