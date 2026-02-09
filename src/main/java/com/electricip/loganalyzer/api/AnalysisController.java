package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.api.GlobalExceptionHandler.ErrorResponse;
import com.electricip.loganalyzer.application.AnalysisService;
import com.electricip.loganalyzer.domain.exception.AnalysisNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * 로그 분석 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Validated
@Tag(name = "Log Analysis", description = "로그 분석 API")
public class AnalysisController {
    
    private final AnalysisService analysisService;
    
    /**
     * 로그 파일 분석
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "로그 파일 분석", description = "CSV 로그 파일을 업로드하여 분석합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 성공",
                    content = @Content(schema = @Schema(implementation = AnalysisIdResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 파일 또는 CSV 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "파일 크기 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "파싱 에러 과다 (유효한 로그 없음)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalysisIdResponse> analyze(
            @Parameter(description = "분석할 CSV 로그 파일", required = true)
            @RequestParam("file") @NotNull(message = "파일은 필수입니다") MultipartFile file) {
        
        log.info("분석 요청: file={}", file.getOriginalFilename());
        
        var result = analysisService.analyze(file);
        
        return ResponseEntity.ok(new AnalysisIdResponse(
                result.getAnalysisId(),
                "분석이 완료되었습니다"
        ));
    }
    
    /**
     * 분석 결과 조회
     */
    @GetMapping("/{analysisId}")
    @Operation(summary = "분석 결과 조회", description = "분석 ID로 결과를 조회합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = AnalysisResponse.class))),
            @ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalysisResponse> getResult(
            @Parameter(description = "분석 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String analysisId) {

        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");

        var result = analysisService.getById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        return ResponseEntity.ok(AnalysisResponse.from(result));
    }
    
    /**
     * 전체 결과 조회
     */
    @GetMapping
    @Operation(summary = "전체 결과 조회", description = "모든 분석 결과를 조회합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<AnalysisResponse>> getAllResults() {
        
        var results = analysisService.getAll().stream()
                .map(AnalysisResponse::from)
                .toList();
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 결과 삭제
     */
    @DeleteMapping("/{analysisId}")
    @Operation(summary = "결과 삭제", description = "분석 결과를 삭제합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteResult(
            @Parameter(description = "분석 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String analysisId) {
        
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
        
        var deleted = analysisService.delete(analysisId);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * 분석 ID 응답 (Record)
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
}
