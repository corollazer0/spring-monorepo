package com.batchflow.job.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 심화 Step 17: 운영 데모 Job — JobOperator로 제어당하는 쪽.
 *
 * 지금까지 Job은 "테스트 코드"가 돌렸다 (JobLauncherTestUtils).
 * 운영자는 코드가 아니라 이름과 문자열 파라미터로 Job을 다룬다:
 *   jobOperator.start("opsDemoJob", "date=2026-06-12")
 * 그 전제가 JobRegistry — 이름 → Job 빈을 찾아주는 전화번호부다.
 *
 * ⚠️ JobRegistryBeanPostProcessor를 등록하지 않으면 레지스트리가 "빈" 채로 남고,
 * operator.start는 NoSuchJobException을 던진다 — Job 빈은 멀쩡히 있는데도!
 * (컨텍스트의 빈 != 레지스트리의 등록 — 이 간극이 이 Step의 1번 함정)
 *
 * BROKEN 스위치는 환경 장애(외부 저장소 다운 등) 시뮬레이션 교보재 —
 * 운영 코드 금지 + 테스트에서 Before/AfterEach 정리 의무 (SEEN_THREADS 규약과 동일).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OpsDemoJobConfig {

    private static final String JOB_NAME = "opsDemoJob";

    /** 교보재: 환경 장애 스위치 — true면 Job이 실패한다 (운영 금지!) */
    public static final AtomicBoolean BROKEN = new AtomicBoolean(false);

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job opsDemoJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(opsDemoStep())
                .build();
    }

    @Bean
    public Step opsDemoStep() {
        return stepBuilderFactory.get("opsDemoStep")
                .tasklet((contribution, chunkContext) -> {
                    if (BROKEN.get()) {
                        // 환경 장애 — 코드가 아니라 "바깥"이 문제인 실패 (운영자가 고친 뒤 재기동한다)
                        throw new IllegalStateException("환경 장애: 외부 저장소에 연결할 수 없습니다 (BROKEN)");
                    }
                    log.info(">>>>> [{}] 정상 처리 완료", JOB_NAME);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * 이름 → Job 전화번호부 등록기.
     * 컨텍스트의 모든 Job 빈을 JobRegistry에 자동 등록한다 — JobOperator의 필수 전제.
     * (BeanPostProcessor는 static 권장 — 다른 빈보다 먼저 떠야 하므로)
     */
    @Bean
    public static JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }
}
