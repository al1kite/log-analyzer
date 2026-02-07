package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.application.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @PostMapping
    @Operation(summary = "로그 파일 분석", description = "CSV 로그 파일을 업로드하여 분석합니다")
    public ResponseEntity<AnalysisIdResponse> analyze(
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
    public ResponseEntity<AnalysisResponse> getResult(@PathVariable String analysisId) {

        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
        
        return analysisService.getById(analysisId)
                .map(AnalysisResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 전체 결과 조회
     */
    @GetMapping
    @Operation(summary = "전체 결과 조회", description = "모든 분석 결과를 조회합니다")
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
    public ResponseEntity<Void> deleteResult(@PathVariable String analysisId) {
        
        Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
        
        var deleted = analysisService.delete(analysisId);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    
    /**
     * 분석 ID 응답 (Record)
     */
    public record AnalysisIdResponse(String analysisId, String message) {

        public AnalysisIdResponse {
            Objects.requireNonNull(analysisId, "analysisId는 null일 수 없습니다");
            Objects.requireNonNull(message, "message는 null일 수 없습니다");
        }
    }
}
