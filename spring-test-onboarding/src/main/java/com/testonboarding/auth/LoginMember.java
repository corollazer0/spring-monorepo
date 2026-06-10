package com.testonboarding.auth;

import com.testonboarding.member.domain.Member;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

/**
 * Spring Security가 인증 후 세션에 보관하는 사용자 정보.
 *
 * Security의 User(UserDetails)를 상속해 우리 도메인 정보(memberId, nickname)를 추가했다.
 * 권한은 "ROLE_" 접두사 + Role 이름 — hasRole("ADMIN")은 내부적으로 "ROLE_ADMIN"을 찾는다.
 */
@Getter
public class LoginMember extends User {

    private final Long memberId;
    private final String nickname;

    public LoginMember(Member member) {
        super(member.getUsername(),
                member.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())));
        this.memberId = member.getMemberId();
        this.nickname = member.getNickname();
    }
}
