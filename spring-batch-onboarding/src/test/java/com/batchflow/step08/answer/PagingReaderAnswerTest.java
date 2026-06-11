package com.batchflow.step08.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.domain.Member;
import com.batchflow.job.dormant.DormantCandidatePagingScanJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.batch.test.StepScopeTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 8 — answer] PagingReaderExerciseTest 모범답안
 *
 * 채점 포인트:
 * - StepScopeTestUtils로 @StepScope 리더를 단독 구동했는가
 * - close를 finally로 보장했는가 (자원 정리)
 * - 카운트가 아닌 "내용물"(status)을 검증했는가
 */
@SpringBatchTest
@SpringBootTest(classes = {DormantCandidatePagingScanJobConfig.class, TestBatchConfig.class})
@DisplayName("페이징 리더 단독 테스트 (모범답안)")
class PagingReaderAnswerTest {

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
        // given (TODO 1 답)
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(
                new JobParametersBuilder()
                        .addString("cutoffDate", "2023-01-01")
                        .toJobParameters());

        // when (TODO 2 답)
        Member first = StepScopeTestUtils.doInStepScope(stepExecution, () -> {
            dormantCandidatePagingReader.open(stepExecution.getExecutionContext());
            try {
                return dormantCandidatePagingReader.read();
            } finally {
                dormantCandidatePagingReader.close(); // (TODO 3의 일부) 자원 정리 보장
            }
        });

        // then (TODO 3 답)
        assertThat(first).isNull();
    }

    @Test
    @DisplayName("읽힌 회원은 전원 ACTIVE 상태다 (WHERE 조건의 내용물 검증)")
    void pagingReader_전원ACTIVE_검증() throws Exception {
        // given & when (TODO 4 답)
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(
                new JobParametersBuilder()
                        .addString("cutoffDate", "2025-06-11")
                        .toJobParameters());

        List<String> statuses = StepScopeTestUtils.doInStepScope(stepExecution, () -> {
            List<String> collected = new ArrayList<>();
            dormantCandidatePagingReader.open(stepExecution.getExecutionContext());
            try {
                Member member;
                while ((member = dormantCandidatePagingReader.read()) != null) {
                    collected.add(member.getStatus());
                }
            } finally {
                dormantCandidatePagingReader.close();
            }
            return collected;
        });

        // then (TODO 5 답) : DORMANT/WITHDRAWN이 한 건이라도 섞이면 이중 전환 사고!
        assertThat(statuses).hasSize(10).containsOnly("ACTIVE");
    }
}
