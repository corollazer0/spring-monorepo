package com.batchflow.job.partition;

import com.batchflow.domain.Member;
import com.batchflow.domain.MemberRowMapper;
import com.batchflow.partitioner.MemberIdRangePartitioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 심화 Step 15: Partitioning — 나눠서 정복
 *
 * 구조:
 *   Manager Step : Partitioner가 데이터(ID 범위)를 gridSize 조각으로 나누고
 *                  각 조각을 Worker Step의 복제본에게 배포 (병렬)
 *   Worker Step  : 자기 ExecutionContext의 minId~maxId 범위만 처리
 *
 * Multi-threaded Step(14-A)과의 차이:
 * - 14-A는 "공유 리더"를 여러 스레드가 당긴다 (thread-safe 리더 필수)
 * - 15는 워커마다 "자기만의 리더"(자기 범위) — 커서 리더도 안전! (공유가 없으니까)
 * - 구조가 원격 워커(다중 서버)로도 확장된다 — 대용량의 끝판왕
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PartitionedMemberScanJobConfig {

    private static final String JOB_NAME = "partitionedMemberScanJob";
    private static final int CHUNK_SIZE = 5;
    private static final int GRID_SIZE = 3;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job partitionedMemberScanJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(partitionManagerStep())
                .build();
    }

    /**
     * Manager: 나누고(partitioner) → 배포하고(step) → 병렬로(taskExecutor) 돌린다.
     */
    @Bean
    public Step partitionManagerStep() {
        return stepBuilderFactory.get("partitionManagerStep")
                .partitioner("partitionWorkerStep", memberIdRangePartitioner())
                .step(partitionWorkerStep())
                .gridSize(GRID_SIZE)
                .taskExecutor(new SimpleAsyncTaskExecutor("partition-"))
                .build();
    }

    @Bean
    public MemberIdRangePartitioner memberIdRangePartitioner() {
        return new MemberIdRangePartitioner(new JdbcTemplate(dataSource));
    }

    @Bean
    public Step partitionWorkerStep() {
        return stepBuilderFactory.get("partitionWorkerStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(partitionRangeReader(null, null))
                .writer(partitionLogWriter())
                .build();
    }

    /**
     * 워커의 리더 — JobParameters가 아니라 "stepExecutionContext"에서 범위를 주입받는다!
     * (파티셔너가 각 워커의 EC에 심어둔 값 — Step 5의 ExecutionContext가 배포 통로)
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Member> partitionRangeReader(
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId) {
        return new JdbcCursorItemReaderBuilder<Member>()
                .name("partitionRangeReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, name, email, status, last_login_at, dormant_at " +
                        "FROM member WHERE member_id BETWEEN ? AND ? ORDER BY member_id")
                .preparedStatementSetter(ps -> {
                    ps.setLong(1, minId);
                    ps.setLong(2, maxId);
                })
                .rowMapper(new MemberRowMapper())
                .saveState(false)
                .build();
    }

    @Bean
    public ItemWriter<Member> partitionLogWriter() {
        return items -> log.info(">>>>> [{}] {}건 처리", Thread.currentThread().getName(), items.size());
    }
}
