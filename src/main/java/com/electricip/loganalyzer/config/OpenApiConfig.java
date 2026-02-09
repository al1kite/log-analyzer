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
                                ## CSV 형식의 접속 로그를 분석해 요약 통계를 생성하고, IP 정보를 결합해 리포트를 제공하는 애플리케이션

                                ### 주요 기능
                                - CSV 로그 파싱 (스트리밍)
                                - 통계 분석
                                - ipinfo API 연동
                                - Caffeine 캐싱

                                ### 에러 코드

                                | 에러 코드 | HTTP 상태 | 설명 |
                                |-----------|-----------|------|
                                | `INVALID_CSV_FORMAT` | 400 | CSV 헤더 누락 등 형식 오류 |
                                | `INVALID_FILE` | 400 | 빈 파일, 지원하지 않는 확장자 |
                                | `LOG_PARSING_ERROR` | 400 | 로그 파싱 실패 |
                                | `TOO_MANY_PARSING_ERRORS` | 422 | 파싱 에러 과다 (유효한 로그 없음) |
                                | `FILE_TOO_LARGE` | 413 | 파일 크기 초과 |
                                | `NOT_FOUND` | 404 | 분석 결과 미발견 |
                                | `DUPLICATE_ID` | 409 | 중복 분석 ID |
                                | `RATE_LIMIT_EXCEEDED` | 429 | ipinfo API rate limit 초과 |
                                | `IPINFO_AUTH_ERROR` | 502 | ipinfo API 인증 실패 |
                                | `IPINFO_SERVER_ERROR` | 502 | ipinfo 서버 오류 |
                                | `IPINFO_ERROR` | 502 | ipinfo API 기타 오류 |
                                | `INTERNAL_ERROR` | 500 | 서버 내부 오류 |
                                """));
    }
}
