package com.electricip.loganalyzer.infrastructure.parser;

import com.electricip.loganalyzer.domain.AccessLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * CSV 로그 파서
 */
@Slf4j
@Component
public class CsvLogParser {
    
    private static final int MAX_ERROR_SAMPLES = 5;
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss.SSS a", Locale.ENGLISH);
    
    @Value("${log-analysis.max-file-lines:200000}")
    private int maxFileLines;
    
    /**
     * CSV 파일 파싱
     * 
     * @param inputStream CSV 입력 스트림
     * @return 파싱 결과
     * @throws NullPointerException inputStream이 null인 경우
     * @throws RuntimeException 파싱 실패 시
     */
    public ParseResult parse(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream은 null일 수 없습니다");
        
        var logs = new ArrayList<AccessLog>();
        var errorSamples = new ArrayList<String>();
        var errorCount = 0;
        var lineNumber = 0;
        
        // try-with-resources
        try (var reader = new BufferedReader(
                new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8))) {
            
            var csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();
            
            try (var csvParser = csvFormat.parse(reader)) {
                for (CSVRecord record : csvParser) {
                    lineNumber++;
                    
                    if (lineNumber > maxFileLines) {
                        log.warn("최대 라인 수 도달: {}", maxFileLines);
                        break;
                    }
                    
                    try {
                        var accessLog = parseRecord(record);
                        logs.add(accessLog);
                    } catch (Exception e) {
                        errorCount++;
                        if (errorSamples.size() < MAX_ERROR_SAMPLES) {
                            errorSamples.add(String.format("Line %d: %s", 
                                    lineNumber, e.getMessage()));
                        }
                    }
                }
            }
            
            log.info("파싱 완료: total={}, success={}, errors={}", 
                    lineNumber, logs.size(), errorCount);
            
            return new ParseResult(logs, errorCount, errorSamples);
            
        } catch (Exception e) {
            log.error("CSV 파싱 실패", e);
            throw new RuntimeException("CSV 파일 파싱 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * CSV 레코드를 AccessLog로 변환
     */
    private AccessLog parseRecord(CSVRecord record) {
        return new AccessLog(
                parseDateTime(record.get("TimeGenerated [UTC]")),
                record.get("ClientIp"),
                record.get("HttpMethod"),
                record.get("RequestUri"),
                record.get("UserAgent"),
                parseInteger(record.get("HttpStatus")),
                record.get("HttpVersion"),
                parseLong(record.get("ReceivedBytes")),
                parseLong(record.get("SentBytes")),
                parseDouble(record.get("ClientResponseTime")),
                record.get("SslProtocol"),
                record.get("OriginalRequestUriWithArgs")
        );
    }
    
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value.trim(), FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
    
    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 파싱 결과 (Record)
     */
    public record ParseResult(
            List<AccessLog> logs,
            int errorCount,
            List<String> errorSamples
    ) {
        /**
         * 가변 컬렉션을 불변으로 변환
         */
        public ParseResult {
            logs = List.copyOf(logs);
            errorSamples = List.copyOf(errorSamples);
        }
    }
}
