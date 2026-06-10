# [Step 8] 통합 테스트와 E2E: @SpringBootTest

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `TestRestTemplate`, `@Sql(AFTER_TEST_METHOD)`, `@AutoConfigureMockMvc`, `RestSessionHelper`(직접 제작)
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step08/`

---

## 1. Before We Start — "그래서, 진짜 되는 거야?"

Step 1~7에서 모든 조각을 검증했습니다. 그런데 이 질문에 우리는 아직 답할 수 없습니다.

> "회원가입하고, 로그인해서, 글 쓰면, **진짜로 되는 거야?**"

조각 테스트들이 증명하지 못한 것들:

- `@WithMockUser`는 **진짜 로그인**(UserDetailsService 조회 + 비밀번호 대조)을 한 번도 검증하지 않았다
- `@MockBean`으로 채웠던 자리들이 **실제 빈으로 연결**될 때의 문제(설정 누락, 빈 충돌)
- schema.sql, MyBatis, Security, Jackson이 **동시에** 어우러질 때의 문제

오케스트라에 비유하면, 지금까지는 파트 연습이었습니다. 바이올린도 첼로도 완벽했죠.
하지만 **합주를 한 번도 안 해봤다면** 공연(배포)을 자신할 수 없습니다.
E2E는 합주 리허설입니다 — 자주 할 수는 없지만(비싸니까), 반드시 해야 합니다.

---

## 2. What We're Building

```
src/test/java/com/testonboarding/
├── support/RestSessionHelper.java            ← 세션/CSRF 쿠키를 다루는 E2E 도우미
└── step08/
    ├── example/MemberJourneyE2eTest.java     ← A: 진짜 HTTP 여정 (RANDOM_PORT)
    ├── example/BoardIntegrationMockMvcTest.java ← B: 중간 지대 (전체 컨텍스트 + MockMvc)
    ├── exercise/AnonymousE2eExerciseTest.java
    └── answer/AnonymousE2eAnswerTest.java
