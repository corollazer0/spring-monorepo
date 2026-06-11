package com.batchflow.job.dormant;

import com.batchflow.domain.Member;
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
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.stream.Collectors;

/**
 * Step 7: JdbcCursorItemReader — 메모리에 안 올리고 한 건씩 흘려 읽기
 *
 * 시나리오: 휴면 전환 "후보"(ACTIVE + 기준일 이전 로그인)를 스캔한다.
 *
 * 커서 방식의 본질: SELECT를 한 번 열어두고(DB 커서) chunk가 달라는 만큼만
 * 흘려보낸다 — 10만 건이어도 메모리에는 fetchSize만큼만 머문다.
 * 수도꼭지에 비유하면, 양동이로 다 받아오는 게 아니라 꼭지를 틀어두고 컵만 대는 것.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DormantCandidateScanJobConfig {

    private static final String JOB_NAME = "dormantCandidateScanJob";
    private static final int CHUNK_SIZE = 4; // 후보 10명 → 4+4+2 = 3 chunk

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job dormantCandidateScanJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(dormantCandidateScanStep())
                .build();
    }

    @Bean
    public Step dormantCandidateScanStep() {
        return stepBuilderFactory.get("dormantCandidateScanStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(dormantCandidateCursorReader(null))
                .writer(candidateLogWriter())
                .build();
    }

    /**
     * 커서 리더 — 상태(커서 위치)를 가지므로 @StepScope 필수 (Step 6의 교훈).
     * cutoffDate(이 날짜 이전 로그인 = 후보)는 JobParameters로 — 기준일이 바뀌어도 코드 불변.
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Member> dormantCandidateCursorReader(
            @Value("#{jobParameters['cutoffDate']}") String cutoffDate) {
        return new JdbcCursorItemReaderBuilder<Member>()
                .name("dormantCandidateCursorReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, name, email, status, last_login_at, dormant_at " +
                        "FROM member " +
                        "WHERE status = 'ACTIVE' AND last_login_at < ? " +
                        "ORDER BY member_id") // 결정적 순서 — 검증과 재시작의 전제
                .preparedStatementSetter(ps -> ps.setString(1, cutoffDate))
                .rowMapper(memberRowMapper())
                .fetchSize(100) // DB가 한 번에 건네주는 묶음 크기 (메모리 상한)
                .build();
    }

    /**
     * RowMapper — ResultSet 한 행을 Member로. (MyBatis 없이 JDBC 그대로 — 배치 리더의 기본기)
     */
    @Bean
    public RowMapper<Member> memberRowMapper() {
        return (rs, rowNum) -> {
            Timestamp lastLogin = rs.getTimestamp("last_login_at");
            Timestamp dormantAt = rs.getTimestamp("dormant_at");
            return Member.builder()
                    .memberId(rs.getLong("member_id"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .status(rs.getString("status"))
                    .lastLoginAt(lastLogin == null ? null : lastLogin.toLocalDateTime())
                    .dormantAt(dormantAt == null ? null : dormantAt.toLocalDateTime())
                    .build();
        };
    }

    @Bean
    public ItemWriter<Member> candidateLogWriter() {
        return items -> log.info(">>>>> [후보 스캔] {}건: {}",
                items.size(),
                items.stream().map(Member::getName).collect(Collectors.toList()));
    }
}
