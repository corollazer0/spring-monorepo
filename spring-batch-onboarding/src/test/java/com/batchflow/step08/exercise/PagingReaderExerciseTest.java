package com.batchflow.step08.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.domain.Member;
import com.batchflow.job.dormant.DormantCandidatePagingScanJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * [Batch Step 8 — exercise] 리더 단독 테스트 기법을 직접 써보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 "리더 단독 테스트"를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 힌트: MetaDataInstanceFactory.createStepExecution(파라미터) +
 *       StepScopeTestUtils.doInStepScope(stepExecution, () -> { open/read/close })
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step08.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {DormantCandidatePagingScanJobConfig.class, TestBatchConfig.class})
@DisplayName("페이징 리더 단독 테스트 (연습문제)")
class PagingReaderExerciseTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcPagingItemReader<Member> dormantCandidatePagingReader;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("과거 기준일(2023-01-01)이면 리더가 한 건도 돌려주지 않는다")
    void pagingReader_과거기준일_빈결과() throws Exception {
        // given : cutoffDate=2023-01-01 파라미터를 가진 가짜 StepExecution을 만드세요
        // TODO 1

        // when : doInStepScope 안에서 리더를 열고, 첫 read() 결과를 받으세요
        // TODO 2

        // then : 첫 read()부터 null(읽을 것 없음)인지 검증하세요
        // TODO 3 (close를 잊지 마세요 — finally!)
    }

    @Test
    @DisplayName("읽힌 회원은 전원 ACTIVE 상태다 (WHERE 조건의 내용물 검증)")
    void pagingReader_전원ACTIVE_검증() throws Exception {
        // given & when : cutoffDate=2025-06-11로 리더 단독 실행,
        //                이번엔 ID가 아니라 "status" 목록을 수집하세요
        // TODO 4

        // then : 수집된 모든 status가 "ACTIVE"인지 검증하세요
        //        (DORMANT가 섞여 들어오면 Step 10에서 이중 전환 사고가 난다!)
        // TODO 5 (힌트: assertThat(statuses).containsOnly("ACTIVE"))
    }
}
