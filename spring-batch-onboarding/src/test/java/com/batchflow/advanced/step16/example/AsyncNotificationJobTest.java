package com.batchflow.advanced.step16.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.async.AsyncNotificationJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
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
 * [심화 Step 16 — example A] 비동기 Job의 정확성 — 빨라져도 장부는 같아야 한다
 *
 * AsyncItemProcessor의 출력은 Future지만, AsyncItemWriter가 쓰기 직전에 풀어주므로
 * StepExecution 카운트(read/write)와 DB 결과는 동기와 완전히 같아야 한다.
 * "성능 개선의 첫 번째 검증은 성능이 아니라 정확성"이다 (Step 14와 같은 순서!).
 *
 * 변경(진짜 INSERT) 테스트 → Before/AfterEach 원상복구 세트 (Step 10의 규약).
 */
@SpringBatchTest
@SpringBootTest(classes = {AsyncNotificationJobConfig.class, TestBatchConfig.class})
@DisplayName("비동기 알림 Job")
class AsyncNotificationJobTest {

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
        jdbcTemplate.update("DELETE FROM notification_history"); // 진짜 커밋된 결과 원상복구
    }

    @Test
    @DisplayName("비동기여도 카운트는 정확하다 — read 15 / write 15 (Future 언래핑 후 집계)")
    void asyncNotificationJob_실행_카운트정확() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then : 장부(StepExecution)가 동기와 똑같다 — 비동기는 구현 디테일일 뿐
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(15);
        assertThat(stepExecution.getWriteCount()).isEqualTo(15);
    }

    @Test
    @DisplayName("결과 내용 검증 — 휴면 회원 15명 전원에게, 휴면 안내 메시지로")
    void asyncNotificationJob_실행_결과내용검증() throws Exception {
        // when
        jobLauncherTestUtils.launchJob();

        // then-1 : 건수
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_history", Integer.class);
        assertThat(total).isEqualTo(15);

        // then-2 : 수신자 전원이 DORMANT 회원인가 (JOIN 교차 검증 — 엉뚱한 사람에게 발송 금지!)
        Integer dormantMatched = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_history n "
                        + "JOIN member m ON n.member_id = m.member_id AND m.status = 'DORMANT'",
                Integer.class);
        assertThat(dormantMatched).isEqualTo(15);

        // then-3 : 메시지 형식
        Integer formatted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_history WHERE message LIKE '휴면 안내:%'",
                Integer.class);
        assertThat(formatted).isEqualTo(15);
    }
}
