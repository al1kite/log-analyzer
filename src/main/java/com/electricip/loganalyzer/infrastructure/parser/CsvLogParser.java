package com.electricip.loganalyzer.infrastructure.parser;

import com.electricip.loganalyzer.application.LogParser;
import com.electricip.loganalyzer.domain.AccessLog;
import com.electricip.loganalyzer.domain.exception.InvalidCsvFormatException;
import com.electricip.loganalyzer.domain.ParseError;
import com.electricip.loganalyzer.domain.ParseStatistics;
import com.electricip.loganalyzer.config.LogAnalysisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CSV 로그 파서
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CsvLogParser implements LogParser {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("M/d/yyyy, h:mm:ss.SSS a", Locale.ENGLISH);

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "TimeGenerated [UTC]",
            "ClientIp",
            "HttpMethod",
            "RequestUri",
            "UserAgent",
            "HttpStatus",
            "HttpVersion",
            "ReceivedBytes",
            "SentBytes",
            "ClientResponseTime",
            "SslProtocol",
            "OriginalRequestUriWithArgs"
    );

    private final LogAnalysisProperties properties;

    /**
     * CSV 파일 파싱
     *
     * @param inputStream CSV 입력 스트림
     * @return 파싱 결과
     * @throws NullPointerException inputStream이 null인 경우
     * @throws InvalidCsvFormatException 헤더가 올바르지 않거나 파싱 실패 시
     */
    public ParseResult parse(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream은 null일 수 없습니다");

        var logs = new ArrayList<AccessLog>();
        var errorSamples = new ArrayList<ParseError>();
        var errorsByType = new HashMap<String, Integer>();
        var errorCount = 0;
        var lineNumber = 0;

        try (var reader = new BufferedReader(
                new InputStreamReader(BOMInputStream.builder().setInputStream(inputStream).get(), StandardCharsets.UTF_8))) {

            var csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            try (var csvParser = csvFormat.parse(reader)) {
                validateHeaders(csvParser.getHeaderMap());

                for (CSVRecord record : csvParser) {
                    lineNumber++;

                    if (lineNumber > properties.maxFileLines()) {
                        log.warn("최대 라인 수 도달: {}", properties.maxFileLines());
                        break;
                    }

                    try {
                        var accessLog = parseRecord(record);
                        logs.add(accessLog);
                    } catch (Exception e) {
                        errorCount++;
                        var errorType = classifyError(e);
                        errorsByType.merge(errorType.name(), 1, Integer::sum);

                        if (errorSamples.size() < ParseStatistics.MAX_ERROR_SAMPLES) {
                            var message = (e.getMessage() != null)
                                    ? e.getMessage()
                                    : e.getClass().getSimpleName();
                            errorSamples.add(new ParseError(
                                    lineNumber,
                                    message,
                                    errorType,
                                    LocalDateTime.now()
                            ));
                        }
                    }
                }
            }

            log.info("파싱 완료: total={}, success={}, errors={}",
                    lineNumber, logs.size(), errorCount);

            var parseStatistics = new ParseStatistics(
                    lineNumber, logs.size(), errorCount, errorsByType, errorSamples);

            return new ParseResult(logs, parseStatistics);

        } catch (InvalidCsvFormatException e) {
            throw e;
        } catch (Exception e) {
            log.error("CSV 파싱 실패", e);
            var cause = (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName();
            throw new InvalidCsvFormatException("CSV 파일 파싱 실패: " + cause, e);
        }
    }

    /**
     * 헤더 검증 — 대소문자 무시 비교, 누락 시 InvalidCsvFormatException 발생
     */
    private void validateHeaders(Map<String, Integer> headerMap) {
        var actualHeaders = headerMap.keySet().stream()
                .map(h -> h.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        var missingHeaders = REQUIRED_HEADERS.stream()
                .filter(required -> !actualHeaders.contains(required.toLowerCase(Locale.ROOT)))
                .sorted()
                .toList();

        if (!missingHeaders.isEmpty()) {
            throw new InvalidCsvFormatException(
                    "필수 헤더가 누락되었습니다: " + missingHeaders, missingHeaders);
        }
    }

    /**
     * 에러 타입 분류
     */
    private ParseError.ErrorType classifyError(Exception e) {
        if (e instanceof NumberFormatException || e instanceof DateTimeParseException) {
            return ParseError.ErrorType.PARSING;
        }
        if (e instanceof IllegalArgumentException) {
            return ParseError.ErrorType.VALIDATION;
        }
        return ParseError.ErrorType.FORMAT;
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

}
