package com.batchflow.job.dormant;

import com.batchflow.domain.Member;
import com.batchflow.domain.MemberRowMapper;
import com.batchflow.processor.ActiveOnlyValidationProcessor;
import com.batchflow.processor.DormantConvertProcessor;
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
 * Step 10: 실전 — 휴면회원 전환 Job (부품 조립)
 *
 * 조립도:
 *   reader   : 후보 조회 (Step 7의 커서 리더 패턴)
 *   processor: 검증 → 변환 Composite (Step 9의 부품들!)
 *   writer   : JdbcBatchItemWriter — chunk 묶음을 batch UPDATE 한 방에
 *
 * 파라미터:
 *   cutoffDate — 이 날짜 이전 로그인 = 후보
 *   dormantAt  — 전환 일시 (ISO: 2026-06-11T03:00:00) — now() 내장 금지(Step 9 교훈)
 *
 * 자연 멱등성: WHERE status='ACTIVE' 조건 덕분에, 이미 전환된 회원은
 * 재실행 시 아예 읽히지 않는다 — "상태 전이를 따라가는 WHERE"가 안전판.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DormantMemberJobConfig {

    private static final String JOB_NAME = "dormantMemberJob";
    private static final int CHUNK_SIZE = 4;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job dormantMemberJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(dormantMemberStep())
                .build();
    }

    @Bean
    public Step dormantMemberStep() {
        return stepBuilderFactory.get("dormantMemberStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(dormantMemberReader(null))
                .processor(dormantMemberProcessor(null))
                .writer(dormantMemberWriter())
                .build();
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<Member> dormantMemberReader(
            @Value("#{jobParameters['cutoffDate']}") String cutoffDate) {
        return new JdbcCursorItemReaderBuilder<Member>()
                .name("dormantMemberReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, name, email, status, last_login_at, dormant_at " +
                        "FROM member " +
                        "WHERE status = 'ACTIVE' AND last_login_at < ? " +
                        "ORDER BY member_id")
                .preparedStatementSetter(ps -> ps.setString(1, cutoffDate))
                .rowMapper(new MemberRowMapper())
                .build();
    }

    /**
     * Step 9에서 단위 검증을 끝낸 부품들을 체인으로 조립.
     * @Bean으로 반환하면 Spring이 afterPropertiesSet()을 자동 호출해준다
     * (Step 9의 수동 조립과 달리 — 컨테이너의 서비스).
     */
    @Bean
    @StepScope
    public CompositeItemProcessor<Member, Member> dormantMemberProcessor(
            @Value("#{jobParameters['dormantAt']}") String dormantAt) {
        CompositeItemProcessor<Member, Member> composite = new CompositeItemProcessor<>();
        composite.setDelegates(Arrays.asList(
                new ActiveOnlyValidationProcessor(),
                new DormantConvertProcessor(LocalDateTime.parse(dormantAt))));
        return composite;
    }

    /**
     * chunk 묶음(4건)을 JDBC batch UPDATE 한 방으로 — 건건이 UPDATE보다 압도적으로 빠르다.
     * beanMapped(): SQL의 :이름 파라미터를 Member의 getter와 자동 매핑.
     */
    @Bean
    public JdbcBatchItemWriter<Member> dormantMemberWriter() {
        return new JdbcBatchItemWriterBuilder<Member>()
                .dataSource(dataSource)
                .sql("UPDATE member SET status = :status, dormant_at = :dormantAt " +
                        "WHERE member_id = :memberId")
                .beanMapped()
                .build();
    }
}
