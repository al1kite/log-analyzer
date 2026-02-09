package com.electricip.loganalyzer.infrastructure.client;

import com.electricip.loganalyzer.domain.IpInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

/**
 * ipinfo.io API 클라이언트
 * — 타임아웃, Retry (exponential backoff), Circuit Breaker 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpInfoClient {

    private final RestTemplate restTemplate;
    private final IpInfoCircuitBreaker circuitBreaker;

    private static final int MAX_ATTEMPTS = 3;
    private static final long BACKOFF_MS = 1_000;

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
    @Cacheable(value = "ipInfoCache", key = "#ip", unless = "#result == null || !#result.isValid()")
    public IpInfo getIpInfo(String ip) {
        Objects.requireNonNull(ip, "ip는 null일 수 없습니다");

        if (ip.isBlank()) {
            throw new IllegalArgumentException("IP는 비어있을 수 없습니다");
        }

        // Circuit Open → fallback
        if (circuitBreaker.isOpen()) {
            log.warn("Circuit Breaker open, fallback 사용: ip={}", ip);
            return IpInfo.unknown(ip);
        }

        return callWithRetry(ip);
    }

    /**
     * 재시도 로직 (exponential backoff)
     */
    private IpInfo callWithRetry(String ip) {
        for (int attempt = 1; ; attempt++) {
            try {
                var result = callApi(ip);
                if (result.isValid()) {
                    circuitBreaker.recordSuccess();
                }
                return result;

            } catch (RateLimitExceededException e) {
                log.error("ipinfo rate limit 초과: ip={}", ip);
                circuitBreaker.recordFailure();
                return IpInfo.unknown(ip);

            } catch (IpInfoAuthException e) {
                log.error("ipinfo 인증 실패: ip={}", ip);
                circuitBreaker.recordFailure();
                return IpInfo.unknown(ip);

            } catch (Exception e) {
                if (attempt >= MAX_ATTEMPTS) {
                    circuitBreaker.recordFailure();
                    log.error("ipinfo API 최종 실패: ip={}, attempts={}", ip, MAX_ATTEMPTS, e);
                    return IpInfo.unknown(ip);
                }

                log.warn("ipinfo API 재시도: ip={}, attempt={}/{}, error={}",
                        ip, attempt, MAX_ATTEMPTS, e.getMessage());

                if (sleep(BACKOFF_MS * attempt)) {
                    log.warn("ipinfo API 재시도 중단 (인터럽트): ip={}", ip);
                    circuitBreaker.recordFailure();
                    return IpInfo.unknown(ip);
                }
            }
        }
    }

    /**
     * API 호출
     */
    private IpInfo callApi(String ip) {
        var url = buildUrl(ip);
        var response = restTemplate.getForObject(url, IpInfoResponse.class);

        if (response != null) {
            log.debug("IP 정보 조회 성공: ip={}, country={}", ip, response.country);

            var resolvedIp = (response.ip != null && !response.ip.isBlank()) ? response.ip : ip;
            return IpInfo.of(
                    resolvedIp,
                    response.country,
                    response.region,
                    response.city,
                    response.org
            );
        }

        return IpInfo.unknown(ip);
    }

    /**
     * API URL 구성
     */
    private String buildUrl(String ip) {
        var builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .pathSegment(ip, "json");

        if (token != null && !token.isBlank()) {
            builder.queryParam("token", token);
        }

        return builder.build().toUriString();
    }

    private boolean sleep(long ms) {
        try {
            Thread.sleep(ms);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
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
