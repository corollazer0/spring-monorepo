package com.batchflow.step11.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.errorhandling.RetryDemoJobConfig;
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
 * [Batch Step 11 — example B] Retry — 일시적 오류는 다시 시도한다
 *
 * 시나리오: item 5가 두 번 "일시적 오류"를 내고 세 번째에 성공.
 * retry(3) 설정으로 Job은 10건 전부를 완주한다.
 *
 * 핵심 깨달음: 재시도는 그 1건만 다시 하는 게 아니다 —
 * 예외가 난 chunk가 통째로 롤백되고 chunk의 다른 항목들도 재처리된다.
 * → processor는 재처리에 안전(멱등)해야 한다!
 */
@SpringBatchTest
@SpringBootTest(classes = {RetryDemoJobConfig.class, TestBatchConfig.class})
@DisplayName("Retry 데모 Job")
class RetryDemoJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("두 번 실패한 item 5가 세 번째 시도에 성공 — 10건 전부 완주")
    void retryDemoJob_일시오류_재시도후완주() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then(1) : 결국 전부 처리됐다 — skip과 달리 버린 것이 없다!
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getWriteCount()).isEqualTo(10);

        // then(2) : 정말 3번 시도했는가 — 프로세서가 EC에 남긴 시도 횟수로 증명
        assertThat(stepExecution.getExecutionContext()
                .getInt(RetryDemoJobConfig.KEY_ITEM5_ATTEMPTS)).isEqualTo(3);

        // then(3) : 재시도의 대가 — 실패 시마다 chunk가 롤백됐다 (2회)
        //           item 5와 같은 chunk였던 4, 6도 그때마다 재처리됐다는 뜻!
        assertThat(stepExecution.getRollbackCount()).isEqualTo(2);
    }
}
