package com.electricip.loganalyzer.domain.exception;

import lombok.Getter;

import java.util.Objects;

/**
 * 분석 결과를 찾을 수 없을 때 발생하는 예외
 */
@Getter
public class AnalysisNotFoundException extends LogAnalyzerException {

    private final String analysisId;

    public AnalysisNotFoundException(String analysisId) {
        super("NOT_FOUND", "분석 결과를 찾을 수 없습니다: " + analysisId);
        this.analysisId = Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
    }
}
