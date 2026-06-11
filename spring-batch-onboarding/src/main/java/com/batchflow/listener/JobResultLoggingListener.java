package com.batchflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Step 11: Job 수명주기 리스너 — "알림 발송 지점"의 표준 위치
 *
 * 실무에서는 afterJob에서 Slack/메일 알림을 쏜다.
 * 여기서는 알림 대신 ExecutionContext에 기록을 남겨 "리스너가 호출되었고
 * 최종 상태를 알고 있었다"를 테스트가 검증할 수 있게 한다.
 */
@Slf4j
public class JobResultLoggingListener implements JobExecutionListener {

    public static final String KEY_NOTIFIED_STATUS = "notifiedStatus";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>>>> [Listener] Job 시작: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // 실무라면 여기서: if (FAILED) slack.send(...) — 알림의 표준 위치
        String status = jobExecution.getStatus().name();
        jobExecution.getExecutionContext().putString(KEY_NOTIFIED_STATUS, status);
        log.info(">>>>> [Listener] Job 종료: {} → {} (여기가 알림 발송 지점)",
                jobExecution.getJobInstance().getJobName(), status);
    }
}
