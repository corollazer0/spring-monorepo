# [Step 12] View 테스트: Thymeleaf 화면과 MockMvc

> **소요 시간**: 약 1.5시간
> **권장 학습 순서**: Step 5(Validation)와 Step 6(Security) 사이 — 번호는 12지만 **필수 코스**입니다
> **이번 Step의 도구**: `@Controller`(vs @RestController), `view().name()`, `model().attribute*()`, `content().string()`, `redirectedUrl*()`, `@ModelAttribute`+`BindingResult`, EntryPoint 분기
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step12/`

---

## 1. Before We Start — 드디어 눈으로 보는 웹앱

지금까지 만든 건 전부 REST API였습니다. 테스트는 다 통과하는데... **보여줄 화면이 없었죠.**
이번 Step에서 Thymeleaf로 화면 5종을 붙입니다. 먼저 직접 띄워보세요:

```bash
.\gradlew :spring-test-onboarding:bootRun
# 브라우저에서 http://localhost:8080/posts
# 로그인: writer1 / spring123!  → 글쓰기까지 해보세요
```

| 화면 | URL | 접근 |
|------|-----|------|
| 글 목록 | `/posts` | 누구나 |
| 글 상세(+댓글) | `/posts/{id}` | 누구나 |
| 글쓰기 폼 | `/posts/new` | 로그인 필요 |
| 로그인 | `/login` | 누구나 |
| 회원가입 | `/signup` | 누구나 |

그리고 질문이 생깁니다 — **화면은 어떻게 테스트하지?** JSON이 아니라 HTML인데?
정답: 검증 "대상"이 바뀔 뿐, 도구(@WebMvcTest + MockMvc)는 그대로입니다.

### 왜 JSP가 아니라 Thymeleaf인가 (테스트 관점 비교)

| 관점 | Thymeleaf | JSP |
|------|-----------|-----|
| Boot 2.7 jar 패키징 | ✅ 공식 지원 | ⚠️ 제약 (war 권장, jar에서 일부 미동작) |
| MockMvc에서 렌더링 | ✅ **실제 HTML 생성** → content() 검증 가능 | ❌ 서블릿 컨테이너가 없어 forward만 검증 — 템플릿 오류를 테스트로 못 잡는다 |
| 템플릿 문법 오류 검출 | 테스트가 잡아준다 | 배포 후 화면 열어봐야 안다 |
| 정적 프로토타이핑 | HTML 그대로 브라우저에서 열림 | 불가 |

> 실무가 JSP라면? 테스트 전략이 달라집니다 — Controller 테스트는 `view().name()`/`model()`까지만
> 검증하고(forward 검증), 렌더링 검증은 배포 환경의 E2E(Selenium 등)로 미뤄야 합니다.
> "MockMvc가 JSP를 렌더링하지 못한다"는 사실 자체를 아는 것이 중요합니다.

---

## 2. What We're Building

```
src/main/java/com/testonboarding/web/      ← SSR 컨트롤러 (REST와 분리)
├── HomeController.java                    ← / → /posts redirect
├── PostViewController.java                ← 목록/상세/작성폼/제출 (example 대상)
└── AuthViewController.java                ← 로그인페이지/가입폼 (example 대상)

src/main/resources/templates/
├── fragments/layout.html                  ← 공통 head/header (로그인 상태별 분기)
├── post/{list,detail,form}.html
└── auth/{login,signup}.html

src/test/java/com/testonboarding/step12/
├── example/PostViewControllerTest.java    ← 뷰/모델/렌더링/보안분기/PRG
├── example/AuthViewControllerTest.java    ← 로그인 페이지 + 가입 폼 두 갈래 에러
├── exercise/PostListViewExerciseTest.java ← 페이징/빈 목록 TODO
└── answer/PostListViewAnswerTest.java
```

---

## 3. Core Concepts

### 3-1. @Controller vs @RestController — 반환값의 의미가 다르다

```java
@RestController → return PostResponse;        // 객체가 JSON 본문이 된다
@Controller     → return "post/list";         // 문자열이 "뷰 이름" — 템플릿으로 렌더링
                → return "redirect:/posts/10"; // redirect 지시
