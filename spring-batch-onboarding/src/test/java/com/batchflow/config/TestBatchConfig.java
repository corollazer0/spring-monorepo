package com.batchflow.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 테스트 전용 Batch 설정
 *
 * 역할:
 * - 테스트 환경에서 Spring Batch 기능 활성화
 * - @EnableBatchProcessing으로 필수 Bean들 자동 등록
 * - @EnableAutoConfiguration으로 테스트 환경 자동 구성
 *
 * 사용 위치:
 * - @SpringBootTest(classes = {XxxJobConfig.class, TestBatchConfig.class})
 */
@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class TestBatchConfig {
    // 테스트 환경에 필요한 기본 배치 설정
    // 별도의 Bean 정의 없이 어노테이션만으로 충분
}
