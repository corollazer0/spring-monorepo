package com.batchflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 휴면 전환 알림 (심화 Step 16) — notification_history 테이블에 대응하는 순수 자바 도메인.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long memberId;
    private String message;

    public static Notification dormantNotice(Member member) {
        return Notification.builder()
                .memberId(member.getMemberId())
                .message("휴면 안내: " + member.getName() + "님, 계정이 휴면 상태입니다. 로그인하여 해제하세요.")
                .build();
    }
}
