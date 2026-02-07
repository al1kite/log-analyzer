package com.electricip.loganalyzer.infrastructure.repository;

import com.electricip.loganalyzer.domain.AnalysisResult;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 분석 결과 저장소
 * 
 */
@Repository
public class AnalysisRepository {
    
    // Thread Safety
    private final ConcurrentHashMap<String, AnalysisResult> storage = new ConcurrentHashMap<>();
    
    /**
     * 저장
     * 
     * @param result 분석 결과
     * @throws NullPointerException result가 null인 경우
     */
    public void save(AnalysisResult result) {
        // 매개변수 유효성 검사
        Objects.requireNonNull(result, "result는 null일 수 없습니다");
        Objects.requireNonNull(result.getAnalysisId(), "analysisId는 null일 수 없습니다");
        
        storage.put(result.getAnalysisId(), result);
    }
    
    /**
     * ID로 조회
     * 
     * Optional 활용
     * 
     * @param analysisId 분석 ID
     * @return 분석 결과 Optional
     * @throws NullPointerException analysisId가 null인 경우
     */
    public Optional<AnalysisResult> findById(String analysisId) {
        // 매개변수 유효성 검사
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
        
        return Optional.ofNullable(storage.get(analysisId));
    }
    
    /**
     * 전체 조회
     * 
     * 빈 컬렉션 반환 (null 반환하지 않음)
     * 방어적 복사 (새로운 리스트 반환)
     * 
     * @return 분석 결과 리스트 (비어있을 수 있음)
     */
    public List<AnalysisResult> findAll() {
        if (storage.isEmpty()) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(storage.values());
    }
    
    /**
     * 삭제
     * 
     * @param analysisId 분석 ID
     * @return 삭제 성공 여부
     * @throws NullPointerException analysisId가 null인 경우
     */
    public boolean deleteById(String analysisId) {
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
        
        return storage.remove(analysisId) != null;
    }
    
    /**
     * 개수 조회
     * 
     * @return 저장된 결과 개수
     */
    public int count() {
        return storage.size();
    }
    
    /**
     * 전체 삭제 (테스트용)
     */
    public void deleteAll() {
        storage.clear();
    }
}
