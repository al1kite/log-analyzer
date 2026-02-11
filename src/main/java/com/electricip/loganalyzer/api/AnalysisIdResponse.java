package com.electricip.loganalyzer.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * 분석 ID 응답
 */
@Schema(description = "분석 완료 응답")
public record AnalysisIdResponse(
        @Schema(description = "분석 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String analysisId,
        @Schema(description = "결과 메시지", example = "분석이 완료되었습니다")
        String message) {

    public AnalysisIdResponse {
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
        Objects.requireNonNull(message, "message는 null일 수 없습니다");
    }
}
