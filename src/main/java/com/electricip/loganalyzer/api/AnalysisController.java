package com.electricip.loganalyzer.api;

import com.electricip.loganalyzer.application.AnalysisService;
import com.electricip.loganalyzer.application.RateLimitService;
import com.electricip.loganalyzer.domain.exception.AnalysisNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

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
    private final RateLimitService rateLimitService;

    /**
     * 로그 파일 분석
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "로그 파일 분석", description = "CSV 로그 파일을 업로드하여 분석합니다")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "분석 성공",
                    headers = @Header(name = "Location", description = "생성된 분석 결과 URI"),
                    content = @Content(schema = @Schema(implementation = AnalysisIdResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 파일 또는 CSV 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "파일 크기 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "파싱 에러 과다 (유효한 로그 없음)",
                    content = @Content(schema = @Schema(implementation = ParsingErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "요청 한도 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AnalysisIdResponse> analyze(
            @Parameter(description = "분석할 CSV 로그 파일", required = true)
            @RequestParam("file") @NotNull(message = "파일은 필수입니다") MultipartFile file,
            HttpServletRequest request) {

        String clientIp = request.getRemoteAddr();
        rateLimitService.checkRateLimit(clientIp);

        var result = analysisService.analyze(file);

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.getAnalysisId())
                .toUri();

        return ResponseEntity.created(location)
                .header("X-RateLimit-Remaining",
                        String.valueOf(rateLimitService.getRemainingRequests(clientIp)))
                .body(new AnalysisIdResponse(
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
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "분석 결과를 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteResult(
            @Parameter(description = "분석 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String analysisId) {

        var deleted = analysisService.delete(analysisId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * 지원 메서드 조회
     */
    @RequestMapping(method = RequestMethod.OPTIONS)
    @Operation(summary = "지원 메서드 조회", description = "이 엔드포인트가 지원하는 HTTP 메서드를 반환합니다")
    @ApiResponse(responseCode = "200", description = "지원 메서드 목록",
            headers = @Header(name = "Allow", description = "지원하는 HTTP 메서드"))
    public ResponseEntity<Void> options() {
        return ResponseEntity.ok()
                .allow(
                        org.springframework.http.HttpMethod.GET,
                        org.springframework.http.HttpMethod.POST,
                        org.springframework.http.HttpMethod.DELETE,
                        org.springframework.http.HttpMethod.OPTIONS
                )
                .build();
    }
}
