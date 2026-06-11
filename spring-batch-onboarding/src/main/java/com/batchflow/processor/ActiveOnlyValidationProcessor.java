package com.batchflow.processor;

import com.batchflow.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * Step 9: 검증 프로세서 — ACTIVE가 아닌 회원은 걸러낸다 (null 반환 = 필터)
 *
 * "리더의 WHERE가 이미 거르는데 왜 또?" — 심층 방어다.
 * 리더 SQL이 수정되거나 다른 리더와 조합될 때, 이미 DORMANT인 회원을
 * 또 전환하는 사고를 프로세서가 한 번 더 막는다.
 *
 * 순수 자바 클래스 — Spring 없이 new 해서 단위 테스트한다 (TestCraft Step 1~2의 철학).
 */
@Slf4j
public class ActiveOnlyValidationProcessor implements ItemProcessor<Member, Member> {

    @Override
    public Member process(Member member) {
        if (!Member.STATUS_ACTIVE.equals(member.getStatus())) {
            log.warn(">>>>> [WARN] ACTIVE 아님 — 필터링: memberId={}, status={}",
                    member.getMemberId(), member.getStatus());
            return null; // 필터 (FILTER_COUNT) — 예외가 아니다
        }
        return member;
    }
}
