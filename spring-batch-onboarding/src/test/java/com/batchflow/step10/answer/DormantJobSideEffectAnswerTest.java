package com.batchflow.step10.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.dormant.DormantMemberJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 10 — answer] DormantJobSideEffectExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 원상복구(@BeforeEach)의 필요성을 이해했는가 — 배치는 진짜 커밋한다
 * - "변하지 말아야 할 것"을 검증했는가 (UPDATE 배치의 안전벨트)
 */
@SpringBatchTest
@SpringBootTest(classes = {DormantMemberJobConfig.class, TestBatchConfig.class})
@DisplayName("휴면 전환의 부수효과 경계 (모범답안)")
class DormantJobSideEffectAnswerTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        // (TODO 1 답) chunk 커밋은 테스트가 끝나도 남는다 — 시드 상태로 직접 복구.
        // 안 하면 앞 테스트의 전환 결과가 다음 테스트의 출발선을 오염시킨다
        jdbcTemplate.update(
                "UPDATE member SET status = 'ACTIVE', dormant_at = NULL " +
                        "WHERE member_id BETWEEN 21 AND 30");
    }

    private JobParameters params(String run) {
        return new JobParametersBuilder()
                .addString("cutoffDate", "2025-06-11")
                .addString("dormantAt", "2026-06-11T03:00:00")
                .addString("run", run)
                .toJobParameters();
    }

    @Test
    @DisplayName("최근 로그인 회원(1~20)은 한 명도 건드리지 않는다")
    void dormantMemberJob_비대상_영향없음() throws Exception {
        // given & when (TODO 2 답)
        jobLauncherTestUtils.launchJob(params("side-effect-1"));

        // then (TODO 3 답) : "오염된 비대상 행"이 0건임을 증명
        Integer touched = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member " +
                        "WHERE member_id BETWEEN 1 AND 20 " +
                        "AND (status <> 'ACTIVE' OR dormant_at IS NOT NULL)",
                Integer.class);
        assertThat(touched).isZero();
    }

    @Test
    @DisplayName("WITHDRAWN(탈퇴) 회원도 그대로다")
    void dormantMemberJob_탈퇴회원_영향없음() throws Exception {
        // given & when (TODO 4 답)
        jobLauncherTestUtils.launchJob(params("side-effect-2"));

        // then (TODO 5 답)
        Integer withdrawn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member WHERE status = 'WITHDRAWN'", Integer.class);
        assertThat(withdrawn).isEqualTo(5);
    }
}
