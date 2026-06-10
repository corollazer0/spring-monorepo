package com.testonboarding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Security 설정 — 세션 + 폼로그인 기반.
 *
 * ⚠️ WebSecurityConfigurerAdapter는 Security 5.7에서 deprecated.
 *    인터넷의 "extends WebSecurityConfigurerAdapter" 예제는 구식이다 —
 *    SecurityFilterChain @Bean 방식이 현재 표준이다.
 *
 * 접근 정책:
 * - GET /api/posts/**      : 누구나 (게시글 읽기)
 * - POST /api/members      : 누구나 (회원가입)
 * - /api/admin/**          : ADMIN 권한만
 * - 그 외 /api/**          : 인증 필요 (글쓰기/수정/삭제 등)
 *
 * 설계 노트:
 * - 여기서는 "URL 단위 접근 제어"만 담당한다.
 *   "작성자 본인만 수정/삭제" 같은 데이터 소유권 검증은 Service 계층의 책임(Step 2).
 * - CSRF는 활성 상태 유지 — 테스트에서 csrf()의 의미를 배우기 위해서다(Step 6).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API이므로 미인증 시 로그인 페이지로 redirect(302) 대신 401 응답
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // CSRF 토큰을 쿠키로 발급 (Step 8 E2E에서 사용). H2 콘솔은 예외
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringAntMatchers("/h2-console/**"))

                // H2 콘솔이 iframe을 쓰므로 같은 출처의 frame 허용
                .headers(headers -> headers.frameOptions().sameOrigin())

                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/h2-console/**", "/login", "/logout").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .antMatchers(HttpMethod.POST, "/api/members").permitAll()
                        .antMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())

                // 세션 기반 폼로그인: POST /login (username, password 폼 파라미터)
                .formLogin(form -> form.loginProcessingUrl("/login"))
                .logout(logout -> logout.logoutUrl("/logout"));

        return http.build();
    }
}
