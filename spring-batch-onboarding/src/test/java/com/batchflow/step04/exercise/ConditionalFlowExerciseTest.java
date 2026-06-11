package com.batchflow.step04.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.flow.ConditionalFlowJobConfig;
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
 * [Batch Step 4 — exercise] BatchStatus vs ExitStatus를 장부에서 직접 증명해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example 두 클래스를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 이 과제의 백미: mode=fail로 실행한 checkStep의 장부 기록을 보면
 * STATUS(BatchStatus)와 EXIT_CODE(ExitStatus)가 **서로 다른 값**으로 남아있다!
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step04.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {ConditionalFlowJobConfig.class, TestBatchConfig.class})
@DisplayName("BatchStatus vs ExitStatus (연습문제)")
class ConditionalFlowExerciseTest {

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
    @DisplayName("분기용 FAILED는 장부에 STATUS=COMPLETED + EXIT_CODE=FAILED로 남는다")
    void checkStep_실패모드_두상태가다르게기록() throws Exception {
        // given & when : mode=fail 파라미터로 Job을 실행하세요
        // TODO 1

        // then : BATCH_STEP_EXECUTION에서 STEP_NAME='checkStep' 행을 조회해
        //        - STATUS    = 'COMPLETED' (Step은 정상 종료했다 — 예외가 없었으니까)
        //        - EXIT_CODE = 'FAILED'    (흐름 제어 코드만 FAILED로 칠했다)
        //        두 컬럼을 각각 검증하세요
        // TODO 2 (힌트: queryForMap("SELECT STATUS, EXIT_CODE FROM ... WHERE STEP_NAME = ?", ...))
    }

    @Test
    @DisplayName("mode 파라미터가 없으면(on(*) 경로) mainStep으로 흐른다")
    void checkStep_파라미터없음_본처리경로() throws Exception {
        // when : 파라미터 없이... 는 안 된다! (Step 3에서 배운 것 — unique 파라미터로 실행하세요)
        // TODO 3

        // then : 실행된 Step 이름이 checkStep → mainStep 순서인지 검증하세요
        // TODO 4
    }
}
