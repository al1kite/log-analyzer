package com.electricip.loganalyzer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger 설정
 */
@Configuration
public class OpenApiConfig {
    
    /**
     * OpenAPI 설정
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Log Analyzer API")
                        .version("1.0.0")
                        .description("""
                                # Log Analyzer API
                                
                                ## 주요 기능
                                - CSV 로그 파싱 (스트리밍)
                                - 통계 분석
                                - ipinfo API 연동
                                - Caffeine 캐싱
                                """));
    }
}