```

데이터는 `Model`에 담아 템플릿에 전달합니다. **비즈니스 로직은 REST와 같은 Service 재사용** —
화면이 늘어도 규칙은 한 곳에 있습니다.

### 3-2. View 테스트의 3단 검증

```java
mockMvc.perform(get("/posts"))
        .andExpect(view().name("post/list"))                      // (1) 올바른 템플릿 선택
        .andExpect(model().attributeExists("posts", "page"))      // (2) 모델 데이터 전달
        .andExpect(content().string(containsString("두번째 글"))); // (3) 렌더링된 HTML 확인
```

(3)이 가능한 건 **Thymeleaf가 MockMvc 안에서 실제로 렌더링되기 때문**입니다.
템플릿의 변수명 오타(`${post.titel}`), `th:if` 분기 누락까지 테스트가 잡아줍니다.
@WebMvcTest 슬라이스에 Thymeleaf 자동구성이 포함되어 있어서 추가 설정도 필요 없습니다.

### 3-3. 한 인증, 두 클라이언트 — EntryPoint 분기

미인증 접근에 대한 올바른 응대는 클라이언트마다 다릅니다.

```
REST 클라이언트(/api/**)  → 401     "프로그램에겐 해석할 상태코드를"
브라우저(화면 URL)         → 302 /login  "사람에겐 따라갈 안내를"
```

```java
.exceptionHandling(ex -> ex
        .defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                new AntPathRequestMatcher("/api/**")))   // api만 401
// 그 외는 formLogin이 등록하는 LoginUrlAuthenticationEntryPoint(→ /login redirect)
```

로그인 성공/실패 응답도 같은 원리로 분기합니다 — `Accept: application/json`이면 200/401,
브라우저면 `redirect:/posts` / `/login?error`. Step 8의 RestSessionHelper가 Accept 헤더를
보내는 이유가 이것입니다.

⚠️ **테스트 함정**: 브라우저 분기를 검증하려면 `accept(MediaType.TEXT_HTML)`을 붙여야 합니다.
MockMvc 기본 요청은 Accept가 "모든 타입"이라 브라우저로 인식되지 않습니다(example의 주석 참고).

### 3-4. 폼 흐름 — @ModelAttribute, BindingResult, PRG

REST와 화면의 입력 처리 비교:

| | REST (Step 4~5) | 화면 (Step 12) |
|---|---|---|
| 입력 | JSON `@RequestBody` | 폼 파라미터 `@ModelAttribute` |
| 검증 실패 | 400 + ErrorResponse JSON | **같은 폼 재표시** + 필드 에러 (입력값 유지) |
| 성공 응답 | 201 + Location | **redirect** (PRG 패턴) |

```java
@PostMapping
public String create(@Valid @ModelAttribute PostCreateRequest request,
                     BindingResult bindingResult,   // ⚠️ @Valid "바로 뒤"여야 한다!
                     Principal principal) {
    if (bindingResult.hasErrors()) {
        return "post/form";                          // 400 대신 폼 재표시
    }
    Long postId = boardService.createPost(principal.getName(), request);
    return "redirect:/posts/" + postId;              // PRG: 새로고침 중복 제출 방지
}
```

BindingResult가 없으면? 검증 실패가 예외로 터져 advice가 가로채고 — 사용자는 JSON을 봅니다.
**BindingResult의 위치(검증 대상 바로 뒤)는 문법 규칙**입니다. 어기면 예외 흐름으로 빠집니다.

### 3-5. CSRF — th:action이 자동으로 처리한다

Step 6에서 REST 테스트마다 `with(csrf())`를 붙였죠. 화면에서는 Thymeleaf의 `th:action`이
**CSRF hidden 필드를 자동으로 폼에 넣어줍니다**. 렌더링된 HTML 소스를 열어보면
`<input type="hidden" name="_csrf" ...>`가 보입니다 — 테스트의 `with(csrf())`가
흉내내던 것의 실물입니다. (로그아웃 버튼이 link가 아니라 POST form인 이유도 CSRF!)

### 3-6. 가입 폼의 두 갈래 에러

| 에러 종류 | REST에서 | 화면에서 |
|----------|---------|---------|
| 형식 검증(@Valid) | 400 + fieldErrors | **필드** 에러 — 입력칸 밑 (`th:errors="*{username}"`) |
| 비즈니스(중복 아이디) | 409 | **글로벌** 에러 — 폼 상단 (`bindingResult.reject(...)`) |

같은 예외가 클라이언트에 따라 "JSON 번역"(advice)과 "폼 안내"(컨트롤러 try-catch)로
다르게 처리됩니다 — example B의 마지막 두 테스트가 이 대비를 보여줍니다.

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step12.*"
```

1. **bootRun으로 화면 먼저 경험** (1장의 절차) — 무엇을 테스트할지 눈으로 확인
2. **PostViewControllerTest** — 3단 검증 → 보안 분기(401 vs 302) → PRG → 검증 실패 폼 재표시
3. **AuthViewControllerTest** — 필드 에러 vs 글로벌 에러
4. **일부러 깨뜨려보기**: `list.html`의 `${post.title}`을 `${post.titel}`로 바꿔 실행 —
   렌더링 검증이 템플릿 오타를 잡는 순간을 목격하세요 (JSP였다면 못 잡습니다!)

---

## 5. Testing — exercise 풀기

`step12/exercise/PostListViewExerciseTest.java`의 TODO 1~7을 채우세요.
페이징 파라미터의 "양쪽 검증"(모델 + verify)과 `th:if` 분기 렌더링 검증이 핵심입니다.

---

## 6. Lessons Learned

### 사례 1: 미인증 테스트가 302가 아니라 401 (이 Step을 만들며 실제로 겪은 버그)

- **증상**: 브라우저로는 분명 로그인 페이지로 가는데, MockMvc 테스트는 401
- **원인**: 브라우저는 `Accept: text/html`을 보내지만 MockMvc 기본값은 모든 타입 허용 —
  로그인 redirect EntryPoint는 "HTML을 원하는 요청"에만 적용된다
- **해결**: `.accept(MediaType.TEXT_HTML)` — "브라우저인 척"을 명시
- **교훈**: 콘텐츠 협상(Accept)이 끼어드는 순간, 테스트는 클라이언트의 정체를 명시해야 한다.

### 사례 2: BindingResult를 빼먹어 검증 실패가 JSON으로

- **증상**: 폼 검증 실패 시 폼 재표시 대신 400 JSON이 화면에 출력
- **원인**: BindingResult 파라미터 누락 → MethodArgumentNotValidException → @RestControllerAdvice가 JSON 번역
- **해결**: `@Valid` 바로 뒤에 `BindingResult` 선언 + 폼 재표시 분기
- **교훈**: 예외 처리에도 "채널"이 있다 — REST는 advice로, 화면은 폼으로.

### 사례 3: 로그아웃을 `<a href>` 링크로 만들었다가 403

- **증상**: 로그아웃 링크 클릭 시 403
- **원인**: 로그아웃은 POST(상태 변경) — GET 링크로는 CSRF 검증을 통과할 수 없다
- **해결**: `th:action` POST form + 버튼 (layout.html 참고)
- **교훈**: Step 6에서 배운 CSRF 규칙은 화면에서도 그대로 — 상태 변경은 POST + 토큰.

### 시니어의 시선

> SSR 화면 테스트에서 가장 가치 있는 한 줄은 `content().string(containsString(...))`입니다.
> 뷰 이름과 모델만 검증하면 "컨트롤러는 맞는데 템플릿이 깨진" 사고를 못 잡습니다.
> 핵심 데이터가 HTML에 "실제로 찍혔는가"까지 — 거기까지가 화면 테스트의 책임 범위입니다.
> 단, 픽셀/레이아웃 검증은 여기서 하지 마세요. 그건 디자이너의 눈과 E2E 도구의 영역입니다.

---

## 7. Key Takeaways

- View 테스트 3단: `view().name()` + `model()` + `content()`(렌더링) — Thymeleaf라서 가능
- 한 인증, 두 클라이언트: /api/**는 401, 화면은 로그인 redirect (EntryPoint 분기)
- 폼 흐름: @ModelAttribute + BindingResult(위치!) + 검증 실패 폼 재표시 + 성공 시 PRG
- th:action = CSRF 자동 처리 — with(csrf())의 실물
- JSP였다면 렌더링 검증 불가 — 템플릿 기술 선택이 테스트 전략을 바꾼다

---

## 8. Next Steps

필수 코스의 모든 조각이 이제 "눈에 보이는 웹앱"으로 완성됐습니다.
Step 6(Security)으로 진행하거나, 이미 완주했다면 Step 9 캡스톤에 화면 테스트를
추가해보세요 — 댓글이 상세 화면에 렌더링되는지 검증하는 테스트는 좋은 보너스 과제입니다.
