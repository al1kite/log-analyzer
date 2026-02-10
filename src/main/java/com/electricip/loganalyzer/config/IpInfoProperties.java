package com.electricip.loganalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * ipinfo API 설정 프로퍼티
 *
 * @param baseUrl ipinfo API 기본 URL
 * @param token   ipinfo API 인증 토큰 (선택)
 */
@ConfigurationProperties(prefix = "ipinfo")
public record IpInfoProperties(
        @DefaultValue("https://ipinfo.io") String baseUrl,
        String token
) {}
