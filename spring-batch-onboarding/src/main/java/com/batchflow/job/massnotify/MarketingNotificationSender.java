package com.batchflow.job.massnotify;

import com.batchflow.domain.Member;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 마케팅 알림 발송기 (심화 Step 18 캡스톤) — 외부 발송 시스템의 대역.
 *
 * 교보재: FAIL_MEMBER_IDS에 담긴 회원은 발송이 실패한다 (잘못된 이메일 등
 * "그 회원만의" 문제 시뮬레이션). 정적 스위치 규약(SEEN_THREADS/BROKEN과 동일):
 * 운영 코드 반입 금지 + 테스트에서 Before/AfterEach 정리 의무.
 */
@Slf4j
public class MarketingNotificationSender {

    /** 교보재: 발송이 실패할 회원 ID 집합 — 비어 있으면 전부 성공 (운영 금지!) */
    public static final Set<Long> FAIL_MEMBER_IDS = ConcurrentHashMap.newKeySet();

    public void send(Member member, String message) {
        if (FAIL_MEMBER_IDS.contains(member.getMemberId())) {
            throw new NotificationSendException(member.getMemberId(), "수신 불가 주소 (시뮬레이션)");
        }
        log.debug(">>>>> [Sender] 발송 완료. memberId={}, message={}", member.getMemberId(), message);
    }
}
