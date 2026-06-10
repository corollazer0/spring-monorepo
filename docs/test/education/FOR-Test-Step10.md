# [심화 Step 10] JWT 인증 필터 만들고 테스트하기

> **소요 시간**: 약 2시간 (심화 — 필수 코스 완주 후)
> **이번 Step의 도구**: jjwt 0.11.5, 다중 `SecurityFilterChain`(@Order), `SessionCreationPolicy.STATELESS`, Step 7의 서블릿 Mock 재활용
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/advanced/step10/`

---

## 1. Before We Start — 세션을 못 쓰는 손님들

필수 코스의 인증은 세션 기반이었습니다. 잘 동작하지만 전제가 있죠 —
**클라이언트가 쿠키를 성실히 관리하는 브라우저라는 것.**

그런데 모바일 앱, 외부 시스템 연동, 여러 대로 늘어난 서버(세션 공유 문제) 앞에서는
이 전제가 무너집니다. 해결책 중 하나가 **JWT(JSON Web Token)**:

```
[세션]  서버가 상태를 기억한다  → "회원증 번호만 주면 장부에서 찾아줄게"
[JWT]   토큰이 상태를 담는다    → "위조 불가 도장이 찍힌 신분증을 매번 보여줘"
```

토큰 안에 사용자 정보(username, role)와 만료시각이 들어있고, 서버의 비밀키로 **서명**되어
있어 위조하면 들통납니다. 서버는 아무것도 기억할 필요가 없습니다(stateless).

이번 Step은 두 가지를 동시에 합니다 — JWT 인증을 **만들고**, 그것을 **테스트**합니다.
재미있는 점: 필요한 테스트 기술은 전부 필수 코스에서 배운 것들입니다.

---

## 2. What We're Building

```
src/main/java/com/testonboarding/
├── auth/jwt/
│   ├── JwtTokenProvider.java        ← 토큰 생성/검증 (순수 클래스!)
│   ├── JwtAuthenticationFilter.java ← Bearer 헤더 해석 → SecurityContext 세팅
│   ├── AuthTokenController.java     ← POST /api/v2/auth/token (발급), GET /api/v2/me
│   └── dto/TokenRequest, TokenResponse
└── config/SecurityConfig.java       ← @Order(1) JWT 체인(/api/v2/**) 추가

src/test/java/com/testonboarding/advanced/step10/
├── example/JwtTokenProviderTest.java        ← A: 토큰 왕복/만료/위조/깨진형식
├── example/JwtAuthenticationFilterTest.java ← B: 필터 단위 (Step 7 기술 재활용)
├── example/JwtAuthE2eTest.java              ← C: 발급→Bearer 호출 E2E
├── exercise/JwtAccessExerciseTest.java
└── answer/JwtAccessAnswerTest.java
```

---

## 3. Core Concepts

### 3-1. 두 개의 SecurityFilterChain — URL로 세계를 나눈다

```java
@Bean @Order(1)  // 먼저 매칭 시도
public SecurityFilterChain jwtFilterChain(HttpSecurity http, ...) {
    http.antMatcher("/api/v2/**")    // v2만 이 체인
        .csrf(csrf -> csrf.disable())                        // 세션 없음 → CSRF 표면 없음
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    ...
}

@Bean @Order(2)  // 나머지 전부는 기존 세션 체인
public SecurityFilterChain securityFilterChain(HttpSecurity http) { ... }
```

| | 세션 체인 (v1) | JWT 체인 (v2) |
|---|---|---|
| 상태 | 서버 세션 | 없음 (토큰 안에) |
| CSRF | 활성 (쿠키 기반이므로) | **비활성** (공격 표면 없음) |
| 테스트 편의 | 쿠키 관리 헬퍼 필요 | 헤더 한 줄 |

### 3-2. 설정 주입이 테스트 가능성을 만든다

`JwtTokenProvider`는 생성자로 `secret`과 `validityMillis`를 받는 **순수 클래스**입니다.
덕분에 테스트가 보안 시나리오를 자유자재로 만듭니다:

```java
new JwtTokenProvider(SECRET, -1000L)              // → 태어날 때부터 만료된 토큰
new JwtTokenProvider("attacker-secret...", 1h)    // → 다른 키로 서명된 위조 토큰
```

secret이 클래스 안에 하드코딩되어 있었다면? 위조 시나리오를 만들 방법이 없습니다.
**"테스트하기 어렵다"는 설계가 보내는 신호**입니다 — 의존성(설정 포함)을 주입받게 바꾸세요.

### 3-3. 보안 코드의 테스트는 "거부"가 주인공

example A의 4개 테스트 중 3개가 **실패 케이스**(만료/위조/깨진 형식)입니다.
기능 코드는 성공 경로가 주인공이지만, **보안 코드는 거부 경로가 주인공**입니다.
"위조 토큰으로 ADMIN을 자칭해도 거부된다" — 이 테스트가 없는 JWT 구현은 미완성입니다.

### 3-4. 필터는 차단하지 않는다 — 책임의 분리

`JwtAuthenticationFilter`는 토큰이 무효여도 401을 직접 보내지 않습니다.
인증을 안 심을 뿐, 체인은 진행시킵니다. 차단은 뒤의 **인가 단계**가 결정합니다.

```
[필터] 인증 정보 수집만   →  [인가] 이 요청을 허용할지 결정
```

example B의 `doFilter_토큰없음_인증없이체인진행` 테스트가 이 계약을 봉인합니다.
이렇게 나누면 "토큰은 없지만 permitAll인 URL"(/api/v2/auth/token)이 자연스럽게 동작합니다.

### 3-5. 배운 기술의 재활용 — 새로운 건 JWT뿐

| 이 Step의 테스트 | 사용한 기술 | 어디서 배웠나 |
|----------------|-----------|--------------|
| 토큰 왕복/만료/위조 | 순수 단위 + 생성자 주입 | Step 1, 2 |
| 필터 단위 | 서블릿 Mock 3총사, clearContext | Step 7 |
| 발급→호출 E2E | RANDOM_PORT + TestRestTemplate | Step 8 |

**새 기술(JWT)이 와도 테스트 전략의 골격은 같습니다.** 이것이 기본기의 힘입니다.

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.advanced.step10.*"
```

1. **example A** — 위조 테스트의 발상(공격자 provider)을 음미하세요
2. **example B** — Step 7과 나란히 놓고 보세요: 구조가 똑같습니다
3. **example C** — Step 8의 세션 E2E와 비교: RestSessionHelper가 사라진 이유

---

## 5. Testing — exercise 풀기

`advanced/step10/exercise/JwtAccessExerciseTest.java`의 TODO 1~5를 채우세요.
두 401의 내부 경로 차이(토큰 없음 vs 위조)를 설명할 수 있으면 합격입니다.

---

## 6. Lessons Learned

### 사례 1: jjwt 버전 지옥

- **증상**: 인터넷 예제(`Jwts.parser()`, `signWith(알고리즘, 문자열키)`)가 컴파일 안 됨,
  혹은 0.12.x 예제(`Jwts.SIG.HS256`)가 0.11.x에서 안 돌아감
- **원인**: jjwt는 0.9 → 0.11 → 0.12에서 API가 크게 바뀌었다. 검색 결과의 버전이 제각각
- **해결**: 이 모듈은 0.11.5(Java 8 호환 안정판) 고정 — `parserBuilder()`, `Keys.hmacShaKeyFor`
- **교훈**: Security 5.7의 Adapter 사건(Step 4)과 동일 — **예제를 보면 버전부터 확인하라.**

### 사례 2: WeakKeyException — 키가 짧아요

- **증상**: `io.jsonwebtoken.security.WeakKeyException: The signing key's size is ... bits`
- **원인**: HS256은 서명 키가 256비트(32바이트) 이상이어야 한다 — 짧은 키는 jjwt가 거부
- **해결**: 32바이트 이상의 secret 사용
- **교훈**: 라이브러리가 보안 기준을 강제해주는 건 고마운 일이다. 우회하지 말 것.

### 사례 3: 토큰 발급은 되는데 모든 v2 요청이 401

- **증상**: 발급된 토큰을 분명 보냈는데 401
- **원인(실제 사례 다수)**: `Bearer` 접두사 누락, 헤더명 오타, 필터를 체인에 등록 안 함
- **해결**: example B(필터 단위)와 example C(E2E)가 각각 다른 층의 원인을 잡는다 —
  단위가 통과하는데 E2E가 깨지면 "배선"(체인 등록, @Order) 문제로 범위가 좁혀진다
- **교훈**: 층층이 쌓인 테스트는 장애의 "위치"를 알려주는 좌표계다.

### 시니어의 시선

> 인증 코드는 한 번 작동하면 아무도 안 건드리는 코드가 되기 쉽습니다.
> 그러다 토큰 만료 정책 변경, 키 교체 같은 요구가 오면 모두가 떨면서 수정하죠.
> 만료/위조/형식오류를 박아둔 단위 테스트가 있으면 그 수정이 "5분 + 그린 빌드"로
> 끝납니다. 보안 코드일수록 테스트가 곧 용기입니다.

---

## 7. Key Takeaways

- JWT = 상태를 토큰에 담는 stateless 인증 — 세션/CSRF/쿠키 관리가 사라진다
- 다중 SecurityFilterChain(@Order + antMatcher)으로 URL별 인증 방식을 분리한다
- 설정(secret/유효기간)을 주입받는 설계가 만료/위조 테스트를 가능하게 한다
- 보안 테스트는 거부 경로가 주인공이다
- 새 기술이 와도 테스트 전략의 골격(단위→슬라이스→E2E)은 재활용된다

---

## 8. Next Steps — 마지막 심화

기능은 다 갖췄습니다. 마지막 주제는 **테스트 코드 자체의 품질**입니다.

- 테스트마다 반복되는 `Member.builder()....build()` 준비 코드
- `@WithMockUser`로는 부족한 우리 도메인 전용 인증(memberId, nickname까지)
- 테스트가 늘수록 느려지는 빌드 — 컨텍스트 캐싱의 비밀과 @MockBean의 대가

**Step 11에서 Fixture 패턴, 커스텀 @WithMockMember, 컨텍스트 캐싱**을 다룹니다.
