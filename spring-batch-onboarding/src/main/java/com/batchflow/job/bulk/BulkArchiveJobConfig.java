package com.batchflow.job.bulk;

import com.batchflow.domain.BulkRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 심화 Step 19: 대량 아카이브 Job — 성능 실습의 측정 대상.
 *
 * bulk_member 전량을 읽어 bulk_archive로 적재하는 단순한 Job이다 —
 * 일부러 단순하다: 실습의 변인은 비즈니스가 아니라 **chunkSize 하나**니까.
 *
 * chunkSize가 JobParameter다 (@JobScope Step + Late Binding, Step 3의 기술!):
 * 같은 Job을 chunk 100으로도, 2000으로도 돌려 "커밋 횟수와 시간"을 비교한다.
 * chunk = 트랜잭션 경계(Step 6) — chunkSize는 곧 커밋 횟수의 설계다:
 *   커밋 수 ≈ ceil(N / chunkSize) — 너무 잘게 = 커밋 오버헤드,
 *   너무 크게 = 트랜잭션 메모리/롤백 비용과 재시작 단위의 비대화.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BulkArchiveJobConfig {

    private static final String JOB_NAME = "bulkArchiveJob";
    private static final int FETCH_SIZE = 1000;   // 읽기 왕복 튜닝 (Step 7/16의 그 fetchSize)

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job bulkArchiveJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(bulkArchiveStep(null))
                .build();
    }

    /** chunkSize를 실행 시점에 주입 — 빌드 시 고정이 아니라 실험 가능한 파라미터로 */
    @Bean
    @JobScope
    public Step bulkArchiveStep(@Value("#{jobParameters['chunkSize']}") Long chunkSize) {
        return stepBuilderFactory.get("bulkArchiveStep")
                .<BulkRecord, BulkRecord>chunk(chunkSize.intValue())
                .reader(bulkMemberReader())
                .writer(bulkArchiveWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<BulkRecord> bulkMemberReader() {
        return new JdbcCursorItemReaderBuilder<BulkRecord>()
                .name("bulkMemberReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, name FROM bulk_member ORDER BY member_id")
                .fetchSize(FETCH_SIZE)
                .rowMapper((rs, rowNum) -> BulkRecord.builder()
                        .memberId(rs.getLong("member_id"))
                        .name(rs.getString("name"))
                        .build())
                .saveState(false)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<BulkRecord> bulkArchiveWriter() {
        return new JdbcBatchItemWriterBuilder<BulkRecord>()
                .dataSource(dataSource)
                .sql("INSERT INTO bulk_archive (member_id, name) VALUES (:memberId, :name)")
                .beanMapped()
                .build();
    }
}
