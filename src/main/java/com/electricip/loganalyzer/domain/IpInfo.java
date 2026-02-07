package com.electricip.loganalyzer.domain;

/**
 * IP 정보 Value Object
 */
public record IpInfo(
    String ip,
    String country,
    String region,
    String city,
    String organization
) {

    public static IpInfo unknown(String ip) {
        return new IpInfo(ip, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN");
    }
    
    /**
     * 유효성 검사 포함
     */
    public static IpInfo of(String ip, String country, String region, 
                           String city, String organization) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("IP는 null이거나 비어있을 수 없습니다");
        }
        return new IpInfo(ip, country, region, city, organization);
    }
    
    /**
     * 유효한 IP 정보인지 확인
     */
    public boolean isValid() {
        return country != null && !"UNKNOWN".equals(country);
    }
}
