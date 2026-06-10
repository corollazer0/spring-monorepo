package com.testonboarding.config;

import com.testonboarding.auth.jwt.JwtAuthenticationFilter;
import com.testonboarding.auth.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import javax.servlet.http.HttpServletResponse;

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

    /**
     * [심화 Step 10] JWT 빈들 — @Component 대신 여기서 등록하는 이유:
     * @WebMvcTest 슬라이스가 @Import(SecurityConfig.class) 하나로
     * 보안 설정 + JWT 필터 + 프로바이더를 전부 가져갈 수 있게 하기 위함.
     */
    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret:testcraft-jwt-secret-key-for-learning-only-do-not-use}") String secret,
            @Value("${jwt.validity-millis:3600000}") long validityMillis) {
        return new JwtTokenProvider(secret, validityMillis);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    /**
     * [심화 Step 10] /api/v2/** 전용 JWT 체인 — @Order(1)이라 먼저 매칭을 시도한다.
     *
     * 세션 체인과의 차이:
     * - STATELESS: 세션을 만들지도, 쓰지도 않는다 (매 요청 토큰으로 인증)
     * - CSRF 비활성: 세션 쿠키가 없으므로 CSRF 공격 표면 자체가 없다
     * - JwtAuthenticationFilter가 Bearer 토큰을 해석해 인증을 심는다
     *
     * @Autowired(필터)는 메서드 파라미터 주입 — JwtAuthenticationFilter가 @Component라 가능.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain jwtFilterChain(HttpSecurity http,
                                              JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .antMatcher("/api/v2/**") // 이 체인은 /api/v2/** 요청에만 적용된다
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/api/v2/auth/token").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2) // v2가 아닌 모든 요청은 이 세션 체인이 담당한다
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
                // REST API이므로 성공/실패를 redirect(302) 대신 상태코드로 응답한다 (Step 8 E2E)
                .formLogin(form -> form
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK))
                        .failureHandler((request, response, exception) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_OK)));

        return http.build();
    }
}
