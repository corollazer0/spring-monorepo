package com.batchflow.job.parameters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 3: JobParameters와 JobInstance — "어느 날짜 기준으로 돌릴 것인가"
 *
 * 운영 배치의 실행 명령은 대부분 이렇다: "dailyGreetingJob을 2026-06-11 기준으로 실행해".
 * 그 "기준값"이 JobParameters이고, Job+파라미터의 조합이 JobInstance(논리적 실행 단위)다.
 *
 * 핵심 설계: 성공한 JobInstance는 같은 파라미터로 재실행할 수 없다
 * (JobInstanceAlreadyCompleteException) — 같은 날짜의 정산이 두 번 도는 사고를
 * 프레임워크 차원에서 막는 장치다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailyGreetingJobConfig {

    private static final String JOB_NAME = "dailyGreetingJob";
    private static final String STEP_NAME = "dailyGreetingStep";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job dailyGreetingJob() {
        return jobBuilderFactory.get(JOB_NAME)
                // @JobScope Bean이라 정의 시점엔 null을 넘긴다 — 실제 값은 실행 시점에 주입(Late Binding)
                .start(dailyGreetingStep(null))
                .build();
    }

    /**
     * @JobScope: 이 Step Bean은 "Job 실행 시점"에 생성된다.
     * 그래야 그 실행의 JobParameters를 @Value SpEL로 주입받을 수 있다 — Late Binding.
     * (@JobScope 없이 jobParameters를 참조하면 컨텍스트 기동 시점에 값이 없어 실패한다!)
     */
    @Bean
    @JobScope
    public Step dailyGreetingStep(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return stepBuilderFactory.get(STEP_NAME)
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [{}] 기준일 {} 인사 배치 실행", JOB_NAME, targetDate);

                    // 주입받은 파라미터를 ExecutionContext에 남겨 테스트가 검증할 수 있게 한다
                    contribution.getStepExecution().getExecutionContext()
                            .putString("greetedDate", targetDate);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
