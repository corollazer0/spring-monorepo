package com.batchflow.job.massnotify;

/**
 * 알림 발송 실패 (심화 Step 18 캡스톤) — skip 대상 예외.
 *
 * 잘못된 이메일, 수신 거부 등 "그 회원만의" 문제 — 1명의 실패가
 * 나머지 29명의 발송을 막아선 안 된다 (faultTolerant skip의 무대).
 */
public class NotificationSendException extends RuntimeException {

    public NotificationSendException(Long memberId, String reason) {
        super("알림 발송 실패. memberId=" + memberId + ", 사유: " + reason);
    }
}
