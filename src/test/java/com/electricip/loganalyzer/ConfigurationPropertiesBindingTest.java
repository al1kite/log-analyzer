package com.electricip.loganalyzer;

import com.electricip.loganalyzer.config.IpInfoProperties;
import com.electricip.loganalyzer.config.LogAnalysisProperties;
import com.electricip.loganalyzer.config.RateLimitProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @ConfigurationProperties} 바인딩 검증 테스트.
 *
 * <p>프로퍼티 record가 Spring 컨텍스트에 정상 등록되고,
 * 값이 올바르게 바인딩되는지 확인한다.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "log-analysis.max-file-lines=99999",
        "log-analysis.top-n-results=7",
        "log-analysis.max-file-size-mb=25",
        "log-analysis.ip-enrichment-timeout-seconds=3",
        "ipinfo.base-url=https://test.ipinfo.io",
        "ipinfo.token=test-token",
        "rate-limit.max-requests-per-minute=20",
        "rate-limit.enabled=false",
        "rate-limit.window-seconds=30"
})
class ConfigurationPropertiesBindingTest {

    @Autowired private LogAnalysisProperties logAnalysisProperties;
    @Autowired private IpInfoProperties ipInfoProperties;
    @Autowired private RateLimitProperties rateLimitProperties;

    @Test
    @DisplayName("LogAnalysisProperties가 @TestPropertySource 값으로 바인딩된다")
    void logAnalysisProperties_areBound() {
        assertThat(logAnalysisProperties.maxFileLines()).isEqualTo(99_999);
        assertThat(logAnalysisProperties.topNResults()).isEqualTo(7);
        assertThat(logAnalysisProperties.maxFileSizeMb()).isEqualTo(25);
        assertThat(logAnalysisProperties.ipEnrichmentTimeoutSeconds()).isEqualTo(3);
    }

    @Test
    @DisplayName("IpInfoProperties가 @TestPropertySource 값으로 바인딩된다")
    void ipInfoProperties_areBound() {
        assertThat(ipInfoProperties.baseUrl()).isEqualTo("https://test.ipinfo.io");
        assertThat(ipInfoProperties.token()).isEqualTo("test-token");
    }

    @Test
    @DisplayName("RateLimitProperties가 @TestPropertySource 값으로 바인딩된다")
    void rateLimitProperties_areBound() {
        assertThat(rateLimitProperties.maxRequestsPerMinute()).isEqualTo(20);
        assertThat(rateLimitProperties.enabled()).isFalse();
        assertThat(rateLimitProperties.windowSeconds()).isEqualTo(30);
    }
}