```

시나리오:

| 시나리오 | 방식 | 증명하는 것 |
|---------|------|------------|
| 가입→로그인→글작성→조회 전체 여정 | A (진짜 HTTP) | 전 구간 연결 + 진짜 로그인 |
| 틀린 비밀번호 로그인 실패 | A | PasswordEncoder 대조가 실제 동작 |
| 글 작성 후 즉시 조회 | B (MockMvc) | Mock 없는 전 레이어 연결 + 롤백 |
| 비로그인 읽기 OK / 쓰기 401 | exercise | 진짜 HTTP에서의 보안 경계 |

---

## 3. Core Concepts

### 3-1. 세 가지 통합 수준 — 이제 전체 지도가 완성된다

| | 슬라이스 (Step 3~7) | B: 풀컨텍스트+MockMvc | A: RANDOM_PORT |
|---|---|---|---|
| 뜨는 빈 | 일부 | **전부 (진짜)** | **전부 (진짜)** |
| 서버(Tomcat) | ❌ | ❌ | ✅ 진짜 |
| HTTP | 가짜 (MockMvc) | 가짜 (MockMvc) | **진짜** |
| 진짜 로그인/쿠키 | ❌ | ❌ (@WithMockUser) | ✅ |
| @Transactional 롤백 | ✅ | ✅ | ❌ **동작 안 함!** |
| 속도 | 빠름 | 중간 | 느림 |
| 적정 수량 | 많이 | 핵심 흐름 몇 개 | **소수 정예** |

### 3-2. ⚠️ 이 Step 최대의 함정: RANDOM_PORT에서는 롤백이 안 된다

```
[MockMvc]     테스트 스레드 ──(같은 스레드)──> Controller    → 한 트랜잭션 → 롤백 OK
[RANDOM_PORT] 테스트 스레드 ──(진짜 HTTP)──> 서버 스레드     → 트랜잭션 분리 → 롤백 불가!
```

테스트에 `@Transactional`을 붙여도 **서버 스레드의 INSERT는 이미 커밋**됐습니다.
그래서 example A는 `@Sql(AFTER_TEST_METHOD)`로 만든 데이터를 직접 치웁니다:

```java
@Sql(scripts = "/sql/cleanup-journey.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
```

이걸 모르면 "첫 실행은 통과, 두 번째 실행은 중복 아이디로 실패"라는 미스터리를 겪습니다.

### 3-3. TestRestTemplate은 브라우저가 아니다 — RestSessionHelper

브라우저는 쿠키를 자동 관리하지만 TestRestTemplate은 안 합니다. 세션 기반 + CSRF 환경에서
E2E를 하려면 쿠키를 직접 다뤄야 합니다. 그 일을 `support/RestSessionHelper`에 모았습니다:

```
1. GET 아무거나        → 응답의 Set-Cookie에서 XSRF-TOKEN 보관
2. POST /login (폼)    → Cookie: XSRF-TOKEN + X-XSRF-TOKEN 헤더 동봉
                         → 성공(200) 시 Set-Cookie의 JSESSIONID 보관
3. 이후 모든 요청       → Cookie: JSESSIONID; XSRF-TOKEN (+쓰기엔 X-XSRF-TOKEN)
```

이런 "테스트 인프라 코드"를 테스트마다 복붙하지 않고 support 패키지에 모으는 것 —
테스트 코드도 코드입니다. 중복 제거와 이름 붙이기가 똑같이 중요합니다.

### 3-4. REST API의 로그인 응답 — 302 대신 상태코드

기본 폼로그인은 성공 시 302 redirect를 보냅니다(웹 페이지용). REST API에서는
클라이언트가 redirect를 따라갈 이유가 없으므로, SecurityConfig에서 핸들러를 바꿨습니다:

```java
.formLogin(form -> form
        .loginProcessingUrl("/login")
        .successHandler((req, res, auth) -> res.setStatus(SC_OK))          // 200
        .failureHandler((req, res, ex) -> res.setStatus(SC_UNAUTHORIZED))) // 401
```

E2E 테스트가 단순해지는 동시에, API 설계도 깔끔해졌습니다 — 테스트하기 좋은 설계가
좋은 설계인 경우가 많습니다.

### 3-5. E2E는 "여정의 연결"을 검증한다

example A의 5단계를 보세요. 각 단계의 세부 검증(필드 하나하나)은 이미 슬라이스가 했습니다.
E2E가 검증하는 것은 **연결**입니다: 가입한 계정으로 로그인이 되고(1→2), 그 세션으로
글이 써지고(2→4), 그 글의 writer가 로그인 사용자이고(4→5). E2E에서 모든 필드를
검증하려 들면 느린 테스트만 잔뜩 생깁니다 — **세부는 아래층에, 연결은 위층에.**

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step08.*"
```

1. **support/RestSessionHelper** 먼저 읽기 — E2E의 절반은 이 클래스 이해입니다
2. **example A** — 전체 여정 5단계 + 틀린 비밀번호. `@Sql` 정리를 주석 처리하고
   두 번 연속 실행해보세요(중복 아이디 실패 체험 → 복구)
3. **example B** — 같은 "전부 진짜" 세계지만 MockMvc라 롤백이 공짜인 중간 지대

---

## 5. Testing — exercise 풀기

`step08/exercise/AnonymousE2eExerciseTest.java`의 TODO 1~6을 채우세요.
비로그인 읽기(200)와 쓰기(401) — 진짜 HTTP에서 보안 경계가 지켜지는 최종 증명입니다.

---

## 6. Lessons Learned

### 사례 1: "첫 실행은 통과, 두 번째는 실패"

- **증상**: E2E를 다시 돌리면 회원가입이 409(중복)로 실패
- **원인**: RANDOM_PORT에서는 롤백이 안 되는데 정리를 안 함 — 이전 실행의 journey1이 남아있다
- **해결**: `@Sql(AFTER_TEST_METHOD)` 정리 스크립트
- **교훈**: E2E는 "내가 만든 데이터는 내가 치운다"가 원칙. 그리고 테스트는
  반드시 **두 번 연속** 돌려보고 커밋하라 — 반복 가능성(F.I.R.S.T의 R)은 공짜가 아니다.

### 사례 2: E2E가 너무 많아져 빌드가 10분

성공에 취해 모든 시나리오를 E2E로 만들면 어느새 빌드가 커피 타임이 됩니다.
- **교훈**: 테스트 피라미드를 기억하라. E2E는 "여정" 단위로 소수 정예.
  필드 검증/분기 검증이 하고 싶어지면 그건 슬라이스나 단위의 일이다.

### 사례 3: 컨텍스트 캐싱 — 똑같은 설정의 테스트는 컨텍스트를 재사용한다

@SpringBootTest가 여러 개여도 **설정이 같으면 컨텍스트는 한 번만** 뜹니다(캐싱).
그런데 @MockBean 구성이 다르거나 @DirtiesContext가 있으면 캐시가 깨져 매번 새로 뜹니다.
테스트가 갑자기 느려졌다면 컨텍스트가 몇 번 뜨는지 로그에서 세어보세요. (심화 Step 11의 주제)

### 시니어의 시선

> E2E의 가장 큰 가치는 사실 "배포 직전의 심리적 안정감"입니다.
> 수백 개의 단위 테스트가 통과해도 마음 한구석이 불안한 이유는
> "조각이 맞물리는 걸 본 적이 없어서"입니다. 핵심 여정 몇 개의 E2E가
> 그 불안을 끄는 스위치입니다. 단, 스위치는 몇 개면 충분합니다 —
> 복도 전체를 스위치로 도배하지 마세요.

---

## 7. Key Takeaways

- 통합의 3수준: 슬라이스(많이) → 풀컨텍스트+MockMvc(핵심 흐름) → RANDOM_PORT E2E(소수 정예)
- RANDOM_PORT에서는 @Transactional 롤백이 안 된다 — @Sql(AFTER_TEST_METHOD)로 직접 정리
- TestRestTemplate은 쿠키를 자동 관리하지 않는다 — 세션/CSRF 처리는 헬퍼로 모은다
- E2E는 여정의 "연결"을 검증한다 — 세부 검증은 아래층(슬라이스/단위)의 일
- 진짜 로그인(비밀번호 대조)은 E2E만이 검증할 수 있다

---

## 8. Next Steps — 졸업 시험

축하합니다 — 테스트 피라미드의 모든 층을 직접 경험했습니다.

```
        ▲  E2E              ← Step 8  ✅
       ▲▲  슬라이스           ← Step 3~7 ✅
      ▲▲▲  단위              ← Step 1~2 ✅
```

이제 마지막 관문입니다. **Step 9 캡스톤**에서는 새 기능(댓글)의 프로덕션 코드와
요구사항 문서만 주어집니다. 어떤 것을 단위로, 어떤 것을 슬라이스로, 어떤 것을 E2E로
검증할지 — **테스트 전략을 스스로 설계하고 작성**하는 것이 과제입니다.
정답지 없이(막히면 answer가 있긴 합니다만, 최대한 버텨보세요) 도전하세요.
