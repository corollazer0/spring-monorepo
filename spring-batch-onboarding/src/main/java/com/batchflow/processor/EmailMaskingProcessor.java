package com.batchflow.processor;

import com.batchflow.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * Step 9: 이메일 마스킹 프로세서 — 개인정보 보호 변환 (exercise 대상)
 *
 * 규칙:
 * - "abcdef@test.com" → "ab***@test.com" (로컬파트 앞 2자 + ***)
 * - 로컬파트가 2자 이하면 전체를 "**"로: "ab@test.com" → "**@test.com"
 * - 이메일이 null이거나 @가 없으면 그 건은 버린다(null 반환 = 필터)
 */
@Slf4j
public class EmailMaskingProcessor implements ItemProcessor<Member, Member> {

    @Override
    public Member process(Member member) {
        String email = member.getEmail();
        if (email == null || !email.contains("@")) {
            log.warn(">>>>> [WARN] 이메일 형식 불량 — 필터링: memberId={}", member.getMemberId());
            return null;
        }

        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex); // '@' 포함

        String masked = (localPart.length() <= 2)
                ? "**" + domainPart
                : localPart.substring(0, 2) + "***" + domainPart;

        member.setEmail(masked);
        return member;
    }
}
