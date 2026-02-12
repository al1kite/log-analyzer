package com.electricip.loganalyzer.application;

import com.electricip.loganalyzer.domain.AccessLog;
import com.electricip.loganalyzer.domain.ParseStatistics;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * 로그 파서 인터페이스
 */
public interface LogParser {

    ParseResult parse(InputStream inputStream);

    record ParseResult(
            List<AccessLog> logs,
            ParseStatistics parseStatistics
    ) {
        public ParseResult {
            logs = List.copyOf(logs);
            Objects.requireNonNull(parseStatistics, "parseStatistics는 null일 수 없습니다");
        }
    }
}
