package com.electricip.loganalyzer.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 파싱 에러 과다 응답 (422)
 */
@Schema(description = "파싱 에러 과다 응답 (422)")
public record ParsingErrorResponse(
        @Schema(description = "에러 발생 시각", example = "2026-02-09T14:30:00")
        LocalDateTime timestamp,
        @Schema(description = "HTTP 상태 코드", example = "422")
        int status,
        @Schema(description = "에러 코드", example = "TOO_MANY_PARSING_ERRORS")
        String errorCode,
        @Schema(description = "에러 메시지", example = "유효한 로그가 없습니다 (전체 100줄 중 100줄 에러)")
        String message,
        @Schema(description = "요청 경로", example = "/api/analysis")
        String path,
        @Schema(description = "전체 라인 수", example = "100")
        long totalLines,
        @Schema(description = "에러 라인 수", example = "100")
        long errorCount,
        @Schema(description = "에러 샘플 (최대 10건)")
        List<ParseErrorSample> errorSamples
) {
    public ParsingErrorResponse {
        Objects.requireNonNull(timestamp, "timestamp는 null일 수 없습니다");
        Objects.requireNonNull(errorCode, "errorCode는 null일 수 없습니다");
        Objects.requireNonNull(path, "path는 null일 수 없습니다");
        message = (message != null) ? message : "알 수 없는 오류";
        errorSamples = (errorSamples != null) ? List.copyOf(errorSamples) : List.of();
    }

    public static ParsingErrorResponse of(String errorCode, String message, String path,
                                           long totalLines, long errorCount,
                                           List<ParseErrorSample> errorSamples) {
        return new ParsingErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                errorCode,
                message,
                path,
                totalLines,
                errorCount,
                errorSamples
        );
    }

    @Schema(description = "파싱 에러 샘플")
    public record ParseErrorSample(
            @Schema(description = "에러 발생 라인 번호", example = "42")
            long lineNumber,
            @Schema(description = "에러 메시지", example = "잘못된 날짜 형식")
            String errorMessage,
            @Schema(description = "에러 유형 (PARSING, VALIDATION, FORMAT)", example = "PARSING")
            String errorType
    ) {}
}
