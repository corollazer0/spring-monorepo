package com.batchflow.processor;

import com.batchflow.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Step 12 교보재: 장애 주입 스위치 — "죽음을 연습"하기 위한 학습 장치.
 *
 * ⚠️ 운영 코드에 이런 정적 스위치를 두면 절대 안 된다!
 * 재시작(Restart)을 가르치려면 "통제된 죽음"이 필요해서 만든 장치다:
 * 스위치 ON + 특정 회원 도달 → 예외 → Job FAILED → 스위치 OFF → 재시작 관찰.
 */
@Slf4j
public class SabotageProcessor implements ItemProcessor<Member, Member> {

    /** 테스트가 켜고 끄는 장애 스위치 (기본 OFF) */
    public static final AtomicBoolean SABOTAGE_ON = new AtomicBoolean(false);

    private final long sabotageMemberId;

    public SabotageProcessor(long sabotageMemberId) {
        this.sabotageMemberId = sabotageMemberId;
    }

    @Override
    public Member process(Member member) {
        if (SABOTAGE_ON.get() && member.getMemberId() == sabotageMemberId) {
            log.error(">>>>> [ERROR] 주입된 장애 발생! memberId={}", sabotageMemberId);
            throw new IllegalStateException("주입된 장애: memberId=" + sabotageMemberId);
        }
        return member;
    }
}
