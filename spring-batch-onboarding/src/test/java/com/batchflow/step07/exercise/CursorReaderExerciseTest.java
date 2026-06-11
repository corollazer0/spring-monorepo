package com.batchflow.step07.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.dormant.DormantCandidateScanJobConfig;
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
 * [Batch Step 7 — exercise] 리더의 결과를 SQL로 교차 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 DormantCandidateScanJobTest를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 취지: "리더가 10명을 읽었다"는 주장을 리더 자신이 아닌 **독립적인 SQL**로
 * 교차 검증한다 — 같은 조건의 COUNT(*)와 READ_COUNT가 일치해야 한다.
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step07.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {DormantCandidateScanJobConfig.class, TestBatchConfig.class})
@DisplayName("커서 리더 교차 검증 (연습문제)")
class CursorReaderExerciseTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("리더의 READ_COUNT는 같은 조건의 SQL COUNT와 일치한다")
    void cursorReader_READCOUNT_SQL교차검증() throws Exception {
        // given : 기준일 2025-06-11로 Job을 실행하고 READ_COUNT를 꺼내세요
        // TODO 1

        // when : 같은 조건(status='ACTIVE' AND last_login_at < '2025-06-11')의
        //        COUNT(*)를 JdbcTemplate로 직접 조회하세요
        // TODO 2

        // then : 두 값이 같은지 검증하세요 — 리더의 WHERE가 의도와 일치한다는 증명!
        // TODO 3
    }

    @Test
    @DisplayName("경계값: 기준일을 정확히 2024-01-15로 주면 몇 명일까?")
    void cursorReader_경계값_예측하고검증() throws Exception {
        // 배경: 휴면대상 시드(회원21~30)의 last_login_at은 정확히 2024-01-15 00:00:00 이다.
        //       WHERE 조건은 "< cutoffDate" (미만!)

        // when : cutoffDate='2024-01-15'로 실행하면 READ_COUNT가 몇일지
        //        먼저 주석으로 예측을 적고, 실행해서 검증하세요
        // TODO 4

        // then : (예측이 빗나갔다면 — 그게 바로 경계값 테스트의 존재 이유입니다!)
        // TODO 5
    }
}
