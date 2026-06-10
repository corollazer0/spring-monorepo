package com.testonboarding.auth.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 인증 필터 — Authorization: Bearer {token} 헤더를 해석해
 * 유효하면 SecurityContext에 인증을 심는다. (세션 없이 매 요청 인증)
 *
 * 주의: 이 필터는 "차단"하지 않는다 — 토큰이 없거나 무효면 인증을 안 심을 뿐,
 * 체인은 계속 진행시킨다. 차단(401)은 뒤의 인가 단계가 결정한다.
 * (인증 수집과 접근 결정의 책임 분리)
 *
 * Bean 등록은 SecurityConfig가 담당한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsername(token);
            String role = jwtTokenProvider.getRole(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username, null,
                            AuthorityUtils.createAuthorityList("ROLE_" + role));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug(">>>>> [DEBUG] JWT 인증 성공: username={}", username);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
