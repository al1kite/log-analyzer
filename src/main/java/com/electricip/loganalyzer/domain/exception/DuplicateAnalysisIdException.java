package com.electricip.loganalyzer.domain.exception;

import lombok.Getter;

import java.util.Objects;

/**
 * 중복된 분석 ID가 존재할 때 발생하는 예외
 */
@Getter
public class DuplicateAnalysisIdException extends LogAnalyzerException {

    private final String analysisId;

    public DuplicateAnalysisIdException(String analysisId) {
        super("DUPLICATE_ID", "이미 존재하는 분석 ID입니다: " + analysisId);
        this.analysisId = Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
    }
}
