package com.batchflow.job.errorhandling;

import com.batchflow.listener.JobResultLoggingListener;
import com.batchflow.listener.SkipLoggingListener;
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
 * Step 11-A: Skip — 1건의 오류로 전체를 죽이지 않는다
 *
 * 시나리오: 숫자 1~10 처리 중 4의 배수(4, 8)에서 "비정상 데이터" 예외 발생.
 * faultTolerant + skip 설정으로 그 2건만 건너뛰고 나머지 8건을 완수한다.
 *
 * Skip의 동작 원리(중요!): 예외 발생 → 해당 chunk 롤백 → 1건씩 재처리하며
 * 범인만 격리 → 나머지는 정상 commit. 그래서 ROLLBACK_COUNT가 올라간다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SkipDemoJobConfig {

    private static final String JOB_NAME = "skipDemoJob";
    private static final int CHUNK_SIZE = 3;
    private static final int SKIP_LIMIT = 3; // 이 횟수를 넘는 skip은 "데이터가 아니라 시스템 문제" → 실패

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job skipDemoJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(skipDemoStep())
                .listener(new JobResultLoggingListener()) // afterJob = 알림 발송 지점
                .build();
    }

    @Bean
    public Step skipDemoStep() {
        return stepBuilderFactory.get("skipDemoStep")
                .<Integer, Integer>chunk(CHUNK_SIZE)
                .reader(skipDemoReader())
                .processor(badDataProcessor())
                .writer(skipDemoWriter())
                .faultTolerant()                               // 오류 제어 모드 ON
                .skip(IllegalArgumentException.class)          // 이 예외는 건너뛰어도 된다
                .skipLimit(SKIP_LIMIT)                         // 단, 한도까지만
                .listener(skipLoggingListener(null))           // 건너뛴 것은 반드시 기록
                .build();
    }

    @Bean
    @StepScope
    public ListItemReader<Integer> skipDemoReader() {
        List<Integer> numbers = IntStream.rangeClosed(1, 10)
                .boxed()
                .collect(Collectors.toList());
        return new ListItemReader<>(numbers);
    }

    /**
     * 4의 배수 = 비정상 데이터 시뮬레이션. null(필터, Step 6)이 아니라 "예외"다 —
     * 필터는 설계된 제외, 예외는 사고. Skip은 사고를 다루는 장치다.
     */
    @Bean
    public ItemProcessor<Integer, Integer> badDataProcessor() {
        return number -> {
            if (number % 4 == 0) {
                throw new IllegalArgumentException("비정상 데이터: " + number);
            }
            return number;
        };
    }

    @Bean
    @StepScope
    public SkipLoggingListener skipLoggingListener(
            @Value("#{stepExecution}") StepExecution stepExecution) {
        return new SkipLoggingListener(stepExecution);
    }

    @Bean
    public ItemWriter<Integer> skipDemoWriter() {
        return items -> log.info(">>>>> [Writer] 정상 처리 묶음: {}", items);
    }
}
