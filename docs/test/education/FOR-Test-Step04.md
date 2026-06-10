# [Step 4] Controller 테스트: @WebMvcTest + MockMvc

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `@WebMvcTest`, `MockMvc`, `@MockBean`, `jsonPath`, `@Import(SecurityConfig.class)`, `andDo(print())`
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step04/`

---

## 1. Before We Start — Step 3까지의 한계

Service의 판단 로직(Step 2)도, XML의 SQL(Step 3)도 검증을 마쳤습니다.
그럼 이제 `GET /api/posts/1`은 잘 동작할까요? **아직 아무것도 보장된 게 없습니다.**

```java
@GetMapping("/{postId}")
public PostResponse getPost(@PathVariable Long postId) { ... }
```

여기엔 Service/DAO와 전혀 다른 관심사들이 있습니다.

| 관심사 | 깨지는 예 |
|--------|----------|
| URL 매핑 | `@RequestMapping("/api/post")` 오타 → 404 |
| 파라미터 바인딩 | `?page=2`가 int로 변환되는가, 기본값은 동작하는가 |
| JSON 직렬화 | DTO 필드명 변경 → 프론트엔드가 받던 `title`이 사라짐 |
| 상태코드 | 생성인데 200? 삭제인데 body가 있는? |
| 보안 경계 | 로그인 없이 글쓰기가 되어버리는 사고 |

이 HTTP 세계를 테스트하려고 진짜 Tomcat을 띄우고 진짜 HTTP를 쏠 필요는 없습니다.
**MockMvc**는 서버 기동 없이 DispatcherServlet에 "가짜 HTTP 요청"을 직접 넣어주는 도구입니다.
극장 전체를 빌리지 않고 **무대 리허설**만 하는 셈이죠 — 배우(Controller)와 대본(매핑)은 진짜입니다.

---

## 2. What We're Building

```
src/main/java/com/testonboarding/
├── board/controller/BoardController.java     ← example 대상
├── member/controller/MemberController.java   ← exercise 대상
├── config/SecurityConfig.java                ← SecurityFilterChain 방식 (주의: 구식 Adapter 금지!)
└── auth/CustomUserDetailsService, LoginMember ← 로그인 시 사용자 조회 (Step 6에서 본격 활용)

src/test/java/com/testonboarding/step04/
├── example/BoardControllerTest.java
├── exercise/MemberControllerExerciseTest.java
└── answer/MemberControllerAnswerTest.java
```

---

## 3. Core Concepts

### 3-1. @WebMvcTest — MVC 조각만 띄우는 슬라이스

```java
@WebMvcTest(BoardController.class)   // 이 컨트롤러와 MVC 인프라만!
@Import(SecurityConfig.class)        // 우리의 보안 규칙도 함께 (아래 3-4 참고)
class BoardControllerTest {
    @Autowired MockMvc mockMvc;      // 가짜 HTTP 요청 발사기
    @MockBean BoardService boardService;  // 안 뜨는 Service는 Mock으로 채운다
}
```

| 뜨는 것 | 안 뜨는 것 |
|---------|-----------|
| 지정한 Controller, Jackson(JSON), Validation, Security 필터, ControllerAdvice | Service, DAO, DataSource, 다른 Controller |

### 3-2. @MockBean vs @Mock — 헷갈리지 마세요

| | `@Mock` (Step 2) | `@MockBean` (Step 4) |
|---|---|---|
| 컨테이너 | Spring 없음 | Spring 컨테이너 안 |
| 하는 일 | 그냥 Mock 객체 생성 | **컨테이너의 Bean을 Mock으로 교체** |
| 주입 | `@InjectMocks`가 직접 | Spring DI가 Controller에 주입 |

Controller는 Spring이 만들고 Spring이 주입하므로, Mock도 컨테이너에 넣어야 합니다(@MockBean).
stubbing 문법(`given/willReturn`)은 완전히 동일합니다.

### 3-3. MockMvc 3단 체인: perform → andDo → andExpect

```java
mockMvc.perform(get("/api/posts/1"))         // 요청 만들기
        .andDo(print())                       // 요청/응답 전문 출력 (디버깅 1순위 도구!)
        .andExpect(status().isOk())           // 상태코드 검증
        .andExpect(jsonPath("$.title").value("제목"));  // JSON 본문 검증
```

`jsonPath` 문법: `$`가 루트. `$.title`(필드), `$[0].postId`(배열 원소), `hasSize(2)`(배열 크기).
**jsonPath 검증은 "프론트엔드와의 계약 검증"입니다** — 필드명을 바꾸면 이 테스트가 먼저 깨집니다.

POST는 3종 세트가 필요합니다:

```java
mockMvc.perform(post("/api/members")
        .contentType(MediaType.APPLICATION_JSON)        // "JSON을 보냅니다"
        .content(objectMapper.writeValueAsString(dto))  // 본문
        .with(csrf()))                                  // CSRF 토큰 (Step 6에서 상세히)
