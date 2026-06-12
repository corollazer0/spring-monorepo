package com.batchflow.advanced.step18.answer;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.massnotify.MarketingNotificationSender;
import com.batchflow.job.massnotify.MassNotificationJobConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 18 — answer] 대량 알림 발송 모범 스위트 — 캡스톤 평가 기준의 구현체
 *
 * 시나리오 5종이 곧 요구사항 검증이다:
 *   전량 발송(파티션 분배 포함) / 재실행 멱등 / 캠페인 분리 /
 *   부분 실패 skip → 실패 복구(자연 멱등의 보상!) / skip 한도 초과 = 시스템 문제
 */
@SpringBatchTest
@SpringBootTest(classes = {MassNotificationJobConfig.class, TestBatchConfig.class})
@DisplayName("대량 알림 발송 Job (모범답안)")
class MassNotificationJobAnswerTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        jdbcTemplate.update("DELETE FROM notification_history");
        MarketingNotificationSender.FAIL_MEMBER_IDS.clear();   // 교보재 정리 의무
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM notification_history");
        MarketingNotificationSender.FAIL_MEMBER_IDS.clear();
    }

    /**
     * campaign + 유니크 run — 같은 캠페인의 "다른 실행"을 만든다 (Step 3 규칙).
     *
     * ⚠️ 헬퍼가 JobParameters를 반환하는 이유: @SpringBatchTest의
     * JobScopeTestExecutionListener는 "JobExecution을 반환하는 아무 메서드"를
     * 잡 스코프 팩토리로 오인해 인자 없이 호출하려다 전 테스트를 깨뜨린다 —
     * JobExecution 반환 헬퍼 금지!
     */
    private JobParameters campaignParams(String campaign) {
        return new JobParametersBuilder()
                .addString("campaign", campaign)
                .addLong("run", System.nanoTime())
                .toJobParameters();
    }

    private Integer countHistory() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification_history", Integer.class);
    }

    private long workerReadSum(JobExecution execution) {
        return execution.getStepExecutions().stream()
                .filter(se -> se.getStepName().startsWith("massNotifyWorkerStep:"))
                .mapToLong(StepExecution::getReadCount)
                .sum();
    }

    @Test
    @DisplayName("전량 발송: ACTIVE 30명 전원, 워커 3분할, 개인화 메시지 — 비대상 0건")
    void massNotificationJob_첫발송_전량과분배() throws Exception {
        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(campaignParams("SUMMER"));

        // then-1 : 완료 + 파티션 구조 (manager 1 + worker 3)
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).hasSize(4);
        assertThat(workerReadSum(execution)).isEqualTo(30);   // 3 워커의 합 = 전체 대상

        // then-2 : DB 적재 30건, 수신자 전원이 ACTIVE (JOIN 교차 — 발송 사고 봉인)
        assertThat(countHistory()).isEqualTo(30);
        Integer activeMatched = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_history h "
                        + "JOIN member m ON h.member_id = m.member_id AND m.status = 'ACTIVE'",
                Integer.class);
        assertThat(activeMatched).isEqualTo(30);

        // then-3 : 개인화 — 대표 1건의 실제 메시지 (캠페인 접두 + 이름)
        String message = jdbcTemplate.queryForObject(
                "SELECT message FROM notification_history WHERE member_id = 1", String.class);
        assertThat(message).startsWith("SUMMER: ").contains("회원01님");
    }

    @Test
    @DisplayName("재실행 멱등: 같은 캠페인 두 번째 실행은 읽을 것이 없다 (NOT EXISTS의 증명)")
    void massNotificationJob_같은캠페인재실행_중복발송없음() throws Exception {
        // given
        jobLauncherTestUtils.launchJob(campaignParams("AUTUMN"));
        assertThat(countHistory()).isEqualTo(30);

        // when : 같은 캠페인, 새 실행 (스케줄러 중복 트리거/운영자 재실행 시나리오)
        JobExecution second = jobLauncherTestUtils.launchJob(campaignParams("AUTUMN"));

        // then : 실패가 아니라 "0건 읽고 정상 종료" — 멱등은 조용하다
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(workerReadSum(second)).isZero();
        assertThat(countHistory()).isEqualTo(30);
    }

    @Test
    @DisplayName("캠페인 분리: 멱등 키는 캠페인 단위 — 다른 캠페인은 새로 발송된다")
    void massNotificationJob_다른캠페인_별도발송() throws Exception {
        // given
        jobLauncherTestUtils.launchJob(campaignParams("EVENT-A"));

        // when
        JobExecution second = jobLauncherTestUtils.launchJob(campaignParams("EVENT-B"));

        // then : A 30건 + B 30건
        assertThat(workerReadSum(second)).isEqualTo(30);
        assertThat(countHistory()).isEqualTo(60);
    }

    @Test
    @DisplayName("부분 실패 → 복구: 2명 skip 후 재실행하면 '그 2명만' 발송된다 (자연 멱등의 보상)")
    void massNotificationJob_부분실패후재실행_실패자만복구() throws Exception {
        // given : 회원 2, 7의 발송이 실패하는 상황 (수신 불가 주소)
        MarketingNotificationSender.FAIL_MEMBER_IDS.addAll(Arrays.asList(2L, 7L));

        // when-1 : 그래도 Job은 완주한다 — 2명의 문제가 28명을 막지 않는다 (skip!)
        JobExecution first = jobLauncherTestUtils.launchJob(campaignParams("VIP"));

        // then-1 : COMPLETED + 28건 적재 + skip 2 집계 + 실패자는 미적재
        // (주의: manager Step이 워커 카운트를 "집계"해 들고 있다 — 전체 합산하면 이중 계산!
        //  카운트 검증은 워커만 필터해서 — workerReadSum과 같은 이유)
        assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(countHistory()).isEqualTo(28);
        long skipSum = first.getStepExecutions().stream()
                .filter(se -> se.getStepName().startsWith("massNotifyWorkerStep:"))
                .mapToLong(StepExecution::getProcessSkipCount).sum();
        assertThat(skipSum).isEqualTo(2);
        Integer failedInserted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_history WHERE member_id IN (2, 7)", Integer.class);
        assertThat(failedInserted).isZero();

        // when-2 : 주소 문제를 고치고(스위치 해제) 같은 캠페인 재실행
        MarketingNotificationSender.FAIL_MEMBER_IDS.clear();
        JobExecution second = jobLauncherTestUtils.launchJob(campaignParams("VIP"));

        // then-2 : 이미 받은 28명은 안 읽힌다 — "실패했던 2명만" 발송 → 전원 완료
        assertThat(workerReadSum(second)).isEqualTo(2);
        assertThat(countHistory()).isEqualTo(30);
    }

    @Test
    @DisplayName("skip 한도 초과(6 > 5): 개별 문제가 아니라 시스템 문제 — Job이 실패해야 한다")
    void massNotificationJob_대량실패_한도초과로실패() throws Exception {
        // given : 6명 연속 실패 — 이 정도면 주소 몇 개가 아니라 발송 시스템이 죽은 것
        MarketingNotificationSender.FAIL_MEMBER_IDS.addAll(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));

        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(campaignParams("BROKEN-DAY"));

        // then : skipLimit이 "계속 갈 일이 아니다"를 판정한다 — 조용한 대량 누락 방지
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
    }
}
