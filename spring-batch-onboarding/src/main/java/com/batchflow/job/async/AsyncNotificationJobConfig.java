package com.batchflow.job.async;

import com.batchflow.domain.Member;
import com.batchflow.domain.MemberRowMapper;
import com.batchflow.domain.Notification;
import com.batchflow.processor.NotificationComposeProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Future;

/**
 * 심화 Step 16-B: 비동기 알림 발송 Job — Processor만 병렬로.
 *
 * Step 14(멀티스레드 Step)와의 결정적 차이:
 *   멀티스레드 Step = chunk "전체"(읽기→가공→쓰기)가 병렬 → reader도 thread-safe(페이징) 강제
 *   AsyncItemProcessor = "가공만" 병렬, 읽기/쓰기는 단일 스레드 그대로
 *                       → 커서 리더를 그대로 쓸 수 있다! 처리 순서도 chunk 안에서 보존된다.
 *
 * 동작: AsyncItemProcessor가 건마다 Future를 즉시 반환(가공은 풀에서 진행)
 *       → AsyncItemWriter가 쓰기 직전에 Future를 풀어(get) 위임 writer에 전달.
 * 그래서 Step의 출력 타입이 Notification이 아니라 Future&lt;Notification&gt;이다.
 *
 * 주의: AsyncItemProcessor/Writer는 spring-batch-core가 아니라
 * spring-batch-integration 모듈에 있다 (의존성 누락 = 컴파일부터 실패).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AsyncNotificationJobConfig {

    private static final String JOB_NAME = "asyncNotificationJob";
    private static final int CHUNK_SIZE = 5;
    private static final int FETCH_SIZE = 100;
    private static final int THREAD_COUNT = 8;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job asyncNotificationJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(asyncNotificationStep())
                .build();
    }

    @Bean
    public Step asyncNotificationStep() {
        return stepBuilderFactory.get("asyncNotificationStep")
                .<Member, Future<Notification>>chunk(CHUNK_SIZE)   // 출력이 Future다!
                .reader(asyncDormantMemberReader())
                .processor(asyncComposeProcessor())
                .writer(asyncNotificationWriter())
                .build();
    }

    /** 커서 리더 그대로 — 읽기는 여전히 단일 스레드라서 (이게 이 방식의 장점) */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Member> asyncDormantMemberReader() {
        return new JdbcCursorItemReaderBuilder<Member>()
                .name("asyncDormantMemberReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, name, email, status, last_login_at, dormant_at "
                        + "FROM member WHERE status = 'DORMANT' ORDER BY member_id")
                .rowMapper(new MemberRowMapper())
                .fetchSize(FETCH_SIZE)
                .build();
    }

    /** 위임 Processor(동기와 동일!)를 스레드 풀에서 돌리는 래퍼 */
    @Bean
    public AsyncItemProcessor<Member, Notification> asyncComposeProcessor() {
        AsyncItemProcessor<Member, Notification> asyncProcessor = new AsyncItemProcessor<>();
        asyncProcessor.setDelegate(new NotificationComposeProcessor()); // 변인 통제 — 같은 가공기
        asyncProcessor.setTaskExecutor(asyncNotifyExecutor());
        return asyncProcessor;
    }

    /** 쓰기 직전에 Future.get()으로 풀어서 위임 writer(동기와 동일)에 전달하는 래퍼 */
    @Bean
    public AsyncItemWriter<Notification> asyncNotificationWriter() {
        AsyncItemWriter<Notification> asyncWriter = new AsyncItemWriter<>();
        asyncWriter.setDelegate(asyncNotificationJdbcWriter());
        return asyncWriter;
    }

    @Bean
    public JdbcBatchItemWriter<Notification> asyncNotificationJdbcWriter() {
        return new JdbcBatchItemWriterBuilder<Notification>()
                .dataSource(dataSource)
                .sql("INSERT INTO notification_history (member_id, message) VALUES (:memberId, :message)")
                .beanMapped()
                .build();
    }

    @Bean
    public ThreadPoolTaskExecutor asyncNotifyExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(THREAD_COUNT);
        executor.setMaxPoolSize(THREAD_COUNT);
        executor.setThreadNamePrefix("batch-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
