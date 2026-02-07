package com.electricip.loganalyzer.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 접속 로그 도메인 모델
 */
public record AccessLog(
    LocalDateTime timeGenerated,
    String clientIp,
    String httpMethod,
    String requestUri,
    String userAgent,
    Integer httpStatus,
    String httpVersion,
    Long receivedBytes,
    Long sentBytes,
    Double clientResponseTime,
    String sslProtocol,
    String originalRequestUriWithArgs
) {
    /**
     * 매개변수 유효성 검사
     */
    public AccessLog {
        // null 허용 (Optional 필드)
    }
    
    /**
     * 비즈니스 로직: 성공 여부
     */
    public boolean isSuccessful() {
        return httpStatus != null && httpStatus >= 200 && httpStatus < 300;
    }
    
    /**
     * 비즈니스 로직: 상태 코드 카테고리
     */
    public String statusCategory() {
        if (httpStatus == null) return "Unknown";
        if (httpStatus >= 200 && httpStatus < 300) return "2xx";
        if (httpStatus >= 300 && httpStatus < 400) return "3xx";
        if (httpStatus >= 400 && httpStatus < 500) return "4xx";
        if (httpStatus >= 500 && httpStatus < 600) return "5xx";
        return "Unknown";
    }
}
