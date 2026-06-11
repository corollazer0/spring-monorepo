package com.batchflow.step01.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * [Batch Step 1 — exercise] 메타데이터 장부를 직접 들여다보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 BatchInfrastructureTest를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 힌트: H2의 INFORMATION_SCHEMA.COLUMNS 에서
 *       WHERE TABLE_NAME = '...' 조건으로 컬럼 목록을 조회할 수 있습니다.
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step01.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBootTest
@DisplayName("메타데이터 스키마 (연습문제)")
class MetadataSchemaExerciseTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("BATCH_JOB_INSTANCE에는 JOB_NAME과 JOB_KEY 컬럼이 있다")
    void jobInstance_식별컬럼_존재확인() {
        // when : BATCH_JOB_INSTANCE 테이블의 컬럼 목록을 조회하세요
        // TODO 1

        // then : JOB_NAME, JOB_KEY 컬럼이 존재하는지 검증하세요
        //        (JOB_KEY = 파라미터의 해시 — Step 3에서 "같은 파라미터 재실행 불가"의 비밀이 된다!)
        // TODO 2
    }

    @Test
    @DisplayName("BATCH_JOB_EXECUTION에는 상태/시각 컬럼이 있다")
    void jobExecution_상태컬럼_존재확인() {
        // when & then : STATUS, EXIT_CODE, START_TIME, END_TIME 컬럼 존재를 검증하세요
        //               (Job이 성공했는지/언제 돌았는지를 운영에서 추적하는 컬럼들)
        // TODO 3
    }
}
