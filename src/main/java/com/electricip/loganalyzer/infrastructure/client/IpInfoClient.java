package com.electricip.loganalyzer.infrastructure.client;

import com.electricip.loganalyzer.domain.IpInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

/**
 * ipinfo.io API 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpInfoClient {
    
    // 의존 객체 주입
    private final RestTemplate restTemplate;
    
    @Value("${ipinfo.base-url:https://ipinfo.io}")
    private String baseUrl;
    
    @Value("${ipinfo.token:#{null}}")
    private String token;
    
    /**
     * IP 정보 조회 (캐싱 적용)
     * 
     * @param ip IP 주소
     * @return IP 정보 (null 반환하지 않음)
     * @throws NullPointerException ip가 null인 경우
     */
    @Cacheable(value = "ipInfoCache", key = "#ip", unless = "#result == null")
    public IpInfo getIpInfo(String ip) {
        Objects.requireNonNull(ip, "ip는 null일 수 없습니다");
        
        if (ip.isBlank()) {
            log.warn("빈 IP 주소");
            return IpInfo.unknown(ip);
        }
        
        try {
            var url = buildUrl(ip);
            var response = restTemplate.getForObject(url, IpInfoResponse.class);
            
            if (response != null) {
                log.debug("IP 정보 조회 성공: ip={}, country={}", ip, response.country);
                
                return IpInfo.of(
                        response.ip,
                        response.country,
                        response.region,
                        response.city,
                        response.org
                );
            }
        } catch (Exception e) {
            log.warn("IP 정보 조회 실패: ip={}, error={}", ip, e.getMessage());
        }
        
        return IpInfo.unknown(ip);
    }
    
    /**
     * API URL 구성
     */
    private String buildUrl(String ip) {
        if (token != null && !token.isBlank()) {
            return String.format("%s/%s?token=%s", baseUrl, ip, token);
        }
        return String.format("%s/%s/json", baseUrl, ip);
    }
    
    /**
     * ipinfo API 응답 DTO (Record)
     */
    private record IpInfoResponse(
            @JsonProperty("ip") String ip,
            @JsonProperty("country") String country,
            @JsonProperty("region") String region,
            @JsonProperty("city") String city,
            @JsonProperty("org") String org
    ) {}
}
