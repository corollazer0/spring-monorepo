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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
                // [Step 12] 한 인증, 두 클라이언트:
                // - REST(/api/**) 미인증 → 401 (프로그램이 해석할 상태코드)
                // - 화면(그 외) 미인증 → 로그인 페이지로 302 (사람이 따라갈 안내)
                // 나머지 요청의 EntryPoint는 formLogin이 등록하는 LoginUrl(/login)이 담당한다
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")))

                // CSRF 토큰을 쿠키로 발급 (Step 8 E2E에서 사용). H2 콘솔은 예외
                // Thymeleaf 폼(th:action)은 hidden 필드로 토큰을 자동 포함한다 (Step 12)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringAntMatchers("/h2-console/**"))

                // H2 콘솔이 iframe을 쓰므로 같은 출처의 frame 허용
                .headers(headers -> headers.frameOptions().sameOrigin())

                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/h2-console/**", "/login", "/logout", "/signup", "/css/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .antMatchers(HttpMethod.POST, "/api/members").permitAll()
                        .antMatchers("/api/admin/**").hasRole("ADMIN")
                        // 순서 주의! 글쓰기 화면은 아래 permitAll(GET /posts/**)보다 먼저 선언해야 한다
                        .antMatchers("/posts/new").authenticated()
                        .antMatchers(HttpMethod.GET, "/", "/posts/**").permitAll()
                        .anyRequest().authenticated())

                // 세션 기반 폼로그인. 응답도 클라이언트 종류에 따라 분기한다:
                // - Accept: application/json (REST 테스트/외부 연동) → 200/401 상태코드
                // - 브라우저 → /posts 로 redirect / /login?error 로 재안내
                .formLogin(form -> form
                        .loginPage("/login")          // 커스텀 로그인 화면 (Step 12)
                        .loginProcessingUrl("/login") // 인증 처리 URL (POST)
                        .successHandler((request, response, authentication) -> {
                            if (wantsJson(request)) {
                                response.setStatus(HttpServletResponse.SC_OK);
                            } else {
                                response.sendRedirect("/posts");
                            }
                        })
                        .failureHandler((request, response, exception) -> {
                            if (wantsJson(request)) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                response.sendRedirect("/login?error");
                            }
                        }))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (wantsJson(request)) {
                                response.setStatus(HttpServletResponse.SC_OK);
                            } else {
                                response.sendRedirect("/posts");
                            }
                        }));

        return http.build();
    }

    /**
     * 클라이언트 구분: Accept 헤더에 application/json이 있으면 프로그램(REST), 아니면 브라우저로 본다.
     */
    private boolean wantsJson(javax.servlet.http.HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }
}
