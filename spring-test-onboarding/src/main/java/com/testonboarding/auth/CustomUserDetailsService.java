package com.testonboarding.auth;

import com.testonboarding.member.dao.MemberDao;
import com.testonboarding.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 로그인 시 Spring Security가 사용자를 조회할 때 호출하는 서비스.
 *
 * 폼로그인(POST /login) → DaoAuthenticationProvider → 이 클래스의 loadUserByUsername →
 * PasswordEncoder로 비밀번호 대조 → 성공 시 LoginMember가 세션에 저장된다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberDao memberDao;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberDao.findByUsername(username);
        if (member == null) {
            throw new UsernameNotFoundException("존재하지 않는 사용자입니다: " + username);
        }
        return new LoginMember(member);
    }
}
