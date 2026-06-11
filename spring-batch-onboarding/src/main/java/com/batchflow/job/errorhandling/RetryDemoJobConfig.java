package com.batchflow.job.errorhandling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Step 11-B: Retry — 일시적 오류는 다시 시도한다
 *
 * 시나리오: item 5 처리 시 "일시적 오류"(네트워크 순단 시뮬레이션)가
 * 두 번 발생하고 세 번째에 성공한다. retry 설정 덕분에 Job은 결국 완주한다.
 *
 * Skip과의 구분:
 * - Skip  : 그 데이터가 글러먹었다 → 버리고 간다 (데이터 문제)
 * - Retry : 지금은 안 되지만 곧 될 것이다 → 다시 시도 (환경 문제)
 *
 * ⚠️ Retry의 대가: 예외가 나면 chunk가 롤백되고 **chunk의 다른 항목들도 재처리**된다.
 *    → processor는 재처리에 안전(멱등)해야 한다!
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RetryDemoJobConfig {

    private static final String JOB_NAME = "retryDemoJob";
    private static final int CHUNK_SIZE = 3;
    private static final int RETRY_LIMIT = 3;

    public static final String KEY_ITEM5_ATTEMPTS = "item5Attempts";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job retryDemoJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(retryDemoStep())
                .build();
    }

    @Bean
    public Step retryDemoStep() {
        return stepBuilderFactory.get("retryDemoStep")
                .<Integer, Integer>chunk(CHUNK_SIZE)
                .reader(retryDemoReader())
                .processor(flakyProcessor(null))
                .writer(retryDemoWriter())
                .faultTolerant()
                .retry(IllegalStateException.class)   // 일시적 오류로 간주할 예외
                .retryLimit(RETRY_LIMIT)              // 시도 총 한도 (초과 시 실패)
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<Integer> retryDemoReader() {
        List<Integer> numbers = IntStream.rangeClosed(1, 10)
                .boxed()
                .collect(Collectors.toList());
        return new ListItemReader<>(numbers);
    }

    /**
     * item 5에서 두 번 실패 후 성공하는 프로세서.
     * 시도 횟수를 ExecutionContext에 남겨 테스트가 "정말 3번 시도했는지" 검증한다.
     */
    @Bean
    @StepScope
    public ItemProcessor<Integer, Integer> flakyProcessor(
            @Value("#{stepExecution}") StepExecution stepExecution) {
        return number -> {
            if (number == 5) {
                int attempts = (stepExecution.getExecutionContext()
                        .containsKey(KEY_ITEM5_ATTEMPTS)
                        ? stepExecution.getExecutionContext().getInt(KEY_ITEM5_ATTEMPTS) : 0) + 1;
                stepExecution.getExecutionContext().putInt(KEY_ITEM5_ATTEMPTS, attempts);

                if (attempts < 3) {
                    log.warn(">>>>> [WARN] item 5 일시적 오류 (시도 {}/3)", attempts);
                    throw new IllegalStateException("일시적 오류 — 잠시 후 재시도");
                }
                log.info(">>>>> item 5 세 번째 시도에 성공!");
            }
            return number;
        };
    }

    @Bean
    public ItemWriter<Integer> retryDemoWriter() {
        return items -> log.info(">>>>> [Writer] 처리 묶음: {}", items);
    }
}
