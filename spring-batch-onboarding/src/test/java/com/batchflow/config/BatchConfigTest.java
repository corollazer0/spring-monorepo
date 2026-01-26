package com.batchflow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BatchConfig 테스트
 *
 * @EnableBatchProcessing이 핵심 Bean들을 제대로 등록하는지 검증합니다.
 */
@SpringBootTest
class BatchConfigTest {

    @Autowired(required = false)
    private JobRepository jobRepository;

    @Autowired(required = false)
    private JobLauncher jobLauncher;

    @Autowired(required = false)
    private JobBuilderFactory jobBuilderFactory;

    @Autowired(required = false)
    private StepBuilderFactory stepBuilderFactory;

    @Test
    @DisplayName("@EnableBatchProcessing - JobRepository Bean 등록 확인")
    void EnableBatchProcessing_JobRepository_정상등록() {
        // then
        assertThat(jobRepository)
                .as("JobRepository는 Job 실행 정보를 저장하는 핵심 Bean입니다")
                .isNotNull();
    }

    @Test
    @DisplayName("@EnableBatchProcessing - JobLauncher Bean 등록 확인")
    void EnableBatchProcessing_JobLauncher_정상등록() {
        // then
        assertThat(jobLauncher)
                .as("JobLauncher는 Job을 실행하는 핵심 Bean입니다")
                .isNotNull();
    }

    @Test
    @DisplayName("@EnableBatchProcessing - JobBuilderFactory Bean 등록 확인")
    void EnableBatchProcessing_JobBuilderFactory_정상등록() {
        // then
        assertThat(jobBuilderFactory)
                .as("JobBuilderFactory는 Job을 생성하는 빌더 팩토리입니다")
                .isNotNull();
    }

    @Test
    @DisplayName("@EnableBatchProcessing - StepBuilderFactory Bean 등록 확인")
    void EnableBatchProcessing_StepBuilderFactory_정상등록() {
        // then
        assertThat(stepBuilderFactory)
                .as("StepBuilderFactory는 Step을 생성하는 빌더 팩토리입니다")
                .isNotNull();
    }

    @Test
    @DisplayName("@EnableBatchProcessing - 모든 핵심 Bean 등록 확인")
    void EnableBatchProcessing_모든핵심Bean_정상등록() {
        // then - @EnableBatchProcessing이 자동으로 등록한 Bean들 확인
        assertThat(jobRepository).isNotNull();
        assertThat(jobLauncher).isNotNull();
        assertThat(jobBuilderFactory).isNotNull();
        assertThat(stepBuilderFactory).isNotNull();
    }
}
