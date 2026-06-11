package com.batchflow.job.parallel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * 심화 Step 14-B: Parallel Flow — 독립적인 Step들을 동시에
 *
 * Multi-threaded Step이 "한 Step 안"의 병렬이라면,
 * split은 "서로 무관한 Step들"을 통째로 동시에 돌린다.
 * 예: 회원 통계와 거래 통계 — 서로 기다릴 이유가 없다.
 *
 * 전제: 두 Flow가 같은 데이터를 쓰지(write) 않아야 한다 — 독립성이 곧 안전.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ParallelFlowJobConfig {

    private static final String JOB_NAME = "parallelFlowJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job parallelFlowJob() {
        Flow splitFlow = new FlowBuilder<Flow>("splitFlow")
                .split(new SimpleAsyncTaskExecutor("flow-")) // Flow마다 새 스레드
                .add(memberStatFlow(), transactionStatFlow())
                .build();

        return jobBuilderFactory.get(JOB_NAME)
                .start(splitFlow)
                .end()
                .build();
    }

    @Bean
    public Flow memberStatFlow() {
        return new FlowBuilder<Flow>("memberStatFlow")
                .start(memberStatStep())
                .build();
    }

    @Bean
    public Flow transactionStatFlow() {
        return new FlowBuilder<Flow>("transactionStatFlow")
                .start(transactionStatStep())
                .build();
    }

    @Bean
    public Step memberStatStep() {
        return stepBuilderFactory.get("memberStatStep")
                .tasklet((contribution, chunkContext) -> {
                    String thread = Thread.currentThread().getName();
                    contribution.getStepExecution().getExecutionContext()
                            .putString("workerThread", thread); // 어느 스레드가 들었는지 증거
                    log.info(">>>>> [{}] 회원 통계 집계", thread);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step transactionStatStep() {
        return stepBuilderFactory.get("transactionStatStep")
                .tasklet((contribution, chunkContext) -> {
                    String thread = Thread.currentThread().getName();
                    contribution.getStepExecution().getExecutionContext()
                            .putString("workerThread", thread);
                    log.info(">>>>> [{}] 거래 통계 집계", thread);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
