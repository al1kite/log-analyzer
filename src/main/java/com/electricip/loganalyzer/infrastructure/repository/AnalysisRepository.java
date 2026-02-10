package com.electricip.loganalyzer.infrastructure.repository;

import com.electricip.loganalyzer.domain.AnalysisResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 분석 결과 저장소
 * — Caffeine Cache 기반: 최대 1000개, TTL 24시간, 미사용 12시간 후 제거
 */
@Slf4j
@Repository
public class AnalysisRepository {

    private final Cache<String, AnalysisResult> storage;

    public AnalysisRepository() {
        this.storage = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .expireAfterAccess(12, TimeUnit.HOURS)
                .removalListener((String key, AnalysisResult value, RemovalCause cause) -> {
                    if (cause.wasEvicted()) {
                        log.debug("분석 결과 자동 제거: id={}, reason={}", key, cause);
                    } else {
                        log.info("분석 결과 제거: id={}, reason={}", key, cause);
                    }
                })
                .recordStats()
                .build();
    }

    /**
     * 테스트용 생성자 — 외부에서 Cache 주입
     */
    AnalysisRepository(Cache<String, AnalysisResult> storage) {
        this.storage = storage;
    }

    /**
     * 저장
     */
    public void save(AnalysisResult result) {
        Objects.requireNonNull(result, "result는 null일 수 없습니다");

        storage.put(result.getAnalysisId(), result);

        log.debug("저장 완료: id={}, cacheSize={}", result.getAnalysisId(), storage.estimatedSize());
    }

    /**
     * ID로 조회
     */
    public Optional<AnalysisResult> findById(String analysisId) {
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");

        return Optional.ofNullable(storage.getIfPresent(analysisId));
    }

    /**
     * 전체 조회
     */
    public List<AnalysisResult> findAll() {
        return List.copyOf(storage.asMap().values());
    }

    /**
     * 삭제
     */
    public boolean deleteById(String analysisId) {
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");

        return storage.asMap().remove(analysisId) != null;
    }

    /**
     * 개수 조회
     */
    public int count() {
        return storage.asMap().size();
    }

    /**
     * 전체 삭제 (테스트용)
     */
    public void deleteAll() {
        storage.invalidateAll();
    }
}
