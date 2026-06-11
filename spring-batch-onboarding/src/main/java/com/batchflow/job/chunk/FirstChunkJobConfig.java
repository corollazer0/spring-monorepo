package com.batchflow.job.chunk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Step 6: Chunk 모델 첫 경험 — 대량 처리의 심장
 *
 * Tasklet의 한계: 10만 건을 한 덩이로 처리하면 ①전부 메모리에 올려야 하고
 * ②하나의 트랜잭션이라 중간 실패 시 전체 롤백된다.
 *
 * Chunk 모델: 읽고(reader, 1건씩) → 가공하고(processor, 1건씩) →
 * 묶어서 쓴다(writer, chunk 단위). **트랜잭션은 chunk마다 커밋** —
 * 5만 번째에서 죽어도 그 전까지의 chunk들은 살아있다.
 *
 * 첫 경험은 DB 없이 숫자 1~10으로 — chunk의 "기계 장치"에만 집중한다.
 * (DB에서 읽는 진짜 Reader는 Step 7부터)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirstChunkJobConfig {

    private static final String JOB_NAME = "firstChunkJob";
    private static final int CHUNK_SIZE = 3; // 1~10을 3개씩 → 3,3,3,1 = 4개의 chunk

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job firstChunkJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(firstChunkStep())
                .build();
    }

    @Bean
    public Step firstChunkStep() {
        return stepBuilderFactory.get("firstChunkStep")
                .<Integer, Integer>chunk(CHUNK_SIZE) // <읽는 타입, 쓰는 타입>
                .reader(numberReader())
                .processor(evenOnlyProcessor())
                .writer(chunkLogWriter())
                .build();
    }

    /**
     * @StepScope 필수! ListItemReader는 "어디까지 읽었는지" 상태를 가진다.
     * 싱글톤이면 첫 실행이 다 소진해버려 두 번째 실행(다른 테스트)에서 읽을 게 없다.
     */
    @Bean
    @StepScope
    public ListItemReader<Integer> numberReader() {
        List<Integer> numbers = IntStream.rangeClosed(1, 10)
                .boxed()
                .collect(Collectors.toList());
        return new ListItemReader<>(numbers);
    }

    /**
     * Processor의 두 가지 일: 변환(x10), 그리고 필터링 — null을 반환하면 그 건은 버려진다.
     * 버려진 건수는 FILTER_COUNT에 집계된다 (예외가 아니다! Skip과 다르다 — Step 11에서 비교)
     */
    @Bean
    public ItemProcessor<Integer, Integer> evenOnlyProcessor() {
        return number -> {
            if (number % 2 != 0) {
                return null; // 홀수는 필터링
            }
            return number * 10;
        };
    }

    /**
     * Writer는 1건씩이 아니라 "chunk 묶음"으로 받는다 — 로그에서 묶음 크기를 확인하라.
     */
    @Bean
    public ItemWriter<Integer> chunkLogWriter() {
        return items -> log.info(">>>>> [Writer] chunk 묶음 도착 ({}건): {}", items.size(), items);
    }
}
