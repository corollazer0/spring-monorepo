package com.batchflow.step01.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 1 — example] 배치 인프라: @EnableBatchProcessing이 깔아주는 판
 *
 * 문제 상황: 배치를 시작하려는데 "Job을 실행하면 그 기록은 어디에 남지?",
 * "실패하면 어디서부터 다시 하지?"라는 질문에 답할 장치가 아무것도 없다.
 *
 * 해결: @EnableBatchProcessing 한 줄이 두 가지를 깔아준다.
 * 1. 핵심 Bean 4종 — JobRepository(기록 저장소), JobLauncher(실행기),
 *    JobBuilderFactory/StepBuilderFactory(조립 공장)
 * 2. 메타데이터 테이블 6종(BATCH_*) — 모든 실행 기록의 장부
 *    (initialize-schema: embedded 설정으로 H2에 자동 생성)
 *
 * 이 테스트는 "판이 제대로 깔렸는가"를 검증한다 — 모든 Step의 전제 조건이다.
 */
@SpringBootTest
@DisplayName("배치 인프라 (@EnableBatchProcessing)")
class BatchInfrastructureTest {

    @Autowired(required = false)
    private JobRepository jobRepository;

    @Autowired(required = false)
    private JobLauncher jobLauncher;

    @Autowired(required = false)
    private JobBuilderFactory jobBuilderFactory;

    @Autowired(required = false)
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("핵심 Bean 4종")
    class CoreBeans {

        @Test
        @DisplayName("@EnableBatchProcessing이 핵심 Bean을 모두 등록한다")
        void enableBatchProcessing_핵심빈4종_정상등록() {
            assertThat(jobRepository).as("JobRepository: Job 실행 기록의 저장소").isNotNull();
            assertThat(jobLauncher).as("JobLauncher: Job을 실행하는 진입점").isNotNull();
            assertThat(jobBuilderFactory).as("JobBuilderFactory: Job 조립 공장 (Batch 4.x)").isNotNull();
            assertThat(stepBuilderFactory).as("StepBuilderFactory: Step 조립 공장 (Batch 4.x)").isNotNull();
        }
    }

    @Nested
    @DisplayName("메타데이터 테이블 — 모든 실행 기록의 장부")
    class MetadataTables {

        /**
         * Spring Batch는 "무엇을 언제 어떤 파라미터로 실행했고 결과가 어땠는지"를
         * 전부 DB에 남긴다. 재시작(Step 12), 중복 실행 방지(Step 3)가 모두
         * 이 장부 위에서 동작한다 — 배치의 신뢰성은 메타데이터에서 나온다.
         */
        @Test
        @DisplayName("BATCH_* 메타데이터 테이블 6종이 자동 생성된다")
        void initializeSchema_메타테이블6종_자동생성() {
            // when : H2의 INFORMATION_SCHEMA로 실제 생성된 테이블 목록을 조회
            List<String> batchTables = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE 'BATCH_%'",
                    String.class);

            // then : 6종이 모두 존재한다 (+ 시퀀스용 보조 테이블이 더 있을 수 있어 contains로 검증)
            assertThat(batchTables).contains(
                    "BATCH_JOB_INSTANCE",            // Job + 파라미터의 논리적 실행 단위
                    "BATCH_JOB_EXECUTION",           // 실행 시도 (1 인스턴스 : N 실행)
                    "BATCH_JOB_EXECUTION_PARAMS",    // 실행 파라미터
                    "BATCH_STEP_EXECUTION",          // Step별 실행 통계 (read/write/skip count)
                    "BATCH_JOB_EXECUTION_CONTEXT",   // Job 수준 ExecutionContext
                    "BATCH_STEP_EXECUTION_CONTEXT"); // Step 수준 ExecutionContext
        }

        @Test
        @DisplayName("BATCH_STEP_EXECUTION에는 처리 통계 컬럼들이 있다 (이후 Step들의 검증 무기)")
        void stepExecution_통계컬럼_존재확인() {
            // when
            List<String> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_NAME = 'BATCH_STEP_EXECUTION'",
                    String.class);

            // then : Chunk 처리(Step 6~)에서 "몇 건 읽고/걸러내고/썼는지"를 검증할 때 쓰는 컬럼들
            assertThat(columns).contains(
                    "READ_COUNT", "WRITE_COUNT", "FILTER_COUNT",
                    "READ_SKIP_COUNT", "WRITE_SKIP_COUNT", "PROCESS_SKIP_COUNT",
                    "ROLLBACK_COUNT", "COMMIT_COUNT");
        }
    }
}
