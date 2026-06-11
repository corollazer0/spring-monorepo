package com.batchflow.step06.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.chunk.FirstChunkJobConfig;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 6 — example A] Chunk의 기계 장치 — 카운트로 전부 증명된다
 *
 * 입력: 숫자 1~10, chunk size 3, 홀수는 필터(processor가 null 반환)
 *
 * 예측해보자 (읽기 전에!):
 * - READ_COUNT   = ?   (reader가 돌려준 건수)
 * - FILTER_COUNT = ?   (processor가 null로 버린 건수)
 * - WRITE_COUNT  = ?   (writer에 도달한 건수)
 * - COMMIT_COUNT = ?   (트랜잭션 커밋 횟수 = chunk 묶음 수)
 *
 * 답: 10 / 5 / 5 / 4 (3,3,3,1 — 마지막 chunk는 1건만 남아도 커밋된다)
 * 이 카운트 검증이 Chunk 테스트의 표준 무기다 — Step 1에서 본 그 컬럼들!
 */
@SpringBatchTest
@SpringBootTest(classes = {FirstChunkJobConfig.class, TestBatchConfig.class})
@DisplayName("첫 Chunk Job (카운트 검증)")
class FirstChunkJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("read 10 / filter 5 / write 5 / commit 4 — chunk의 모든 것이 카운트에 있다")
    void firstChunkJob_실행_카운트검증() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount())
                .as("reader가 1~10을 전부 돌려줬다").isEqualTo(10);
        assertThat(stepExecution.getFilterCount())
                .as("processor가 홀수 5개를 null로 버렸다 (예외 아님!)").isEqualTo(5);
        assertThat(stepExecution.getWriteCount())
                .as("writer에는 짝수 5개만 도달했다").isEqualTo(5);
        assertThat(stepExecution.getCommitCount())
                .as("chunk(3) 단위 커밋: 3+3+3+1 = 4묶음").isEqualTo(4);
    }

    @Test
    @DisplayName("같은 reader Bean인데 두 번째 실행도 10건을 읽는다 (@StepScope의 가치)")
    void firstChunkJob_재실행_reader가새로생성() throws Exception {
        // given : 한 번 실행해 reader를 소진시킨다
        jobLauncherTestUtils.launchJob();

        // when : 다시 실행 (unique 파라미터 — Step 3!)
        JobExecution second = jobLauncherTestUtils.launchJob();

        // then : @StepScope 덕분에 실행마다 reader가 새로 만들어져 또 10건을 읽는다.
        //        (싱글톤이었다면 두 번째는 READ_COUNT 0 — 직접 @StepScope를 지워 확인해보라!)
        StepExecution stepExecution = second.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(10);
    }
}
