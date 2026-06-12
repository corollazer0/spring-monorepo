package com.batchflow.advanced.step16.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.async.AsyncNotificationJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * [심화 Step 16 — exercise] 비동기 Job을 직접 봉인해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 AsyncNotificationJobTest)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 질문: 비동기 Step에서 "중복 발송"이 생긴다면 어디서 의심해야 할까?
 * (힌트: writeCount와 DB COUNT가 어긋나는 순간이 그 증거다 — 그래서 둘 다 잰다)
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step16.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {AsyncNotificationJobConfig.class, TestBatchConfig.class})
@DisplayName("비동기 알림 Job (연습문제)")
class AsyncNotificationExerciseTest {

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
        // when : ① launchJob()을 "두 번" 실행하세요
        //         (힌트: JobLauncherTestUtils.launchJob()은 매번 유니크 파라미터를 만들어준다
        //          — 같은 파라미터였다면 Step 3에서 배운 그 예외가 터졌을 것!)
        // TODO 1

        // then : ② 두 번째 실행의 StepExecution에서 read/write가 각각 15인지 검증하세요
        // TODO 2

        // then : ③ DB의 notification_history가 총 30건인지 검증하세요
        //         (장부 따로 DB 따로 — 둘이 함께 맞아야 "정확"이다)
        // TODO 3
    }
}
