package com.batchflow.job.hello;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 2: 첫 번째 Job 생성 - Hello Batch
 *
 * 학습 목표:
 * - Job, Step, Tasklet의 기본 구조 이해
 * - JobBuilderFactory, StepBuilderFactory 사용법
 * - Tasklet의 RepeatStatus 반환값 의미
 *
 * 실행 방법:
 * - 테스트 코드를 통한 실행: HelloJobConfigTest 실행
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HelloJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    /**
     * helloJob: 가장 간단한 형태의 Batch Job
     *
     * @return Job - Spring Batch의 최상위 실행 단위
     */
    @Bean
    public Job helloJob() {
        return jobBuilderFactory.get("helloJob")  // Job 이름 지정 (필수)
                .start(helloStep())                // 첫 번째 Step 지정
                .build();
    }

    /**
     * helloStep: 단순 로그 출력을 수행하는 Step
     *
     * Tasklet 방식:
     * - 단순 작업에 적합
     * - execute() 메서드가 한 번 실행됨
     * - RepeatStatus.FINISHED를 반환하면 Step 종료
     *
     * @return Step - Job을 구성하는 독립적인 작업 단위
     */
    @Bean
    public Step helloStep() {
        return stepBuilderFactory.get("helloStep")  // Step 이름 지정 (필수)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Hello, Spring Batch!");
                    log.info(">>>>> Step 실행 완료");
                    return RepeatStatus.FINISHED;  // Tasklet 종료 (한 번만 실행)
                })
                .build();
    }
}
