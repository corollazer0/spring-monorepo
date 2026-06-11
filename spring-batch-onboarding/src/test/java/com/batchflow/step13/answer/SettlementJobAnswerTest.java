package com.batchflow.step13.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.settlement.DailySettlementJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 13 — answer] 정산 Job 통합 테스트
 *
 * 전략: 통합에서는 "조립 + 실제 INSERT 값 + 재실행 멱등성 + 빈 날짜"만 본다.
 * 계산 규칙의 케이스는 SettlementProcessorAnswerTest(단위)가 이미 다 했다.
 */
@SpringBatchTest
@SpringBootTest(classes = {DailySettlementJobConfig.class, TestBatchConfig.class})
@DisplayName("일일 정산 Job (캡스톤 모범답안)")
class SettlementJobAnswerTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        // 배치는 진짜 커밋한다(Step 10) — 정산 결과 원상복구
        jdbcTemplate.update("DELETE FROM settlement");
    }

    private JobParameters params(String targetDate, String run) {
        return new JobParametersBuilder()
                .addString("targetDate", targetDate)
                .addString("run", run) // 보정 재실행(다른 인스턴스) 시뮬레이션용
                .toJobParameters();
    }

    @Test
    @DisplayName("06-10 정산: 5명 적재 + 대표 값(음수 순액 포함) 정확성")
    void settlementJob_기준일_5명정산() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params("2026-06-10", "r1"));

        // then(1) : 시드 기준값 — 정산 대상 5명 (Step 6에서 봉인)
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(settlementCount("2026-06-10")).isEqualTo(5);

        // then(2) : 대표 행의 "실제 INSERT된 값" — 카운트는 적재까지만 증명한다
        Map<String, Object> m1 = jdbcTemplate.queryForMap(
                "SELECT total_deposit, total_withdraw, net_amount, transaction_count " +
                        "FROM settlement WHERE settlement_date = '2026-06-10' AND member_id = 1");
        assertThat(((Number) m1.get("NET_AMOUNT")).longValue()).isEqualTo(25000L);
        assertThat(((Number) m1.get("TRANSACTION_COUNT")).intValue()).isEqualTo(3);

        // 음수 순액(m3)이 DB까지 그대로 — 절대값 보정 같은 사고가 없었다는 증명
        Long m3Net = jdbcTemplate.queryForObject(
                "SELECT net_amount FROM settlement " +
                        "WHERE settlement_date = '2026-06-10' AND member_id = 3", Long.class);
        assertThat(m3Net).isEqualTo(-10000L);
    }

    @Test
    @DisplayName("보정 재실행해도 정산은 중복되지 않는다 — clearStep의 존재 이유")
    void settlementJob_보정재실행_중복없음() throws Exception {
        // given : 1차 정산 완료 (5건)
        jobLauncherTestUtils.launchJob(params("2026-06-10", "r1"));

        // when : 데이터 보정 후 같은 날짜를 다시 정산 (다른 run 파라미터 = 새 인스턴스)
        jobLauncherTestUtils.launchJob(params("2026-06-10", "r2"));

        // then : clearStep이 기존 5건을 지우고 다시 만들었다 — 10건이 아니라 5건!
        assertThat(settlementCount("2026-06-10")).isEqualTo(5);
    }

    @Test
    @DisplayName("거래가 없는 날짜는 0건으로 정상 종료한다")
    void settlementJob_거래없는날_0건정상() throws Exception {
        // when : 시드에 거래가 없는 날짜
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params("2026-01-01", "r1"));

        // then : "할 일 없음"은 실패가 아니다 (Step 7의 교훈)
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(settlementCount("2026-01-01")).isZero();
    }

    private Integer settlementCount(String date) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement WHERE settlement_date = ?",
                Integer.class, date);
    }
}
