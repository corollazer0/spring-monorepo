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
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 심화 Step 16-A: 동기 알림 발송 Job — 비교 실험의 대조군.
 *
 * 휴면 회원(DORMANT 15명)에게 알림을 만들어 notification_history에 저장한다.
 * Processor가 건당 20ms 걸리므로(외부 API 흉내) 단일 스레드에선
 * 최소 15 x 20 = 300ms가 처리 시간의 바닥이다 — 읽기/쓰기가 아무리 빨라도.
 *
 * 튜닝 포인트 2가지가 이미 적용되어 있다 (50-Step 32의 핵심):
 * - reader fetchSize: DB 왕복당 가져오는 행 수 (행 단위 왕복 방지)
 * - JdbcBatchItemWriter: chunk 단위 JDBC batch INSERT (건당 INSERT 왕복 방지)
 * 그런데도 느리다 — 병목이 I/O가 아니라 "Processor의 대기"라서. 그게 16-B의 동기다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SyncNotificationJobConfig {

    private static final String JOB_NAME = "syncNotificationJob";
    private static final int CHUNK_SIZE = 5;
    private static final int FETCH_SIZE = 100;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job syncNotificationJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(syncNotificationStep())
                .build();
    }

    @Bean
    public Step syncNotificationStep() {
        return stepBuilderFactory.get("syncNotificationStep")
                .<Member, Notification>chunk(CHUNK_SIZE)
                .reader(syncDormantMemberReader())
                .processor(new NotificationComposeProcessor())   // 상태 없는 순수 가공기 — new로 충분
                .writer(syncNotificationWriter())
                .build();
    }

    /** 커서 리더 OK — 이 Step은 단일 스레드다 (Step 14의 멀티스레드와 다른 점!) */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Member> syncDormantMemberReader() {
        return new JdbcCursorItemReaderBuilder<Member>()
                .name("syncDormantMemberReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, name, email, status, last_login_at, dormant_at "
                        + "FROM member WHERE status = 'DORMANT' ORDER BY member_id")
                .rowMapper(new MemberRowMapper())
                .fetchSize(FETCH_SIZE)   // 튜닝 ①: 행 단위 DB 왕복 방지
                .build();
    }

    /** 튜닝 ②: chunk 단위 JDBC batch INSERT — 건당 INSERT 왕복 방지 */
    @Bean
    public JdbcBatchItemWriter<Notification> syncNotificationWriter() {
        return new JdbcBatchItemWriterBuilder<Notification>()
                .dataSource(dataSource)
                .sql("INSERT INTO notification_history (member_id, message) VALUES (:memberId, :message)")
                .beanMapped()
                .build();
    }
}
