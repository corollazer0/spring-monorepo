package com.batchflow.job.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
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
 * Step 4: Flow 제어 — 검증 결과에 따라 흐름을 가른다
 *
 * 시나리오: checkStep(검증) → 통과면 mainStep(본처리), 실패면 recoveryStep(복구)
 *
 * 핵심: 분기의 기준은 BatchStatus가 아니라 **ExitStatus(흐름 제어용 코드)**다.
 * checkStep은 예외를 던지지 않고 정상 종료하면서 ExitStatus만 FAILED로 칠한다 —
 * "Step은 멀쩡히 끝났지만, 다음 행선지는 복구"라는 신호.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ConditionalFlowJobConfig {

    private static final String JOB_NAME = "conditionalFlowJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job conditionalFlowJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(checkStep(null))
                    .on("FAILED").to(recoveryStep())   // 검증 실패(ExitStatus 기준!) → 복구 경로
                .from(checkStep(null))
                    .on("*").to(mainStep())            // 그 외 전부 → 본처리 경로
                .end()                                  // Flow 정의 종료
                .build();
    }

    /**
     * mode=fail 파라미터면 ExitStatus를 FAILED로 — "예외 없이" 흐름만 바꾸는 것이 포인트.
     */
    @Bean
    @JobScope
    public Step checkStep(@Value("#{jobParameters['mode']}") String mode) {
        return stepBuilderFactory.get("checkStep")
                .tasklet((contribution, chunkContext) -> {
                    if ("fail".equals(mode)) {
                        log.warn(">>>>> [WARN] 검증 실패 — 복구 경로로 분기시킨다");
                        contribution.setExitStatus(ExitStatus.FAILED); // 흐름 제어 코드만 조작
                    } else {
                        log.info(">>>>> 검증 통과 — 본처리 경로로");
                    }
                    return RepeatStatus.FINISHED; // Step 자체는 어느 쪽이든 정상 종료!
                })
                .build();
    }

    @Bean
    public Step mainStep() {
        return stepBuilderFactory.get("mainStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [mainStep] 본처리 실행");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step recoveryStep() {
        return stepBuilderFactory.get("recoveryStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [recoveryStep] 복구 처리 실행");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
