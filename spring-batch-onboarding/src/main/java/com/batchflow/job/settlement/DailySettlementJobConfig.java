package com.batchflow.job.settlement;

import com.batchflow.domain.DailyTxSummary;
import com.batchflow.domain.Settlement;
import com.batchflow.processor.SettlementProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDate;

/**
 * Step 13 캡스톤: 일일 정산 Job — 필수 트랙의 종합 과제 (프로덕션 제공)
 *
 * 구조 (요구사항: FOR-BatchFlow-Step13-Requirements.md):
 *   clearStep      : 해당 날짜의 기존 정산 삭제 — 보정 재실행 안전 장치(설계된 멱등성)
 *   settlementStep : 거래 집계(GROUP BY 리더) → 순액 계산(프로세서) → INSERT(라이터)
 *
 * 학습자는 이 Job의 테스트 전략을 스스로 설계해 작성한다 —
 * 무엇을 단위로(계산 규칙), 무엇을 단독으로(집계 리더), 무엇을 통합으로(전체+멱등성)?
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DailySettlementJobConfig {

    private static final String JOB_NAME = "dailySettlementJob";
    private static final int CHUNK_SIZE = 3;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job dailySettlementJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(settlementClearStep(null))   // 1. 비우고
                .next(settlementStep())             // 2. 채운다
                .build();
    }

    /**
     * 같은 날짜의 기존 정산을 지운다 — "재실행하면 중복 INSERT"를 설계로 차단.
     * (Step 10의 자연 멱등성과 또 다른 방식: 이쪽은 "지우고 다시 만드는" 명시적 멱등성)
     */
    @Bean
    @JobScope
    public Step settlementClearStep(@Value("#{jobParameters['targetDate']}") String targetDate) {
        return stepBuilderFactory.get("settlementClearStep")
                .tasklet((contribution, chunkContext) -> {
                    int deleted = new JdbcTemplate(dataSource).update(
                            "DELETE FROM settlement WHERE settlement_date = ?", targetDate);
                    log.info(">>>>> [정산] {} 기존 정산 {}건 삭제 (재실행 안전)", targetDate, deleted);
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step settlementStep() {
        return stepBuilderFactory.get("settlementStep")
                .<DailyTxSummary, Settlement>chunk(CHUNK_SIZE)
                .reader(dailyTxSummaryReader(null))
                .processor(settlementProcessor(null))
                .writer(settlementWriter())
                .build();
    }

    /**
     * 집계는 DB에서(GROUP BY) — "거를 수 있으면 SQL에서"(Step 7)의 집계 버전.
     * 자바에서 건건이 모아 집계하는 것보다 압도적으로 빠르고 단순하다.
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<DailyTxSummary> dailyTxSummaryReader(
            @Value("#{jobParameters['targetDate']}") String targetDate) {
        return new JdbcCursorItemReaderBuilder<DailyTxSummary>()
                .name("dailyTxSummaryReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, " +
                        "  SUM(CASE WHEN tx_type = 'DEPOSIT'  THEN amount ELSE 0 END) AS total_deposit, " +
                        "  SUM(CASE WHEN tx_type = 'WITHDRAW' THEN amount ELSE 0 END) AS total_withdraw, " +
                        "  COUNT(*) AS tx_count " +
                        "FROM bank_transaction " +
                        "WHERE transaction_date = ? " +
                        "GROUP BY member_id " +
                        "ORDER BY member_id")
                .preparedStatementSetter(ps -> ps.setString(1, targetDate))
                .rowMapper((rs, rowNum) -> DailyTxSummary.builder()
                        .memberId(rs.getLong("member_id"))
                        .totalDeposit(rs.getLong("total_deposit"))
                        .totalWithdraw(rs.getLong("total_withdraw"))
                        .txCount(rs.getInt("tx_count"))
                        .build())
                .saveState(false) // clearStep 선행 + 전체 재실행 정책 — 위치 저장 불필요(Step 12)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<DailyTxSummary, Settlement> settlementProcessor(
            @Value("#{jobParameters['targetDate']}") String targetDate) {
        return new SettlementProcessor(LocalDate.parse(targetDate));
    }

    @Bean
    public JdbcBatchItemWriter<Settlement> settlementWriter() {
        return new JdbcBatchItemWriterBuilder<Settlement>()
                .dataSource(dataSource)
                .sql("INSERT INTO settlement " +
                        "(settlement_date, member_id, total_deposit, total_withdraw, net_amount, transaction_count) " +
                        "VALUES (:settlementDate, :memberId, :totalDeposit, :totalWithdraw, :netAmount, :transactionCount)")
                .beanMapped()
                .build();
    }
}