```

### 3-4. ⚠️ 이 Step 최대의 사건: @Import(SecurityConfig.class) 없으면 전부 401

`@WebMvcTest`는 Security **필터는 띄우지만**, 우리가 만든 `SecurityConfig`(@Configuration)는
컴포넌트 스캔 대상이 아닙니다. 그러면 Spring Boot의 **기본 보안 설정**이 적용되는데,
기본값은 "**모든 요청은 인증 필요**"입니다.

```
결과: permitAll이어야 할 GET /api/posts 가 401 Unauthorized!
"분명 permitAll인데 왜 401이지?!" ← @WebMvcTest 입문자가 반드시 겪는 사건
```

해결: `@Import(SecurityConfig.class)`로 우리 규칙을 슬라이스에 명시적으로 들여온다.

### 3-5. 무엇을 검증하고, 무엇은 검증하지 않나

Controller 테스트에서 `boardService.getPosts`가 **올바른 목록을 만드는지** 검증하지 않습니다
(그건 Step 2에서 끝났습니다). 검증하는 것은 오직:

1. 요청이 올바른 핸들러에 도달하는가 (URL 매핑)
2. 파라미터가 올바르게 변환되어 Service에 전달되는가 (`verify(boardService).getPosts(2, 5)`)
3. Service의 반환값이 올바른 상태코드+JSON으로 나가는가

**레이어별 테스트는 각자 자기 관심사만** — 이래야 실패했을 때 원인 위치가 바로 보입니다.

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step04.*"
```

`BoardControllerTest` 읽기 순서:

1. 클래스 어노테이션 3종 — 특히 `@Import(SecurityConfig.class)`를 주석 처리하고 돌려보기 (401 사건 체험!)
2. `GetPosts` — jsonPath 배열 검증 + 파라미터 바인딩 verify
3. `GetPost` — 단건 JSON 검증, 그리고 **cliffhanger 테스트**:
   `getPost_없는글_아직은예외가그대로터진다` — 예외가 HTTP 응답으로 변환되지 못하는 현재 상태를 그대로 보여줍니다
4. `CreatePost` — 미인증 401: Security가 Controller보다 앞단에서 차단

---

## 5. Testing — exercise 풀기

`step04/exercise/MemberControllerExerciseTest.java`의 TODO 1~6을 채우세요.

추가 실험:
- `with(csrf())`를 빼고 실행 → 403이 뜹니다. 401(인증 없음)과 403(거부)의 차이는 Step 6에서 정식으로 다룹니다
- `@Import(SecurityConfig.class)`를 지우고 실행 → permitAll이 사라지며 401

---

## 6. Lessons Learned

### 사례 1: "분명 permitAll인데 401이 나와요" (입문자 단골 사건)

- **증상**: SecurityConfig에 분명 permitAll을 설정했는데 @WebMvcTest에서 401
- **원인**: @WebMvcTest 슬라이스는 @Configuration을 스캔하지 않음 → Boot 기본 보안(전부 인증 필요) 적용
- **해결**: `@Import(SecurityConfig.class)`
- **교훈**: 슬라이스 테스트는 "무엇이 로드되고 무엇이 안 되는지"를 아는 것이 절반이다.
  Step 3의 Replace.NONE과 같은 계열의 함정 — **슬라이스의 자동 구성을 의심하라.**

### 사례 2: WebSecurityConfigurerAdapter를 따라 했더니 deprecated

- **증상**: 인터넷 예제(extends WebSecurityConfigurerAdapter)를 복사하니 deprecated 경고, 최신 문서와 구조가 다름
- **원인**: Security 5.7(Boot 2.7)부터 Adapter 상속 방식이 deprecated — 자료의 90%가 구식
- **해결**: `SecurityFilterChain` @Bean 방식 (이 모듈의 SecurityConfig 참고)
- **교훈**: Spring 생태계 검색 결과는 항상 버전을 먼저 확인하라.

### 사례 3: JSON 필드명 회귀

DTO의 `getTitle()`을 `getSubject()`로 리팩토링 → 프론트엔드가 받던 `title` 필드 증발.
컴파일은 통과, Service 테스트도 통과 — **jsonPath 검증을 둔 Controller 테스트만이 이걸 잡습니다.**
API 응답의 필드명은 외부와의 계약(contract)이고, 계약은 테스트로 봉인해야 합니다.

### 시니어의 시선

> Controller 테스트에서 비즈니스 로직까지 검증하려는 유혹이 옵니다. 참으세요.
> Service는 @MockBean으로 밀어두고 **HTTP 계약만** 검증하는 겁니다.
> 반대로 "Service 테스트가 있으니 Controller 테스트는 불필요하다"도 틀렸습니다 —
> URL 오타와 JSON 계약 파괴는 Service 테스트가 영원히 못 잡습니다.
> 각 레이어의 테스트는 서로를 대체하지 않습니다. 서로 다른 종류의 버그를 잡을 뿐입니다.

---

## 7. Key Takeaways

- Controller의 관심사는 HTTP: URL 매핑, 바인딩, JSON 계약, 상태코드 — Service 로직은 @MockBean으로 격리
- `@WebMvcTest` + `@Import(SecurityConfig.class)` — 임포트 없으면 Boot 기본 보안으로 전부 401
- MockMvc 체인: `perform → andDo(print()) → andExpect(status/jsonPath)`
- POST 3종 세트: contentType + content + with(csrf())
- `@MockBean` = 컨테이너의 Bean을 Mock으로 교체 (@Mock의 Spring 버전)

---

## 8. Next Steps — 다음 Step의 문제

이번 Step에서 두 가지 찜찜함이 남았습니다.

1. **없는 글을 조회하면?** — `PostNotFoundException`이 HTTP 응답으로 번역되지 못하고
   그대로 터져나왔습니다. 사용자는 404 대신 끔찍한 500 스택트레이스를 보게 됩니다.

2. **제목이 빈 문자열인 글쓰기 요청이 오면?** — 지금은 그대로 Service까지 흘러들어갑니다.
   `new PostCreateRequest("", "")` 도 멀쩡히 저장되겠죠.

"잘못된 입력을 문 앞에서 거절"(Bean Validation)하고, "예외를 일관된 에러 응답으로 번역"
(GlobalExceptionHandler)하는 것 — **Step 5의 주제입니다.**
