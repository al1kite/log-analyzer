package com.electricip.loganalyzer.domain.exception;

/**
 * 분석 결과를 찾을 수 없을 때 발생하는 예외
 */
public class AnalysisNotFoundException extends LogAnalyzerException {

    private final String analysisId;

    public AnalysisNotFoundException(String analysisId) {
        super("NOT_FOUND", "분석 결과를 찾을 수 없습니다: " + analysisId);
        this.analysisId = analysisId;
    }

    public String getAnalysisId() {
        return analysisId;
    }
}
