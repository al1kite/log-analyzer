package com.electricip.loganalyzer.config;

import com.electricip.loganalyzer.infrastructure.client.IpInfoErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Web 설정
 */
@Configuration
public class WebConfig {

    /**
     * RestTemplate Bean
     * — 연결 타임아웃 3초, 읽기 타임아웃 5초
     * — ipinfo API 에러 핸들러 적용
     */
    @Bean
    public RestTemplate restTemplate() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(5_000);

        var restTemplate = new RestTemplate(factory);
        restTemplate.setErrorHandler(new IpInfoErrorHandler());

        return restTemplate;
    }
}
