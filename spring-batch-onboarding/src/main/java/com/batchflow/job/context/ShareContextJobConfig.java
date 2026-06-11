package com.batchflow.job.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 5: ExecutionContext — Step 사이의 공식 전달 통로
 *
 * 시나리오: countStep이 집계한 "대상 건수"를 reportStep이 받아서 사용한다.
 *
 * 규칙:
 * - 각 Step은 자기만의 ExecutionContext(사물함)를 가진다 — 남의 사물함은 못 본다
 * - Step 간 공유하려면 Job 레벨 ExecutionContext(공용 게시판)로 올려야 한다
 * - 그 승격을 해주는 것이 ExecutionContextPromotionListener (keys에 지정한 것만!)
 *
 * 멤버 변수/static으로 공유하면 안 되는 이유: 멀티스레드(Step 14)와 재시작(Step 12)에서
 * 전부 깨진다. ExecutionContext는 장부(BATCH_*_EXECUTION_CONTEXT)에 직렬화되어
 * 재시작 시에도 복원된다 — 그것이 "공식 통로"인 이유다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ShareContextJobConfig {

    private static final String JOB_NAME = "shareContextJob";
    public static final String KEY_TARGET_COUNT = "targetCount";
    public static final String KEY_SECRET_NOTE = "secretNote";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job shareContextJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(countStep())
                .next(reportStep())
                .build();
    }

    /**
     * 대상 건수를 집계해 자기 ExecutionContext에 넣는다.
     * promotionListener가 Step 종료 시 keys에 지정된 것만 Job 레벨로 승격한다.
     */
    @Bean
    public Step countStep() {
        return stepBuilderFactory.get("countStep")
                .tasklet((contribution, chunkContext) -> {
                    int targetCount = 42; // 실전이라면 SELECT COUNT(*) 결과

                    ExecutionContext stepContext =
                            contribution.getStepExecution().getExecutionContext();
                    stepContext.putInt(KEY_TARGET_COUNT, targetCount);   // 승격 대상
                    stepContext.putString(KEY_SECRET_NOTE, "step-only"); // 승격 안 함 (exercise 소재!)

                    log.info(">>>>> [countStep] 대상 건수 집계: {}", targetCount);
                    return RepeatStatus.FINISHED;
                })
                .listener(promotionListener())
                .build();
    }

    /**
     * Job 레벨 ExecutionContext(공용 게시판)에서 앞 Step의 결과를 읽는다.
     */
    @Bean
    public Step reportStep() {
        return stepBuilderFactory.get("reportStep")
                .tasklet((contribution, chunkContext) -> {
                    ExecutionContext jobContext = chunkContext.getStepContext()
                            .getStepExecution().getJobExecution().getExecutionContext();

                    int received = jobContext.getInt(KEY_TARGET_COUNT);
                    log.info(">>>>> [reportStep] 전달받은 대상 건수: {}", received);

                    // 받았다는 증거를 자기 사물함에 남긴다 (테스트 검증용)
                    contribution.getStepExecution().getExecutionContext()
                            .putInt("reportedCount", received);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * keys에 지정한 키만 Step → Job 레벨로 승격된다. 빠진 키는 Step 사물함에만 남는다.
     */
    @Bean
    public ExecutionContextPromotionListener promotionListener() {
        ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{KEY_TARGET_COUNT});
        return listener;
    }
}
