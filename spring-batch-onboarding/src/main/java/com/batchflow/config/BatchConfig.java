package com.batchflow.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 기본 설정
 *
 * @EnableBatchProcessing
 * - Spring Batch의 핵심 인프라 Bean들을 자동으로 등록합니다.
 * - JobRepository: Job 실행 정보를 저장하는 저장소
 * - JobLauncher: Job을 실행하는 런처
 * - JobBuilderFactory: Job을 생성하는 빌더 팩토리
 * - StepBuilderFactory: Step을 생성하는 빌더 팩토리
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    // 기본 설정은 @EnableBatchProcessing이 자동으로 처리
    // 필요에 따라 커스텀 설정 추가 가능 (TaskExecutor, DataSource 등)
}
