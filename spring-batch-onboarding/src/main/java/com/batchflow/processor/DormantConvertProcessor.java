package com.batchflow.processor;

import com.batchflow.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDateTime;

/**
 * Step 9: 변환 프로세서 — 후보를 휴면(DORMANT) 상태로 바꾼다
 *
 * 전환 시각(dormantAt)을 생성자로 주입받는 이유:
 * 내부에서 LocalDateTime.now()를 부르면 테스트가 "지금"에 묶여 비결정적이 된다.
 * 시각을 주입받으면 테스트는 고정 시각으로 결정적으로 검증한다 —
 * "설정 주입이 테스트 가능성을 만든다" (TestCraft JWT Step의 교훈 재사용).
 */
@Slf4j
public class DormantConvertProcessor implements ItemProcessor<Member, Member> {

    private final LocalDateTime dormantAt;

    public DormantConvertProcessor(LocalDateTime dormantAt) {
        this.dormantAt = dormantAt;
    }

    @Override
    public Member process(Member member) {
        member.setStatus(Member.STATUS_DORMANT);
        member.setDormantAt(dormantAt);
        log.debug(">>>>> [DEBUG] 휴면 전환: memberId={}, name={}",
                member.getMemberId(), member.getName());
        return member;
    }
}
