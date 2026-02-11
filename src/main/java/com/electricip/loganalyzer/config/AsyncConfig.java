package com.electricip.loganalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 비동기 처리 설정
 */
@Configuration
public class AsyncConfig {

    @Bean("ipEnrichmentExecutor")
    public Executor ipEnrichmentExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
