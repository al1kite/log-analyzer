package com.electricip.loganalyzer.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 에러 응답
 */
@Schema(description = "에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 발생 시각", example = "2026-02-09T14:30:00")
        LocalDateTime timestamp,
        @Schema(description = "HTTP 상태 코드", example = "400")
        int status,
        @Schema(description = "에러 코드", example = "INVALID_FILE")
        String errorCode,
        @Schema(description = "에러 메시지", example = "파일이 비어있습니다")
        String message,
        @Schema(description = "요청 경로", example = "/api/analysis")
        String path
) {
    public ErrorResponse {
        Objects.requireNonNull(timestamp, "timestamp는 null일 수 없습니다");
        Objects.requireNonNull(errorCode, "errorCode는 null일 수 없습니다");
        Objects.requireNonNull(path, "path는 null일 수 없습니다");
        message = (message != null) ? message : "알 수 없는 오류";
    }

    public static ErrorResponse of(HttpStatus httpStatus, String errorCode,
                                   String message, String path) {
        return new ErrorResponse(
                LocalDateTime.now(),
                httpStatus.value(),
                errorCode,
                message,
                path
        );
    }
}
