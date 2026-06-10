package com.testonboarding.support.security;

import com.testonboarding.auth.LoginMember;
import com.testonboarding.member.domain.Member;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * @WithMockMember가 붙은 테스트 실행 전에 호출되어
 * LoginMember(우리 도메인 principal) 기반 SecurityContext를 만들어준다.
 */
public class WithMockMemberSecurityContextFactory
        implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {
        Member member = Member.builder()
                .memberId(annotation.memberId())
                .username(annotation.username())
                .password("{noop}test-only")
                .nickname(annotation.nickname())
                .role(annotation.role())
                .build();

        LoginMember loginMember = new LoginMember(member);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                loginMember, loginMember.getPassword(), loginMember.getAuthorities()));
        return context;
    }
}
