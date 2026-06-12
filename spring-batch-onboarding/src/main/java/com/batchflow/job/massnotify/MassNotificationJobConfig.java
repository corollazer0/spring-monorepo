package com.batchflow.job.massnotify;

import com.batchflow.domain.Member;
import com.batchflow.domain.MemberRowMapper;
import com.batchflow.domain.Notification;
import com.batchflow.partitioner.MemberIdRangePartitioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 심화 Step 18 — 제2 캡스톤: 대량 알림 발송 Job (50-Step 47~50 압축)
 *
 * 캠페인 파라미터를 받아 ACTIVE 회원 전원(30명)에게 개인화 알림을 발송하고
 * notification_history에 적재한다. 심화 트랙의 무기 종합:
 *
 *   파티셔닝(15) : ID 범위 3분할 — 워커마다 자기 리더 (MemberIdRangePartitioner 재사용)
 *   Skip(11)     : 1명의 발송 실패가 전체를 막지 않는다 — skip + 한도 + 리스너 기록
 *   자연 멱등(12) : reader의 NOT EXISTS — 이미 발송된 회원은 다시 읽히지조차 않는다
 *                  (스킵된 회원만 재발송되는 "실패 복구" 시나리오가 공짜로 따라온다)
 *
 * 설계 메모 — 왜 Async(16)가 아니라 Partitioning(15)인가:
 * skip은 "어느 item이 실패했나"를 알아야 하는데, AsyncItemProcessor는 실패가
 * Future 안에 숨었다가 쓰기 시점(언래핑)에 터진다 — skip 의미론이 흐려진다.
 * 부분 실패 허용이 핵심 요구인 이 Job에선 파티셔닝(워커 안은 동기)이 맞는 도구다.
 *
 * 멱등 키 = 메시지 접두 '캠페인명: ' (콜론 구분).
 * ⚠️ 대괄호([campaign]) 접두는 금지 — MS-SQL의 LIKE에서 [ ]는 와일드카드라서
 * H2 테스트는 통과하고 실서버에서 깨진다 (mybatis-mssql 스킬의 방언 함정).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MassNotificationJobConfig {

    private static final String JOB_NAME = "massNotificationJob";
    private static final int CHUNK_SIZE = 10;
    private static final int GRID_SIZE = 3;
    private static final int SKIP_LIMIT = 5;   // 이걸 넘으면 "개별 문제"가 아니라 "시스템 문제"다

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    @Bean
    public Job massNotificationJob() {
        return jobBuilderFactory.get(JOB_NAME)
                .start(massNotifyManagerStep())
                .build();
    }

    @Bean
    public Step massNotifyManagerStep() {
        return stepBuilderFactory.get("massNotifyManagerStep")
                .partitioner("massNotifyWorkerStep", massNotifyPartitioner())
                .step(massNotifyWorkerStep())
                .gridSize(GRID_SIZE)
                .taskExecutor(new SimpleAsyncTaskExecutor("notify-"))
                .build();
    }

    /** Step 15의 파티셔너 그대로 재사용 — 검증된 부품은 다시 만들지 않는다 */
    @Bean
    public MemberIdRangePartitioner massNotifyPartitioner() {
        return new MemberIdRangePartitioner(new JdbcTemplate(dataSource));
    }

    @Bean
    public Step massNotifyWorkerStep() {
        return stepBuilderFactory.get("massNotifyWorkerStep")
                .<Member, Notification>chunk(CHUNK_SIZE)
                .reader(massNotifyTargetReader(null, null, null))
                .processor(massNotifyComposeProcessor(null))
                .writer(massNotifyHistoryWriter())
                .faultTolerant()
                .skip(NotificationSendException.class)   // 발송 실패는 그 회원만의 문제
                .skipLimit(SKIP_LIMIT)
                .listener(massNotifySkipListener())
                .build();
    }

    /**
     * 대상자 리더 — 세 겹의 WHERE가 곧 요구사항이다:
     * ① status='ACTIVE'         : 탈퇴/휴면 회원에게 마케팅 발송 = 사고 (대상자 필터)
     * ② BETWEEN minId AND maxId : 파티셔너가 배포한 내 몫의 범위
     * ③ NOT EXISTS (...)        : 이 캠페인으로 이미 발송된 회원 제외 — 자연 멱등의 심장
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Member> massNotifyTargetReader(
            @Value("#{jobParameters['campaign']}") String campaign,
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId) {
        return new JdbcCursorItemReaderBuilder<Member>()
                .name("massNotifyTargetReader")
                .dataSource(dataSource)
                .sql("SELECT member_id, name, email, status, last_login_at, dormant_at "
                        + "FROM member m "
                        + "WHERE m.status = 'ACTIVE' "
                        + "  AND m.member_id BETWEEN ? AND ? "
                        + "  AND NOT EXISTS (SELECT 1 FROM notification_history h "
                        + "                  WHERE h.member_id = m.member_id AND h.message LIKE ?) "
                        + "ORDER BY m.member_id")
                .preparedStatementSetter(ps -> {
                    ps.setLong(1, minId);
                    ps.setLong(2, maxId);
                    ps.setString(3, campaign + ":%");
                })
                .rowMapper(new MemberRowMapper())
                .saveState(false)   // 파티션 워커는 범위 재실행 설계 — 위치 저장 불요
                .build();
    }

    /** 개인화 메시지 조립 + 발송 — 발송 실패는 NotificationSendException으로 (→ skip) */
    @Bean
    @StepScope
    public ItemProcessor<Member, Notification> massNotifyComposeProcessor(
            @Value("#{jobParameters['campaign']}") String campaign) {
        MarketingNotificationSender sender = marketingNotificationSender();
        return member -> {
            String message = campaign + ": " + member.getName() + "님, 고객님만을 위한 혜택이 도착했습니다";
            sender.send(member, message);   // 여기서 실패하면 이 회원만 skip — 적재도 안 된다
            return Notification.builder()
                    .memberId(member.getMemberId())
                    .message(message)
                    .build();
        };
    }

    @Bean
    public MarketingNotificationSender marketingNotificationSender() {
        return new MarketingNotificationSender();
    }

    /** 발송 이력 적재 — 이 INSERT가 다음 실행의 NOT EXISTS 조건이 된다 (멱등의 양면) */
    @Bean
    public JdbcBatchItemWriter<Notification> massNotifyHistoryWriter() {
        return new JdbcBatchItemWriterBuilder<Notification>()
                .dataSource(dataSource)
                .sql("INSERT INTO notification_history (member_id, message) VALUES (:memberId, :message)")
                .beanMapped()
                .build();
    }

    /** 기록 없는 Skip = 데이터 증발 (Step 11의 규약) — 누구를 왜 건너뛰었는지 남긴다 */
    @Bean
    public SkipListener<Member, Notification> massNotifySkipListener() {
        return new SkipListener<Member, Notification>() {
            @Override
            public void onSkipInRead(Throwable t) {
                log.warn(">>>>> [SKIP-READ] 읽기 중 건너뜀: {}", t.getMessage());
            }

            @Override
            public void onSkipInProcess(Member member, Throwable t) {
                // 실무라면 실패 테이블 INSERT — 사후 보정(재발송)의 근거
                log.warn(">>>>> [SKIP] 발송 제외. memberId={}, 사유: {}", member.getMemberId(), t.getMessage());
            }

            @Override
            public void onSkipInWrite(Notification item, Throwable t) {
                log.warn(">>>>> [SKIP-WRITE] 적재 중 건너뜀. memberId={}", item.getMemberId());
            }
        };
    }
}
