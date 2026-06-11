package com.batchflow.job.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Step 4: JobExecutionDecider — Step 없이 "판단만" 하는 분기 전문가
 *
 * checkStep 방식(ExitStatus 조작)의 찜찜함: 분기 판단을 위해 "가짜 Step"이 하나 끼고,
 * Step의 종료 코드를 본래 용도(성공/실패)와 다르게 쓴다.
 *
 * Decider는 처리 없이 판단만 담당하는 전용 컴포넌트 — 분기 로직이 복잡하면 이쪽이 정석.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OddEvenDeciderJobConfig {

    private static final String JOB_NAME = "oddEvenDeciderJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job oddEvenDeciderJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(numberLoadStep())
                .next(oddEvenDecider())                                  // Step 다음에 판단자 배치
                .from(oddEvenDecider()).on("EVEN").to(evenStep())
                .from(oddEvenDecider()).on("ODD").to(oddStep())
                .end()
                .build();
    }

    /**
     * Decider는 Step이 아니다 — StepExecution을 만들지 않고(장부에 안 남고) 판단 코드만 반환한다.
     */
    @Bean
    public JobExecutionDecider oddEvenDecider() {
        return (jobExecution, stepExecution) -> {
            Long number = jobExecution.getJobParameters().getLong("number");
            if (number == null) {
                log.warn(">>>>> [WARN] number 파라미터 없음 — ODD 경로로 보낸다");
                return new FlowExecutionStatus("ODD");
            }
            FlowExecutionStatus status = (number % 2 == 0)
                    ? new FlowExecutionStatus("EVEN")
                    : new FlowExecutionStatus("ODD");
            log.info(">>>>> [Decider] number={} → {}", number, status.getName());
            return status;
        };
    }

    @Bean
    public Step numberLoadStep() {
        return stepBuilderFactory.get("numberLoadStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [numberLoadStep] 판정 대상 준비");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step evenStep() {
        return stepBuilderFactory.get("evenStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [evenStep] 짝수 처리");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step oddStep() {
        return stepBuilderFactory.get("oddStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [oddStep] 홀수 처리");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
