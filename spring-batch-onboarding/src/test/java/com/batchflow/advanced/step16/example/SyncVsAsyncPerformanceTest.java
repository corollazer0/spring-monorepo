package com.batchflow.advanced.step16.example;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.async.AsyncNotificationJobConfig;
import com.batchflow.job.async.SyncNotificationJobConfig;
import com.batchflow.processor.NotificationComposeProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 16 — example B] 동기 vs 비동기 성능 비교 — 측정으로 증명한다 (50-Step 31·34)
 *
 * 비교 실험의 3원칙:
 * 1. 변인 통제 — 두 Job은 같은 데이터(15건), 같은 Processor(20ms 지연), 같은 writer.
 *    다른 것은 "실행 방식" 하나뿐이다.
 * 2. 바닥 증명 — 동기는 구조적으로 15 x 20ms = 300ms 미만이 될 수 없다.
 *    이 바닥을 단언해야 "측정이 제대로 됐다"는 전제가 선다.
 * 3. 상대 비교 — 절대값(예: 비동기는 100ms 미만)은 CI 머신 사양에 흔들린다.
 *    "비동기 < 동기"라는 관계만 단언한다 (300ms 바닥이 안전 마진을 보장).
 *
 * 주의: Job 빈이 2개라 @SpringBatchTest(JobLauncherTestUtils)를 못 쓴다 —
 * JobLauncher를 직접 쓰고, 메타데이터 격리는 유니크 파라미터로 해결한다.
 */
@SpringBootTest(classes = {
        SyncNotificationJobConfig.class,
        AsyncNotificationJobConfig.class,
        TestBatchConfig.class})
@DisplayName("동기 vs 비동기 성능 비교")
class SyncVsAsyncPerformanceTest {

    private static final long SYNC_FLOOR_MS =
            15 * NotificationComposeProcessor.LATENCY_MS;   // 300ms — 동기의 구조적 바닥

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("syncNotificationJob")
    private Job syncNotificationJob;

    @Autowired
    @Qualifier("asyncNotificationJob")
    private Job asyncNotificationJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanNotifications() {
        jdbcTemplate.update("DELETE FROM notification_history");
    }

    @Test
    @DisplayName("같은 일, 같은 결과 — 그러나 비동기가 더 빠르다 (Processor 대기의 병렬화)")
    void 성능비교_동기바닥확인_비동기가더빠름() throws Exception {
        // when : 동기 실행 + 측정
        long syncMillis = runAndMeasure(syncNotificationJob, "sync");
        assertThat(countNotifications()).isEqualTo(15);
        jdbcTemplate.update("DELETE FROM notification_history");

        // when : 비동기 실행 + 측정
        long asyncMillis = runAndMeasure(asyncNotificationJob, "async");
        assertThat(countNotifications()).isEqualTo(15);   // 결과는 동일해야 비교가 성립

        // then-1 : 동기의 구조적 바닥(300ms) — 측정 자체의 검증
        assertThat(syncMillis)
                .as("동기는 15건 x 20ms 직렬 대기보다 빠를 수 없다")
                .isGreaterThanOrEqualTo(SYNC_FLOOR_MS);

        // then-2 : 상대 비교 — 절대값이 아니라 관계로 단언 (머신 사양 독립)
        assertThat(asyncMillis)
                .as("Processor 대기가 병렬화되면 동기보다 빨라야 한다 (sync=%dms, async=%dms)",
                        syncMillis, asyncMillis)
                .isLessThan(syncMillis);
    }

    private long runAndMeasure(Job job, String tag) throws Exception {
        // 유니크 파라미터 — JobInstance 충돌 없이 매번 새 실행 (Step 3의 교훈 응용)
        JobParameters params = new JobParametersBuilder()
                .addLong("ts", System.nanoTime())
                .addString("tag", tag)
                .toJobParameters();

        long start = System.nanoTime();
        JobExecution execution = jobLauncher.run(job, params);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        return elapsedMillis;
    }

    private Integer countNotifications() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification_history", Integer.class);
    }
}
