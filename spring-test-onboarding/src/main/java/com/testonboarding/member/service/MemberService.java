package com.testonboarding.member.service;

import com.testonboarding.common.exception.DuplicateUsernameException;
import com.testonboarding.member.dao.MemberDao;
import com.testonboarding.member.domain.Member;
import com.testonboarding.member.domain.Role;
import com.testonboarding.member.dto.MemberSignupRequest;
import com.testonboarding.member.policy.PasswordPolicyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 회원 서비스 — Step 2 연습문제(exercise) 대상.
 *
 * 핵심 비즈니스 규칙:
 * 1. 중복 아이디 가입 불가 → DuplicateUsernameException
 * 2. 비밀번호는 정책(Step 1) 통과 후, 반드시 인코딩해서 저장
 * 3. 신규 가입자의 권한은 항상 USER
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;

    /** 순수 자바 정책 검증기 — 의존성이 없으므로 주입 대신 직접 생성 */
    private final PasswordPolicyValidator passwordPolicyValidator = new PasswordPolicyValidator();

    public Long signup(MemberSignupRequest request) {
        if (memberDao.findByUsername(request.getUsername()) != null) {
            throw new DuplicateUsernameException(request.getUsername());
        }

        passwordPolicyValidator.validate(request.getPassword());

        Member member = Member.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER)
                .build();

        memberDao.insert(member);
        log.info(">>>>> [MemberService] 회원가입 완료. username={}", request.getUsername());
        return member.getMemberId();
    }

    public Member getByUsername(String username) {
        return memberDao.findByUsername(username);
    }
}
