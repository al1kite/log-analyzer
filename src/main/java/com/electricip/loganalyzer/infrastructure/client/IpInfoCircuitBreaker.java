package com.electricip.loganalyzer.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ipinfo API Circuit Breaker
 * — 연속 실패 시 호출 차단, 타임아웃 후 half-open 복구
 */
@Slf4j
@Component
public class IpInfoCircuitBreaker {

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    static final int FAILURE_THRESHOLD = 5;
    static final long TIMEOUT_MS = 60_000; // 1분

    /**
     * Circuit이 열려있는지 확인
     * — 실패 횟수가 임계치 미만이면 닫힘 (정상)
     * — 타임아웃 이후면 half-open (재시도 허용, 카운터 리셋)
     */
    public boolean isOpen() {
        int currentFailures = failureCount.get();
        if (currentFailures < FAILURE_THRESHOLD) {
            return false;
        }

        // 타임아웃 지났으면 half-open → CAS로 단일 스레드만 리셋
        if (System.currentTimeMillis() - lastFailureTime.get() > TIMEOUT_MS) {
            if (failureCount.compareAndSet(currentFailures, 0)) {
                log.info("Circuit Breaker half-open: 재시도 허용");
                return false;
            }
            // 다른 스레드가 이미 리셋 → 현재 상태로 판단
            return failureCount.get() >= FAILURE_THRESHOLD;
        }

        return true;
    }

    /**
     * 성공 기록 — 카운터 리셋
     */
    public void recordSuccess() {
        failureCount.set(0);
    }

    /**
     * 실패 기록 — 카운터 증가 + 시각 갱신
     */
    public void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        failureCount.incrementAndGet();
    }

    /**
     * 현재 실패 횟수 (테스트용)
     */
    int getFailureCount() {
        return failureCount.get();
    }
}
