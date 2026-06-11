package com.batchflow.job.dormant;

import com.batchflow.domain.Member;
import com.batchflow.domain.MemberRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step 8: JdbcPagingItemReader — 끊어 읽기
 *
 * 커서(Step 7)의 약점: 처리 내내 커넥션/커서를 점유하고, thread-safe하지 않다.
 *
 * 페이징 방식: "1페이지 주세요" → 끊고 → "2페이지 주세요" — 매 페이지가 독립 쿼리.
 * 커넥션을 오래 잡지 않고, **thread-safe**라서 멀티스레드 Step(Step 14)의 전제가 된다.
 *
 * 대가: sortKeys가 "절대적으로" 중요해진다 — 페이지 사이에 순서가 흔들리면
 * 같은 행을 두 번 읽거나 빠뜨린다. (커서는 한 번 열어 흘리므로 이 문제가 없었다)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DormantCandidatePagingScanJobConfig {

    private static final String JOB_NAME = "dormantCandidatePagingScanJob";
    private static final int CHUNK_SIZE = 4;
    private static final int PAGE_SIZE = 4; // 보통 chunk size와 맞춘다

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job dormantCandidatePagingScanJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(dormantCandidatePagingScanStep())
                .build();
    }

    @Bean
    public Step dormantCandidatePagingScanStep() {
        return stepBuilderFactory.get("dormantCandidatePagingScanStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(dormantCandidatePagingReader(null))
                .writer(pagingLogWriter())
                .build();
    }

    /**
     * 페이징 SQL(OFFSET/FETCH, TOP...)은 DB마다 다르다 — 빌더가 DataSource로
     * DB 제품을 감지해 PagingQueryProvider를 골라준다.
     *
     * ⚠️ 함정: 우리 H2는 MODE=MSSQLServer지만 제품명은 "H2" → H2 Provider가 선택된다!
     * 실서버(진짜 MS-SQL)에서는 SqlServer Provider가 선택되어 "다른 SQL"이 나간다.
     * H2 테스트 통과 ≠ 실서버 페이징 SQL 검증 — 교육 문서의 Lessons Learned 참고.
     */
    @Bean
    @StepScope
    public JdbcPagingItemReader<Member> dormantCandidatePagingReader(
            @Value("#{jobParameters['cutoffDate']}") String cutoffDate) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("cutoffDate", cutoffDate);

        // 페이징의 생명선: 유일성이 보장되는 정렬 키 (PK). 없으면 중복/누락 읽기!
        Map<String, Order> sortKeys = new LinkedHashMap<>();
        sortKeys.put("member_id", Order.ASCENDING);

        return new JdbcPagingItemReaderBuilder<Member>()
                .name("dormantCandidatePagingReader")
                .dataSource(dataSource)
                .pageSize(PAGE_SIZE)
                .selectClause("SELECT member_id, name, email, status, last_login_at, dormant_at")
                .fromClause("FROM member")
                .whereClause("WHERE status = 'ACTIVE' AND last_login_at < :cutoffDate")
                .parameterValues(parameters)
                .sortKeys(sortKeys)
                .rowMapper(new MemberRowMapper())
                .build();
    }

    @Bean
    public ItemWriter<Member> pagingLogWriter() {
        return items -> log.info(">>>>> [페이징 스캔] {}건: {}",
                items.size(),
                items.stream().map(Member::getMemberId).collect(Collectors.toList()));
    }
}
