package com.electricip.loganalyzer.infrastructure.client;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/**
 * ipinfo API 에러 응답 핸들러
 * — HTTP 상태 코드별 구조화된 예외 분류
 */
public class IpInfoErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        var status = response.getStatusCode();

        if (status.value() == 429) {
            throw new RateLimitExceededException("ipinfo API rate limit 초과");
        }
        if (status.value() == 401 || status.value() == 403) {
            throw new IpInfoAuthException("ipinfo API 인증 실패");
        }
        if (status.is5xxServerError()) {
            throw new IpInfoServerException("ipinfo 서버 오류: " + status.value());
        }

        throw new IpInfoException("ipinfo API 오류: " + status.value());
    }
}
