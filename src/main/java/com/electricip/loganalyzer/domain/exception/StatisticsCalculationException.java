package com.electricip.loganalyzer.domain.exception;

/**
 * 통계 계산 중 발생하는 예외
 */
public class StatisticsCalculationException extends LogAnalyzerException {

    public StatisticsCalculationException(String message) {
        super("STATISTICS_CALCULATION_ERROR", message);
    }

    public StatisticsCalculationException(String message, Throwable cause) {
        super("STATISTICS_CALCULATION_ERROR", message, cause);
    }
}
