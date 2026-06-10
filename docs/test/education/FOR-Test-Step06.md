# [Step 6] Security 테스트: 인증과 인가

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `@WithMockUser`, `@WithAnonymousUser`, `csrf()`, `user()` (spring-security-test)
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step06/`

---

## 1. Before We Start — "로그인한 척"이 필요하다

이 규칙들을 테스트해야 합니다.

- 로그인 안 한 사용자는 글을 못 쓴다
- 작성자가 아닌 사용자는 남의 글을 못 고친다
- ADMIN만 관리 URL에 접근할 수 있다

정직하게 하려면 테스트마다 회원가입 → 로그인 → 세션 쿠키 보관 → 요청에 첨부...
글쓰기 테스트 하나에 로그인 절차가 주렁주렁 달립니다. 우리가 검증하려는 건
"로그인 절차"가 아니라 **"로그인된 상태에서의 규칙"** 인데 말이죠.

`spring-security-test`가 주는 해답: **인증 결과를 직접 심어버린다.**
Spring Security는 "현재 사용자"를 SecurityContext라는 보관함에서 꺼내 쓰는데,
`@WithMockUser`는 로그인 절차를 건너뛰고 이 보관함에 가짜 인증 정보를 넣어줍니다.
연극으로 치면, 1막(로그인)을 공연하는 대신 **2막의 무대 상태를 바로 세팅**하는 겁니다.

> 그럼 1막(진짜 로그인)은 영영 검증 안 하나요? 아닙니다 — Step 8의 E2E에서
> 진짜 폼로그인으로 검증합니다. 여기서는 "로그인 이후의 규칙"에 집중합니다.

---

## 2. What We're Building

프로덕션 코드 변경 없음 — Step 4에서 만든 SecurityConfig의 규칙을 정면으로 테스트합니다.

```
src/test/java/com/testonboarding/step06/
├── example/BoardSecurityTest.java               ← 인증/CSRF/인가 3개 그룹
├── exercise/BoardDeleteSecurityExerciseTest.java ← 삭제 시나리오 3종
└── answer/BoardDeleteSecurityAnswerTest.java
```

| 시나리오 | 기대 | 종류 |
|---------|------|------|
| 미인증 글쓰기 | 401 | 인증 실패 |
| 인증 글쓰기 | 201 + principal이 Service까지 전달 | 인증 성공 |
| 인증했지만 CSRF 토큰 없음 | 403 | CSRF |
| USER가 /api/admin/** | 403 | URL 인가 |
| 타인 글 수정 | 403 | 데이터 소유권 인가 |
| 본인 글 수정 | 200 | 인가 성공 |

---

## 3. Core Concepts

### 3-1. 401 vs 403 — 가장 중요한 한 장의 표

| | 401 Unauthorized | 403 Forbidden |
|---|---|---|
| 의미 | "너 누구야?" | "너 안 돼" |
| 상태 | 인증 자체가 없음/실패 | 인증은 됐지만 권한·조건 불충족 |
| 이 모듈에서 | 미인증 요청 (HttpStatusEntryPoint) | 권한 부족, CSRF 누락, 소유자 아님 |
| 장애 분석 | "로그인이 풀렸나?" | "권한 설정이 잘못됐나?" |

이름이 헷갈리게 지어졌습니다(Unauthorized인데 인증 문제...). 표로 외우지 말고
**테스트 코드로 박아두세요** — example의 테스트명이 그대로 사전이 됩니다.

### 3-2. @WithMockUser — 로그인 건너뛰고 결과만 심기

```java
@Test
@WithMockUser(username = "writer1", roles = "USER")   // roles 기본값도 USER
void createPost_인증사용자_201() { ... }
```

- DB 조회도, 비밀번호 검증도 없습니다. **UserDetailsService를 타지 않습니다**
- 그냥 "writer1이라는 USER가 인증된 상태"가 SecurityContext에 들어갑니다
- Controller의 `Principal principal`에는 "writer1"이 주입됩니다

example의 핵심 트릭: `given(boardService.createPost(eq("writer1"), any()))` —
**eq("writer1") stubbing이 적중했다는 것 자체가 인증 주체가 Service까지 정확히
전달됐다는 증명**입니다.

여러 사용자를 한 테스트에서 번갈아 쓸 때는 요청 단위의 `with(user("writer2"))`를 씁니다.

### 3-3. CSRF — 정체불명이었던 with(csrf())의 정체

세션 기반 인증의 약점: 브라우저는 해당 도메인 요청에 세션 쿠키를 **자동으로** 붙입니다.
악성 사이트가 `<form action="https://우리사이트/api/posts" method="POST">`를 몰래 제출하면
**내 세션으로** 글이 써집니다. 이것이 CSRF(Cross-Site Request Forgery) 공격입니다.

방어: 서버가 발급한 CSRF 토큰을 상태 변경 요청(POST/PUT/DELETE)에 동봉해야만 통과.
악성 사이트는 토큰을 모르므로 위조 요청이 차단됩니다.

테스트에서의 의미:

```java
.with(csrf())   // "유효한 CSRF 토큰을 가진 정상 요청"을 시뮬레이션
```

⚠️ **함정**: CSRF 검증은 인증 확인보다 **먼저** 수행됩니다. 토큰 없는 요청은
인증이 됐든 안 됐든 **403**입니다. "로그인했는데 왜 403이지?!"의 단골 원인.
그래서 401을 검증하려는 테스트에도 csrf()를 붙여야 합니다 — 안 그러면 CSRF 403이
먼저 떠서 테스트의 의도(인증 검증)가 가려집니다.

GET은 상태를 바꾸지 않으므로 CSRF 면제 — example의 `getPosts_GET요청_csrf불필요` 참고.

### 3-4. 인가의 두 층위 — URL 인가 vs 데이터 소유권 인가

```
[ URL 인가 ]        /api/admin/** 는 ADMIN만     → SecurityConfig (hasRole)
[ 소유권 인가 ]      이 글은 작성자 본인만 수정     → Service (Step 2) + Advice 403 (Step 5)
```

"작성자 본인만"을 SecurityConfig에 넣을 수 없는 이유: URL만 봐서는 **그 글의 작성자가
누군지 알 수 없기 때문**입니다(DB를 봐야 안다). 그래서 데이터 기반 인가는 Service의 책임.

example의 `updatePost_타인글_403` 테스트는 **세 층의 합작**을 한 번에 검증합니다:
Security(인증 통과) → Service(소유권 검사 → 예외) → Advice(403 번역).

### 3-5. URL 인가는 핸들러가 없어도 동작한다

`adminUrl_일반사용자_403` 테스트의 `/api/admin/dashboard`는 **아직 Controller가 없는 URL**
입니다. 그런데도 403이 나옵니다 — Security 필터는 DispatcherServlet(핸들러 매핑)보다
앞단에서 URL 패턴만으로 차단하기 때문입니다. 보안 규칙이 코드 존재 여부와 무관하게
동작한다는 것, 그리고 그 규칙 자체를 테스트로 봉인할 수 있다는 것을 보여줍니다.

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step06.*"
```

`BoardSecurityTest`의 3개 @Nested를 순서대로:

1. **AuthenticationCases** — 401 → 201 대비, eq("writer1") 트릭, user() 방식
2. **CsrfCases** — csrf 없는 403 직접 체험 (Step 4부터의 미스터리 해소)
3. **AuthorizationCases** — URL 인가(403) vs 소유권 인가(403)의 출처 차이

**일부러 깨뜨려보기**: SecurityConfig에서 `.antMatchers(HttpMethod.GET, "/api/posts/**").permitAll()`
줄을 지우면 어떤 테스트들이 깨질까요? 예측하고 → 지우고 → 확인하고 → 복구하세요.

---

## 5. Testing — exercise 풀기

`step06/exercise/BoardDeleteSecurityExerciseTest.java`의 TODO 1~7을 채우세요.

주의: `deletePost`는 반환값이 없는(void) 메서드라 stubbing 순서가 다릅니다 —
`given(...).willThrow(...)`가 아니라 **`willThrow(...).given(mock).deletePost(...)`**.

---

## 6. Lessons Learned

### 사례 1: "로그인했는데 403이 나와요" (CSRF 미스터리)

- **증상**: @WithMockUser를 분명 붙였는데 POST가 403
- **원인**: CSRF 토큰 누락 — CSRF 검증이 인증 확인보다 먼저다
- **해결**: `.with(csrf())`
- **교훈**: 403이 나오면 "권한 문제"로 단정하지 말 것. 세션 기반 + 상태 변경 요청이면
  CSRF부터 의심하라. (실서비스 디버깅에서도 똑같이 적용되는 순서다)

### 사례 2: 401을 검증하려 했는데 403이 나온다

- **증상**: 미인증 테스트에서 401을 기대했는데 403
- **원인**: csrf() 없이 보낸 미인증 POST — CSRF 차단(403)이 인증 차단(401)보다 먼저
- **해결**: 미인증 테스트에도 csrf()를 붙여 "순수하게 인증만" 검증
- **교훈**: 테스트는 한 번에 하나의 관심사만 검증해야 한다. 다른 요인이 끼어들면 격리하라.

### 사례 3: 모든 보안 테스트가 통과하는데 운영에서 뚫렸다

@WithMockUser는 **로그인 절차를 검증하지 않습니다**. UserDetailsService의 버그
(비밀번호 비교 누락, 잠금 계정 미처리)는 이 방식으로 영영 못 잡습니다.
→ 그래서 Step 8의 진짜 로그인 E2E가 반드시 한 줄은 있어야 합니다. 역할 분담:
규칙 검증은 @WithMockUser로 많이, 로그인 절차 검증은 E2E로 핵심만.

### 시니어의 시선

> 보안 설정은 "한 줄 지워도 컴파일이 되는" 코드입니다. permitAll 한 줄,
> hasRole 한 줄이 사라져도 빌드는 통과하고, 기능 테스트도 통과하고,
> 침해 사고가 나서야 발견됩니다. 보안 규칙 하나당 테스트 하나 —
> 이것이 보안 설정의 회귀를 막는 유일한 자동화 장치입니다.
> exercise의 `deletePost_미인증_401` 같은 테스트는 사소해 보여도
> **누군가 실수로 permitAll을 추가하는 순간 깨지며 사고를 막아줍니다.**

---

## 7. Key Takeaways

- 401 = 인증 없음("너 누구야"), 403 = 인가 실패("너 안 돼") — 테스트명으로 박아두라
- @WithMockUser = 로그인 절차 생략, SecurityContext에 인증 결과 직접 주입
- CSRF 검증은 인증보다 먼저 — "로그인했는데 403"의 단골 원인
- 인가는 두 층: URL 인가(SecurityConfig) + 데이터 소유권 인가(Service)
- 보안 규칙 하나당 테스트 하나 — 설정 한 줄의 회귀를 막는 유일한 장치

---

## 8. Next Steps — 다음 Step의 문제

지금까지 요청은 이런 길을 지나왔습니다.

```
요청 → [Filter들: 로깅, 인증, CSRF...] → DispatcherServlet → [Interceptor] → Controller
```

우리는 Controller와 그 뒤(Service/DAO)만 테스트했습니다. 그런데 회사 코드를 열어보면
요청 ID를 채번하는 **Filter**, 관리자 메뉴 접근을 막는 **Interceptor** 같은 것들이
요청의 길목마다 서 있습니다. 이들의 로직이 틀리면? 모든 요청이 영향을 받습니다.

이 길목 컴포넌트들을 **그 자체로** 테스트하는 법 — MockHttpServletRequest와
MockFilterChain으로 필터를 "손에 들고" 검증하는 기술이 **Step 7의 주제**입니다.
