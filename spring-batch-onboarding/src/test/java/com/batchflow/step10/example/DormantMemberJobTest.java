package com.batchflow.step10.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.dormant.DormantMemberJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 10 — example] 휴면회원 전환 Job — 부품의 조립과 진짜 커밋
 *
 * 드디어 DB를 "진짜로" 바꾼다. 그래서 새 숙제가 생긴다:
 * 🚨 Chunk는 진짜 커밋한다 — 테스트가 끝나도 변경이 남는다 (TestCraft의 롤백 없음)!
 * → @BeforeEach에서 데이터를 직접 원상복구한다 (RANDOM_PORT E2E의 @Sql 정리와 같은 철학)
 *
 * 검증 3단:
 * 1. 카운트 (read/filter/write)
 * 2. DB 상태 (정말 DORMANT가 됐는가, 전환 시각까지)
 * 3. 자연 멱등성 (재실행하면 후보가 0명 — WHERE가 상태 전이를 따라간다)
 */
@SpringBatchTest
@SpringBootTest(classes = {DormantMemberJobConfig.class, TestBatchConfig.class})
@DisplayName("휴면회원 전환 Job (통합)")
class DormantMemberJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        // 🚨 배치는 진짜 커밋한다 — 이전 테스트의 전환 결과를 원상복구 (시드 상태로)
        jdbcTemplate.update(
                "UPDATE member SET status = 'ACTIVE', dormant_at = NULL " +
                        "WHERE member_id BETWEEN 21 AND 30");
    }

    private JobParameters params(String suffix) {
        return new JobParametersBuilder()
                .addString("cutoffDate", "2025-06-11")
                .addString("dormantAt", "2026-06-11T03:00:00")
                .addString("run", suffix) // 테스트 간 JobInstance 분리용
                .toJobParameters();
    }

    @Test
    @DisplayName("후보 10명이 전부 DORMANT로 전환되고, 전환 시각까지 기록된다")
    void dormantMemberJob_실행_10명전환() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params("first"));

        // then(1) : 카운트 — 검증 프로세서가 거른 것 없음(리더 WHERE가 이미 정확하므로)
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(10);
        assertThat(stepExecution.getFilterCount()).isEqualTo(0);
        assertThat(stepExecution.getWriteCount()).isEqualTo(10);

        // then(2) : DB 상태 — 카운트는 "썼다"까지만 증명한다. "맞게 썼는가"는 재조회로!
        Integer dormantTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member WHERE status = 'DORMANT'", Integer.class);
        assertThat(dormantTotal).isEqualTo(25); // 기존 15 + 신규 10

        LocalDateTime dormantAt = jdbcTemplate.queryForObject(
                "SELECT dormant_at FROM member WHERE member_id = 21", LocalDateTime.class);
        assertThat(dormantAt).isEqualTo(LocalDateTime.of(2026, 6, 11, 3, 0, 0));
    }

    @Test
    @DisplayName("전환 후 재실행하면 후보 0명 — WHERE가 만든 자연 멱등성")
    void dormantMemberJob_재실행_자연멱등() throws Exception {
        // given : 1차 실행으로 전부 전환
        jobLauncherTestUtils.launchJob(params("first"));

        // when : 다른 파라미터로 한 번 더 (운영의 "실수로 두 번 돌림" 시뮬레이션)
        JobExecution second = jobLauncherTestUtils.launchJob(params("second"));

        // then : 이미 DORMANT라 WHERE(status='ACTIVE')에 안 걸린다 — 읽을 게 없다.
        //        "상태 전이를 따라가는 WHERE"가 이중 전환 사고를 구조적으로 막는다
        StepExecution stepExecution = second.getStepExecutions().iterator().next();
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getReadCount()).isEqualTo(0);

        Integer dormantTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member WHERE status = 'DORMANT'", Integer.class);
        assertThat(dormantTotal).isEqualTo(25); // 그대로!
    }
}
