package com.batchflow.advanced.step15.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.partition.PartitionedMemberScanJobConfig;
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

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 15 — example] Partitioning — 나눠서 정복의 검증
 *
 * 파티셔닝 테스트의 세 기둥:
 * 1. 분할: 워커가 gridSize(3)만큼 생겼는가 (partition0~2)
 * 2. 완전성: 워커들의 READ 합계 = 전체 50 — 빠뜨린 범위도 겹친 범위도 없다
 * 3. 분배: 각 워커가 실제로 일을 나눠 받았는가 (전부 한 워커에 몰리지 않았는가)
 */
@SpringBatchTest
@SpringBootTest(classes = {PartitionedMemberScanJobConfig.class, TestBatchConfig.class})
@DisplayName("파티셔닝 회원 스캔 Job")
class PartitionedMemberScanJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    @DisplayName("3개 파티션이 50명을 빠짐없이/겹침없이 나눠 읽는다")
    void partitionedScanJob_3분할_완전성검증() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then(1) : Manager 1 + Worker 3 = StepExecution 4개
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions()).hasSize(4);

        // then(2) : 워커들만 추려서 — 이름 규약은 "워커스텝명:파티션명"
        List<StepExecution> workers = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStepName().startsWith("partitionWorkerStep:partition"))
                .collect(Collectors.toList());
        assertThat(workers).hasSize(3);

        // then(3) : 완전성 — 합계가 정확히 전체 회원 수 (겹치면 50 초과, 빠지면 미달!)
        int totalRead = workers.stream().mapToInt(StepExecution::getReadCount).sum();
        assertThat(totalRead).isEqualTo(50);

        // then(4) : 분배 — 모든 워커가 실제로 일감을 받았다 (범위 분할: 17+17+16)
        assertThat(workers).allSatisfy(worker ->
                assertThat(worker.getReadCount()).isGreaterThan(0));
    }
}
