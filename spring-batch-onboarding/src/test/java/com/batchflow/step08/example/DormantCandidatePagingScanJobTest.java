package com.batchflow.step08.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.domain.Member;
import com.batchflow.job.dormant.DormantCandidatePagingScanJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
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
 * [Batch Step 8 — example] 페이징 리더 — 끊어 읽기와 "리더 단독 테스트" 기법
 *
 * 두 가지를 배운다:
 * 1. 페이징 Job의 카운트 검증 (Step 6~7의 무기 재사용)
 * 2. 🆕 리더를 Job 없이 "단독으로" 여닫아 내용물까지 검증하는 기법
 *    (StepScopeTestUtils — @StepScope Bean을 가짜 StepExecution 안에서 실행)
 *
 * 카운트는 "몇 건"만 말해준다. "어떤 행이, 어떤 순서로"까지 보려면 단독 테스트가 필요하다.
 */
@SpringBatchTest
@SpringBootTest(classes = {DormantCandidatePagingScanJobConfig.class, TestBatchConfig.class})
@DisplayName("휴면 후보 페이징 스캔 (JdbcPagingItemReader)")
class DormantCandidatePagingScanJobTest {

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
    @DisplayName("후보 10명을 페이지(4)씩 끊어 읽는다 — 카운트는 커서와 동일")
    void pagingScanJob_기준일_후보10명() throws Exception {
        // when
        JobParameters params = new JobParametersBuilder()
                .addString("cutoffDate", "2025-06-11")
                .toJobParameters();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

        // then : 읽는 "방식"은 달라도 결과 카운트는 커서(Step 7)와 같아야 한다
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(10);
        assertThat(stepExecution.getCommitCount()).isEqualTo(3); // 4+4+2
    }

    /**
     * 🆕 리더 단독 테스트: Job 전체를 돌리지 않고 리더만 여닫는다.
     * @StepScope Bean은 Step 실행 중에만 존재하므로, StepScopeTestUtils가
     * "가짜 StepExecution(파라미터 포함)"을 만들어 그 안에서 실행해준다.
     */
    @Test
    @DisplayName("리더 단독 테스트 — 내용물(ID 21~30)과 정렬 순서까지 검증")
    void pagingReader_단독실행_내용과순서검증() throws Exception {
        // given : cutoffDate 파라미터를 가진 가짜 StepExecution
        StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(
                new JobParametersBuilder()
                        .addString("cutoffDate", "2025-06-11")
                        .toJobParameters());

        // when : 그 스코프 안에서 리더를 직접 open → read 반복 → close
        List<Long> memberIds = StepScopeTestUtils.doInStepScope(stepExecution, () -> {
            List<Long> ids = new ArrayList<>();
            dormantCandidatePagingReader.open(stepExecution.getExecutionContext());
            try {
                Member member;
                while ((member = dormantCandidatePagingReader.read()) != null) {
                    ids.add(member.getMemberId());
                }
            } finally {
                dormantCandidatePagingReader.close();
            }
            return ids;
        });

        // then : 카운트가 아니라 "내용물" — 정확히 회원 21~30이, 오름차순으로
        assertThat(memberIds).containsExactly(21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L);
    }
}
