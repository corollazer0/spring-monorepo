---
name: security-patterns
description: Spring Security 설정을 작성·수정하거나 인증/인가/CSRF/JWT 관련 코드와 테스트를 다룰 때 사용. SecurityFilterChain 표준(구식 Adapter 금지), 세션/JWT 이중 체인, EntryPoint 분기, 보안 테스트 패턴을 정의한다.
---

# Spring Security 표준 (Boot 2.7 / Security 5.7)

## 1. 설정 표준

- **`WebSecurityConfigurerAdapter` 상속 금지** (deprecated) — `SecurityFilterChain` @Bean + 람다 DSL만 사용. 인터넷 예제 대부분이 구식이니 버전부터 확인하라.
- 인가 패턴은 **구체적인 것을 먼저** 선언 (예: `/posts/new` authenticated가 GET `/posts/**` permitAll보다 앞에)
- URL 인가(SecurityConfig) vs 데이터 소유권 인가(Service)를 구분 — "작성자 본인만"은 Service 책임
- 비밀번호: `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (접두사 방식), 평문 저장 금지

## 2. 한 인증, 두 클라이언트 (REST + 화면 공존 시)

```java
// 미인증 응대 분기: 프로그램에겐 상태코드를, 사람에겐 안내를
.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
        new AntPathRequestMatcher("/api/**")))     // API → 401, 그 외 → 로그인 페이지(302)
// 로그인 성공/실패 핸들러도 Accept: application/json 여부로 200/401 vs redirect 분기
```

## 3. 다중 체인 (세션 + JWT 공존 시)

- `@Order(1)` + `antMatcher("/api/v2/**")`: STATELESS + csrf disable + `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`
- `@Order(2)`: 나머지 전부 — 세션 + 폼로그인 + CSRF 활성
- JWT 필터는 **차단하지 않는다** — 유효하면 SecurityContext에 인증을 심고, 무효면 안 심을 뿐 체인은 진행 (차단은 인가 단계)
- jjwt는 0.11.x API(`parserBuilder`, `Keys.hmacShaKeyFor`) — HS256 키는 32바이트 이상

## 4. 보안 테스트 패턴

| 검증 | 도구 |
|------|------|
| 로그인된 상태의 규칙 | `@WithMockUser` (절차 생략, SecurityContext 직접 주입) / 요청 단위는 `with(user())` |
| 도메인 principal 필요 시 | 커스텀 `@WithMockMember`(@WithSecurityContext + 팩토리 — 50줄이면 만든다) |
| 상태 변경 요청 | `with(csrf())` 필수 — **CSRF 검증이 인증보다 먼저**라 누락 시 401 검증 의도가 403에 가려진다 |
| 진짜 로그인(비밀번호 대조) | E2E만 가능 — @WithMockUser는 UserDetailsService를 타지 않는다 |
| 401 vs 403 | 401=인증 없음("너 누구야"), 403=인가/CSRF 실패("너 안 돼") — 테스트명으로 구분해 박는다 |
| JWT | 만료(음수 유효기간 provider) / 위조(다른 키 provider) / 깨진 형식 — **거부 경로가 주인공** |

**원칙: 보안 규칙 한 줄당 테스트 하나** — permitAll/hasRole은 지워도 컴파일되는 코드다. 테스트만이 회귀를 막는다.

> 이 레포의 모범: `spring-test-onboarding` SecurityConfig + step06/step12/advanced.step10 테스트.
