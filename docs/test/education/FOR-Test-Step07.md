# [Step 7] Filter & Interceptor 테스트

> **소요 시간**: 약 1시간
> **이번 Step의 도구**: `MockHttpServletRequest`/`MockHttpServletResponse`/`MockFilterChain`, `OncePerRequestFilter`, `HandlerInterceptor.preHandle`, `SecurityContextHolder` 수동 세팅
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step07/`

---

## 1. Before We Start — 요청의 길목에 서 있는 것들

HTTP 요청은 Controller에 도달하기 전에 여러 관문을 지납니다.

```
요청 ──> [Filter 체인]  ──> DispatcherServlet ──> [Interceptor] ──> Controller
         서블릿 표준           Spring MVC 영역        Spring MVC 영역
         (Security도 필터!)
```

| | Filter | Interceptor |
|---|---|---|
| 소속 | 서블릿 표준 (Spring 밖에서도 동작) | Spring MVC |
| 시점 | DispatcherServlet **이전** | 핸들러 매핑 **이후**, Controller 직전 |
| 용도 | 인코딩, 로깅, 인증 토큰, CORS | 권한 보조 검사, 감사 로그, 핸들러 정보 활용 |
| 등록 | @Component (Boot 자동 등록) | WebConfig에 **경로와 함께 수동 등록** |

이들의 특징: **모든(또는 광범위한) 요청에 적용된다**는 것. 로직이 틀리면 전체 장애입니다.
그런데 MockMvc 테스트는 "통과한 결과"만 보여주지, 필터 내부의 분기
(헤더가 있을 때/없을 때, 예외 시 정리)를 따로 검증하기 어렵습니다.

해결책: 필터도 인터셉터도 결국 **그냥 클래스**입니다. Spring의 서블릿 Mock으로
가짜 요청/응답을 만들어 메서드를 직접 호출하면 됩니다.

---

## 2. What We're Building

```
src/main/java/com/testonboarding/
├── common/filter/RequestLoggingFilter.java      ← X-Request-Id 발급 + MDC + 처리시간 로깅
├── common/interceptor/AdminCheckInterceptor.java ← 관리자 감사로그 + 2차 방어(403)
├── config/WebConfig.java                         ← 인터셉터를 /api/admin/** 에 등록
└── board/controller/AdminController.java         ← GET /api/admin/stats

src/test/java/com/testonboarding/step07/
├── example/RequestLoggingFilterTest.java     ← 필터 단위 테스트 (서블릿 Mock 3총사)
├── example/AdminCheckInterceptorTest.java    ← 인터셉터 단위 테스트 (SecurityContext 수동)
├── exercise/AdminAccessExerciseTest.java     ← 통합(MockMvc) + 단위 혼합 과제
└── answer/AdminAccessAnswerTest.java
```

---

## 3. Core Concepts

### 3-1. 서블릿 Mock 3총사 — 필터를 손에 들고 테스트하기

```java
MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/posts");
MockHttpServletResponse response = new MockHttpServletResponse();
MockFilterChain chain = new MockFilterChain();

filter.doFilter(request, response, chain);

assertThat(response.getHeader("X-Request-Id")).isNotBlank(); // 필터가 쓴 헤더
assertThat(chain.getRequest()).isSameAs(request);            // 체인이 이어졌는가!
```

`chain.getRequest()` 검증의 의미: 필터가 `chain.doFilter(...)`를 빠뜨리면
**모든 요청이 무응답이 되는 대형사고**입니다. MockFilterChain은 자기에게 도달한
요청을 기억하므로, "다음 단계로 넘겼다"는 사실을 검증할 수 있습니다.

### 3-2. 체인 안을 엿보는 기법 — 람다 FilterChain

"요청 처리 **중에는** MDC에 ID가 있고, 처리 **후에는** 비워진다"를 어떻게 검증할까요?
FilterChain은 메서드가 하나뿐인 인터페이스라 람다로 만들 수 있습니다.

```java
AtomicReference<String> mdcInsideChain = new AtomicReference<>();
FilterChain spyingChain = (req, res) -> mdcInsideChain.set(MDC.get("requestId"));

filter.doFilter(request, response, spyingChain);

assertThat(mdcInsideChain.get()).isNotBlank();      // 처리 중에는 있었다
assertThat(MDC.get("requestId")).isNull();          // 처리 후에는 비워졌다
```

체인 자리에 "엿보는 코드"를 꽂는 것 — Controller가 실행될 시점의 상태를 검증하는 기법입니다.

### 3-3. 인터셉터 단위 테스트 — SecurityContext 수동 세팅

`@WithMockUser`는 Spring 테스트 컨텍스트의 기능입니다. Spring 없이 인터셉터를
단위 테스트할 때는 SecurityContext를 직접 구성합니다.

```java
SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
                "admin", "password", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

boolean result = interceptor.preHandle(request, response, new Object());
```

⚠️ **SecurityContextHolder는 ThreadLocal(스레드 전역) 저장소**입니다.
`@AfterEach`에서 `clearContext()`를 안 하면 인증 상태가 다음 테스트로 새어 들어가
"혼자 돌리면 통과, 전체 돌리면 실패"라는 최악의 비결정성 버그가 생깁니다.

### 3-4. @WebMvcTest는 Filter/Interceptor를 자동 포함한다

`@WebMvcTest`의 컴포넌트 스캔 범위에는 `@Controller` 외에도
**Filter, HandlerInterceptor, WebMvcConfigurer**가 포함됩니다.
그래서 answer의 통합 테스트에서 `header().exists("X-Request-Id")`가 통과합니다 —
필터가 슬라이스 안에서 실제로 돌고 있다는 증거죠.

(반대로 말하면: Step 4~6의 MockMvc 테스트들도 사실 이 필터를 통과하고 있었습니다.
슬라이스에 뭐가 포함되는지 모르면 "내 테스트가 뭘 검증하고 있는지"도 모르는 겁니다.)

### 3-5. 단위 vs 통합 — 이 Step의 역할 분담

| 검증 대상 | 방식 |
|----------|------|
| 필터/인터셉터의 **내부 분기** (ID 유무, 권한별, null 안전성, MDC 정리) | 단위 (서블릿 Mock) |
| **등록/배선** (경로 매핑이 맞는지, 체인에 실제로 끼는지) | 통합 (MockMvc) |

WebConfig의 `addPathPatterns("/api/admin/**")` 오타는 단위 테스트로 못 잡습니다 —
그건 배선이니까 MockMvc로. 반대로 "미인증이면 NPE 없이 403"은 단위로 깔끔하게.

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step07.*"
```

1. **RequestLoggingFilterTest** — 3총사 기본형 → 기존 ID 보존 → 람다 체인 엿보기
2. **AdminCheckInterceptorTest** — ADMIN true / USER 403 / 미인증 null 안전성, 그리고 `@AfterEach clearContext()`

**일부러 깨뜨려보기**: `RequestLoggingFilter`의 `finally { MDC.remove(...) }`를 지워보세요.
어떤 테스트가 깨지나요? 그 테스트가 바로 "스레드 오염 사고"를 막는 보초입니다.

---

## 5. Testing — exercise 풀기

`step07/exercise/AdminAccessExerciseTest.java`의 TODO 1~9를 채우세요.
통합(MockMvc) 파트와 단위(직접 호출) 파트가 섞여 있습니다 — 두 방식의 차이를 느껴보세요.

TODO 6에서 생각해볼 것: USER의 403은 Security(hasRole)와 인터셉터 중 **누가** 먼저 막았을까요?
(Security 필터가 DispatcherServlet보다 앞이므로 Security가 먼저. 인터셉터는 2차 방어선입니다)

---

## 6. Lessons Learned

### 사례 1: MDC를 안 지워서 남의 requestId가 찍힌 로그

- **증상**: 운영 로그에서 서로 다른 요청이 같은 requestId로 찍혀 장애 추적이 혼선
- **원인**: 서블릿 스레드는 풀에서 재사용되는데 ThreadLocal(MDC)을 정리하지 않음
- **해결**: `finally { MDC.remove(...) }` + 그것을 검증하는 단위 테스트(example 세 번째)
- **교훈**: ThreadLocal을 쓰는 코드는 "정리"가 기능의 절반이다. 정리도 테스트하라.

### 사례 2: 인터셉터를 만들었는데 동작하지 않는다

- **증상**: 인터셉터에 분명 로직을 넣었는데 아무 일도 일어나지 않음
- **원인**: @Component만 붙이고 WebConfig의 addInterceptors 등록을 빠뜨림
  (Filter는 Bean이면 자동 등록되지만 **Interceptor는 수동 등록**이라 헷갈린다!)
- **해결**: WebConfig 등록 + answer의 통합 테스트(200/403)가 이 누락을 잡는다
- **교훈**: 단위 테스트가 전부 통과해도 배선이 안 됐으면 무용지물 — 통합 한 줄이 필요한 이유.

### 사례 3: 테스트 순서에 따라 결과가 달라진다

- **증상**: 인터셉터 테스트가 단독 실행은 통과, 전체 실행은 실패(또는 그 반대)
- **원인**: 앞 테스트가 SecurityContextHolder를 정리하지 않아 인증 상태가 누수
- **해결**: `@AfterEach SecurityContextHolder.clearContext()`
- **교훈**: static/ThreadLocal 상태를 만지는 테스트는 반드시 원상복구하라 (F.I.R.S.T의 I).

### 시니어의 시선

> 필터와 인터셉터는 "잘 되면 아무도 모르고, 깨지면 전부가 아는" 코드입니다.
> 호출 빈도로 따지면 시스템에서 가장 많이 실행되는 코드인데도
> 테스트 커버리지는 가장 낮은 곳이기도 합니다 — "MockMvc가 어차피 통과하니까"라며
> 넘어가기 때문이죠. 길목 코드일수록 분기 하나하나를 단위로 박아두세요.
> 전 요청 장애의 원인 분석이 "테스트 실패 1건"으로 줄어듭니다.

---

## 7. Key Takeaways

- Filter(서블릿 표준, 자동 등록) vs Interceptor(Spring MVC, WebConfig 수동 등록)
- 서블릿 Mock 3총사로 필터/인터셉터를 new 해서 단위 테스트한다
- `chain.getRequest()` = 체인 진행 검증, 람다 체인 = 처리 중 상태 엿보기
- ThreadLocal(MDC, SecurityContextHolder)은 정리까지 테스트하라
- 내부 분기는 단위로, 등록/배선은 MockMvc 통합으로 — 역할 분담

---

## 8. Next Steps — 다음 Step의 문제

이제 모든 조각이 검증됐습니다.

```
정책(1) ✅  Service(2) ✅  DAO/SQL(3) ✅  Controller(4) ✅
Validation/예외(5) ✅  Security(6) ✅  Filter/Interceptor(7) ✅
```

그런데 누군가 묻습니다 — **"그래서, 회원가입하고 로그인해서 글 쓰면 진짜 되는 거야?"**

...우리는 아직 이 질문에 답할 증거가 없습니다. 조각은 다 통과했지만
**전체가 한 몸으로 도는 것**은 아무도 증명하지 않았습니다. 게다가:

- @WithMockUser는 진짜 로그인(UserDetailsService + 비밀번호 대조)을 검증한 적이 없다
- @MockBean으로 채운 자리들이 실제 빈으로 연결될 때의 문제(빈 충돌, 설정 누락)는?
- schema.sql과 MyBatis와 Security와 Jackson이 **동시에** 어우러질 때는?

**Step 8: @SpringBootTest로 전부 띄우고, 진짜 HTTP로, 회원가입→로그인→글쓰기→조회**
전체 시나리오를 검증합니다. 드디어 E2E입니다.
