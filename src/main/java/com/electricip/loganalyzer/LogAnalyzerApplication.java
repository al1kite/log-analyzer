package com.electricip.loganalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Log Analyzer Application
 *
 * ## 적용된 코드 컨벤션
 * 
 * ### 객체 생성과 파괴
 * - 정적 팩터리 메서드 (IpInfo.unknown(), ErrorResponse.of())
 * - 빌더 패턴 (AnalysisResult, Statistics)
 * - 의존 객체 주입 (생성자 주입, @RequiredArgsConstructor)
 * - try-with-resources (CsvLogParser)
 * 
 * ### 모든 객체의 공통 메서드
 * - Thread Safety (ConcurrentHashMap)
 * 
 * ### 클래스와 인터페이스
 * - 불변 클래스 (Record, @Value)
 * 
 * ### 제네릭
 * - 타입 안전성 (제네릭 사용)
 * 
 * ### 메서드
 * - 매개변수 유효성 검사 (Objects.requireNonNull, Bean Validation)
 * - 방어적 복사 (List.copyOf, Collections.unmodifiable*)
 * - null이 아닌 빈 컬렉션 반환 (Collections.emptyList())
 * - Optional 활용 (Optional<AnalysisResult>)
 * 
 * ## 아키텍처
 * ```
 * domain/         순수 도메인 모델 (Record)
 * application/    비즈니스 로직
 * infrastructure/ 기술 구현
 * api/            REST API
 * config/         설정
 * ```
 * 
 * ## 핵심 기술
 * - Java 17 Record (불변성, 간결성)
 * - Lombok (@Value, @Builder, @RequiredArgsConstructor)
 * - Caffeine Cache (비용 절감)
 * - Bean Validation (매개변수 검증)
 * - Streaming CSV Parser (메모리 효율)
 * 
 * @author Electric IP Backend Team
 * @version 1.0.0
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LogAnalyzerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LogAnalyzerApplication.class, args);
    }
}
