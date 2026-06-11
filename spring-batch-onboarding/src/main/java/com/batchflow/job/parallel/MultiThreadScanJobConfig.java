package com.batchflow.job.parallel;

import com.batchflow.domain.Member;
import com.batchflow.domain.MemberRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 심화 Step 14-A: Multi-threaded Step — chunk를 여러 스레드가 나눠 든다
 *
 * 4시간짜리 배치를 1시간으로 줄이는 첫 번째 무기.
 * Step은 하나지만 chunk 처리(읽기→가공→쓰기)가 스레드 풀에서 병렬로 돈다.
 *
 * 전제 조건 (Step 8의 복선 회수!):
 * - reader가 thread-safe 해야 한다 → JdbcPagingItemReader ✅ (커서 리더는 ❌)
 * - 처리 순서가 보장되지 않아도 되는 업무여야 한다
 * - saveState가 의미를 잃는다 (여러 스레드의 위치를 하나로 저장 불가) → 재시작은 전체 재실행 설계로
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MultiThreadScanJobConfig {

    private static final String JOB_NAME = "multiThreadScanJob";
    private static final int CHUNK_SIZE = 10;
    private static final int PAGE_SIZE = 10;
    private static final int THREAD_COUNT = 4;

    /** 교보재: 어떤 스레드들이 일했는지 수집 (테스트 검증용 — 운영 코드엔 두지 말 것) */
    public static final Set<String> SEEN_THREADS = ConcurrentHashMap.newKeySet();

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job multiThreadScanJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(multiThreadScanStep())
                .build();
    }

    @Bean
    public Step multiThreadScanStep() {
        return stepBuilderFactory.get("multiThreadScanStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(allMemberPagingReader())
                .processor(threadRecordingProcessor())
                .writer(multiThreadLogWriter())
                .taskExecutor(multiThreadTaskExecutor()) // 이 한 줄이 병렬화의 스위치
                .throttleLimit(THREAD_COUNT)             // 동시에 도는 chunk 수 상한
                .build();
    }

    /**
     * thread-safe 리더는 페이징! (Step 8) — 커서 리더를 여기 꽂으면 데이터가 깨진다.
     */
    @Bean
    @StepScope
    public JdbcPagingItemReader<Member> allMemberPagingReader() {
        return new JdbcPagingItemReaderBuilder<Member>()
                .name("allMemberPagingReader")
                .dataSource(dataSource)
                .pageSize(PAGE_SIZE)
                .selectClause("SELECT member_id, name, email, status, last_login_at, dormant_at")
                .fromClause("FROM member")
                .sortKeys(Collections.singletonMap("member_id", Order.ASCENDING))
                .rowMapper(new MemberRowMapper())
                .saveState(false) // 멀티스레드에서 위치 저장은 무의미 — 명시적으로 끈다
                .build();
    }

    @Bean
    public ItemProcessor<Member, Member> threadRecordingProcessor() {
        return member -> {
            SEEN_THREADS.add(Thread.currentThread().getName());
            return member;
        };
    }

    @Bean
    public ItemWriter<Member> multiThreadLogWriter() {
        return items -> log.info(">>>>> [{}] {}건 처리", Thread.currentThread().getName(), items.size());
    }

    @Bean
    public TaskExecutor multiThreadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(THREAD_COUNT);
        executor.setMaxPoolSize(THREAD_COUNT);
        executor.setThreadNamePrefix("batch-mt-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
