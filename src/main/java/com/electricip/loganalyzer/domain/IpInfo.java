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

    /**
     * Compact Constructor: ip 검증 + null/blank → "UNKNOWN" 치환
     */
    public IpInfo {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("IP는 null이거나 비어있을 수 없습니다");
        }
        country = country != null && !country.isBlank() ? country : "UNKNOWN";
        region = region != null && !region.isBlank() ? region : "UNKNOWN";
        city = city != null && !city.isBlank() ? city : "UNKNOWN";
        organization = organization != null && !organization.isBlank() ? organization : "UNKNOWN";
    }

    public static IpInfo unknown(String ip) {
        return new IpInfo(ip, "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN");
    }

    /**
     * 정적 팩터리 메서드 (compact constructor가 검증/치환 수행)
     */
    public static IpInfo of(String ip, String country, String region,
                            String city, String organization) {
        return new IpInfo(ip, country, region, city, organization);
    }

    /**
     * 유효한 IP 정보인지 확인
     */
    public boolean isValid() {
        return country != null && !"UNKNOWN".equals(country);
    }
}
