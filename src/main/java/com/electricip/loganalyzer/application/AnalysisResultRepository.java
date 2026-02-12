package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.domain.AnalysisResult;

import java.util.List;
import java.util.Optional;

/**
 * 분석 결과 저장소 인터페이스
 */
public interface AnalysisResultRepository {

    void save(AnalysisResult result);

    Optional<AnalysisResult> findById(String analysisId);

    List<AnalysisResult> findAll();

    boolean deleteById(String analysisId);

    long count();
}
