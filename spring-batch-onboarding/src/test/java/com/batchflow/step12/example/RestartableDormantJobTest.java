package com.batchflow.step12.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.restart.RestartableDormantJobConfig;
import com.batchflow.processor.SabotageProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [Batch Step 12 — example] 재시작 — 죽었다 살아나기
 *
 * 시나리오 (운영의 악몽을 통제된 환경에서 재현):
 * 1. 장애 스위치 ON → 실행 → 회원 26에서 사망 (chunk1의 4명만 전환된 채 FAILED)
 * 2. "장애 복구" (스위치 OFF)
 * 3. 🔑 같은 파라미터로 재실행 — FAILED 인스턴스라서 허용된다! (Step 3의 반대편)
 * 4. 남은 6명만 처리하고 COMPLETED — 이미 전환된 4명은 WHERE가 걸러서 안전
 *
 * 그리고 장부: JobInstance는 1개, JobExecution은 2개(FAILED→COMPLETED) — 1:N의 실증.
 */
@SpringBatchTest
@SpringBootTest(classes = {RestartableDormantJobConfig.class, TestBatchConfig.class})
@DisplayName("재시작 가능한 휴면 전환 Job")
class RestartableDormantJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        // 배치는 진짜 커밋한다(Step 10) — 데이터 원상복구
        jdbcTemplate.update("UPDATE member SET status = 'ACTIVE', dormant_at = NULL " +
                "WHERE member_id BETWEEN 21 AND 30");
        SabotageProcessor.SABOTAGE_ON.set(false); // 스위치 기본 OFF
    }

    @AfterEach
    void tearDown() {
        SabotageProcessor.SABOTAGE_ON.set(false); // static 스위치 — 정리는 내 책임 (TestCraft Step 7 교훈)
    }

    private JobParameters fixedParams() {
        // 재시작 검증의 핵심: 두 실행이 "같은 파라미터" = 같은 JobInstance 여야 한다!
        return new JobParametersBuilder()
                .addString("cutoffDate", "2025-06-11")
                .addString("dormantAt", "2026-06-11T03:00:00")
                .toJobParameters();
    }

    @Test
    @DisplayName("장애로 죽은 Job을 같은 파라미터로 재시작하면 남은 것만 처리하고 완주한다")
    void restartableJob_장애후재시작_이어서완주() throws Exception {
        // ── 1막: 장애 발생 — 회원 26에서 사망 ───────────────────────────
        SabotageProcessor.SABOTAGE_ON.set(true);
        JobExecution firstRun = jobLauncherTestUtils.launchJob(fixedParams());

        assertThat(firstRun.getStatus()).isEqualTo(BatchStatus.FAILED);
        // chunk1(21~24)은 이미 커밋되어 살아있다 — chunk 단위 커밋의 가치!
        assertThat(dormantCount()).isEqualTo(19); // 기존 15 + 생존한 4

        // ── 2막: 장애 복구 ──────────────────────────────────────────────
        SabotageProcessor.SABOTAGE_ON.set(false);

        // ── 3막: 같은 파라미터로 재시작 — FAILED 인스턴스니까 허용된다! ──
        JobExecution restartRun = jobLauncherTestUtils.launchJob(fixedParams());

        assertThat(restartRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // 남은 6명(25~30)만 읽었다 — 이미 전환된 21~24는 WHERE(ACTIVE)가 걸렀다
        assertThat(restartRun.getStepExecutions().iterator().next().getReadCount()).isEqualTo(6);
        assertThat(dormantCount()).isEqualTo(25); // 전원 전환 완료

        // ── 증거: 두 실행은 "같은 JobInstance"의 1차/2차 시도다 ─────────
        assertThat(restartRun.getJobInstance().getId())
                .isEqualTo(firstRun.getJobInstance().getId());
    }

    @Test
    @DisplayName("성공으로 끝난 인스턴스는 재시작할 수 없다 (Step 3의 규칙 회수)")
    void restartableJob_성공후재실행_거부() throws Exception {
        // given : 장애 없이 완주시킨다
        jobLauncherTestUtils.launchJob(fixedParams());

        // when & then : COMPLETED 인스턴스의 같은 파라미터 재실행 — 이건 재시작이 아니라 중복!
        assertThatThrownBy(() -> jobLauncherTestUtils.launchJob(fixedParams()))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    private Integer dormantCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member WHERE status = 'DORMANT'", Integer.class);
    }
}
