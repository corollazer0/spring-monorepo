package com.batchflow.advanced.step18.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.massnotify.MarketingNotificationSender;
import com.batchflow.job.massnotify.MassNotificationJobConfig;
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
 * [심화 Step 18 — warmup exercise] 캡스톤 준비 운동 — 첫 발송을 봉인해보세요
 *
 * 진행 방법:
 * 1. FOR-BatchFlow-Step18-Requirements.md의 요구사항·체크리스트를 먼저 읽는다
 * 2. 클래스 위의 @Disabled 를 지우고 TODO를 채운다
 * 3. 이 warmup이 돌면, answer를 "보기 전에" 나머지 시나리오
 *    (재실행 멱등 / 부분 실패 skip / 실패 복구 / skip 한도)를 직접 설계·작성한다
 *
 * 힌트: campaign은 JobParameters로 — JobParametersBuilder.addString("campaign", ...)
 *       + 유니크 run 파라미터 (Step 3의 JobInstance 규칙!)
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step18-Requirements.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {MassNotificationJobConfig.class, TestBatchConfig.class})
@DisplayName("대량 알림 발송 (준비 운동)")
class MassNotificationWarmupExerciseTest {

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
        MarketingNotificationSender.FAIL_MEMBER_IDS.clear();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM notification_history");
        MarketingNotificationSender.FAIL_MEMBER_IDS.clear();
    }

    @Test
    @DisplayName("첫 캠페인 발송: ACTIVE 30명 전원에게, 정확히 한 번씩")
    void massNotificationJob_첫발송_30건() throws Exception {
        // when : ① campaign=WELCOME + 유니크 run 파라미터로 Job을 실행하세요
        // TODO 1

        // then : ② COMPLETED 인지 검증하세요
        // TODO 2

        // then : ③ notification_history가 30건인지 검증하세요
        // TODO 3

        // then : ④ 수신자 전원이 ACTIVE인지 JOIN으로 교차 검증하세요
        //         (DORMANT/WITHDRAWN에게 마케팅이 가면 — 발송 사고다!)
        // TODO 4
    }
}
