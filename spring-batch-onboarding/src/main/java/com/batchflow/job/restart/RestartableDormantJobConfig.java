package com.batchflow.job.restart;

import com.batchflow.domain.Member;
import com.batchflow.domain.MemberRowMapper;
import com.batchflow.processor.DormantConvertProcessor;
import com.batchflow.processor.SabotageProcessor;
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
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Step 12: 재시작 가능한 휴면 전환 Job — 죽었다 살아나기
 *
 * Step 10의 Job과 같은 일을 하지만, 장애 주입 스위치(SabotageProcessor)가 끼어 있어
 * "도중에 죽는 시나리오"를 통제할 수 있다.
 *
 * 🔑 재시작 설계의 핵심 결정 — saveState(false):
 * 우리 쿼리는 WHERE status='ACTIVE' — 처리할수록 대상이 줄어드는 "상태 전이 쿼리"다.
 * 커서 리더의 기본 동작(읽은 위치를 EC에 저장, 재시작 시 그만큼 건너뜀)과 만나면
 * 대참사가 난다: 재시작 시 남은 6명에서 또 4명을 건너뛰어 2명만 처리!
 * → 상태 전이 쿼리에는 위치 저장을 끄고(saveState false), WHERE가 멱등성을 책임지게 한다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestartableDormantJobConfig {

    private static final String JOB_NAME = "restartableDormantJob";
    private static final int CHUNK_SIZE = 4;
    public static final long SABOTAGE_TARGET_MEMBER_ID = 26L; // chunk2(25~28)에서 죽는다

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job restartableDormantJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(restartableDormantStep())
                .build();
    }

    @Bean
    public Step restartableDormantStep() {
        return stepBuilderFactory.get("restartableDormantStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(restartableDormantReader(null))
                .processor(restartableDormantProcessor(null))
                .writer(restartableDormantWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<Member> restartableDormantReader(
            @Value("#{jobParameters['cutoffDate']}") String cutoffDate) {
        return new JdbcCursorItemReaderBuilder<Member>()
                .name("restartableDormantReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, name, email, status, last_login_at, dormant_at " +
                        "FROM member " +
                        "WHERE status = 'ACTIVE' AND last_login_at < ? " +
                        "ORDER BY member_id")
                .preparedStatementSetter(ps -> ps.setString(1, cutoffDate))
                .rowMapper(new MemberRowMapper())
                .saveState(false) // 🔑 상태 전이 쿼리 — 위치 저장 OFF, 멱등성은 WHERE가 책임진다
                .build();
    }

    @Bean
    @StepScope
    public CompositeItemProcessor<Member, Member> restartableDormantProcessor(
            @Value("#{jobParameters['dormantAt']}") String dormantAt) {
        CompositeItemProcessor<Member, Member> composite = new CompositeItemProcessor<>();
        composite.setDelegates(Arrays.asList(
                new SabotageProcessor(SABOTAGE_TARGET_MEMBER_ID), // 교보재: 통제된 죽음
                new DormantConvertProcessor(LocalDateTime.parse(dormantAt))));
        return composite;
    }

    @Bean
    public JdbcBatchItemWriter<Member> restartableDormantWriter() {
        return new JdbcBatchItemWriterBuilder<Member>()
                .dataSource(dataSource)
                .sql("UPDATE member SET status = :status, dormant_at = :dormantAt " +
                        "WHERE member_id = :memberId")
                .beanMapped()
                .build();
    }
}
