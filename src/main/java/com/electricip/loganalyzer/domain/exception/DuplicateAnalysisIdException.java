package com.electricip.loganalyzer.domain.exception;

/**
 * 중복된 분석 ID가 존재할 때 발생하는 예외
 */
public class DuplicateAnalysisIdException extends LogAnalyzerException {

    private final String analysisId;

    public DuplicateAnalysisIdException(String analysisId) {
        super("DUPLICATE_ID", "이미 존재하는 분석 ID입니다: " + analysisId);
        this.analysisId = analysisId;
    }

    public String getAnalysisId() {
        return analysisId;
    }
}
