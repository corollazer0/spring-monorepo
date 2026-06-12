package com.batchflow.advanced.step16.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.async.AsyncNotificationJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 16 — answer] AsyncNotificationExerciseTest 모범답안
 *
 * 채점 포인트: 장부(StepExecution)와 DB(COUNT)를 "둘 다" 검증했는가 —
 * 비동기에서 어긋남이 생기면 그 간극이 첫 번째 단서다.
 */
@SpringBatchTest
@SpringBootTest(classes = {AsyncNotificationJobConfig.class, TestBatchConfig.class})
@DisplayName("비동기 알림 Job (모범답안)")
class AsyncNotificationAnswerTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        jdbcTemplate.update("DELETE FROM notification_history");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM notification_history");
    }

    @Test
    @DisplayName("두 번 실행해도 장부와 DB가 함께 정확하다 (실행마다 15건씩, 합 30건)")
    void asyncNotificationJob_두번실행_장부와DB일치() throws Exception {
        // when (TODO 1 답) : launchJob()은 매번 유니크 파라미터 — 재실행 거부가 없다
        jobLauncherTestUtils.launchJob();
        JobExecution second = jobLauncherTestUtils.launchJob();

        // then (TODO 2 답) : 두 번째 실행의 장부도 정확히 15/15
        StepExecution stepExecution = second.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(15);
        assertThat(stepExecution.getWriteCount()).isEqualTo(15);

        // then (TODO 3 답) : DB는 누적 30건 — 장부와 DB가 함께 맞아야 "정확"이다
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_history", Integer.class);
        assertThat(total).isEqualTo(30);
    }
}
