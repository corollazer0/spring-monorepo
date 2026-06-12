# TestCraft 커리큘럼 — 일주일 만에 갖추는 테스트 기본기

> **대상**: 테스트 코드를 한 번도 작성해본 적 없는 SpringBoot 초보 개발자
> **목표**: 일주일(하루 1~2시간) 안에 Controller / Service / DAO / Filter / Interceptor / Security / Validation 전 영역에서
> **스스로 테스트를 설계하고 작성할 수 있는 기본기**를 만든다.
> **스택**: Java 1.8 · Spring Boot 2.7.17 · Spring Security · MyBatis(XML) · H2(MS-SQL 호환 모드) · Thymeleaf · JUnit5 · Mockito · AssertJ

---

## 1. 학습 철학: 문제를 먼저 겪고, 해결책을 배운다

이 커리큘럼은 레이어별 사전식 나열이 아니라 **문제 주도형(problem-driven)** 으로 설계되었습니다.

```
순수 단위 테스트          "DAO가 주입돼야 하는 Service는 new조차 못 하는데?"
   ↓ 한계
Mockito                  "Mock으로는 XML 속 SQL 오타를 못 잡는데?"
   ↓ 한계
@MybatisTest             "URL 매핑, JSON 변환, 상태코드는 누가 검증하지?"
   ↓ 한계
@WebMvcTest              "잘못된 입력과 에러 응답 포맷은? 로그인은? 필터는?"
   ↓ 한계
Validation / Security / Filter
   ↓ 한계
@SpringBootTest E2E      "조각은 다 통과하는데 전체가 한 몸으로 도는 증명은?"
   ↓ 졸업
캡스톤                    "정답지 없이 새 기능의 테스트 전략을 스스로 세운다"
```

각 Step의 시작은 항상 **"앞 Step만으로는 해결할 수 없는 실제 문제"** 입니다.
왜 이 도구가 필요한지 몸으로 느낀 뒤에 도구를 배우기 때문에, 단순 암기가 아닌 **선택 기준**이 남습니다.

---

## 2. 사용 방법 (자기주도 학습 가이드)

### 2-1. 시작하기

```bash
# 레포 클론 후 모듈 테스트가 도는지 먼저 확인
.\gradlew :spring-test-onboarding:test

# 웹앱을 직접 띄워서 눈으로 보기 (Step 12에서 만든 화면)
.\gradlew :spring-test-onboarding:bootRun
# → http://localhost:8080/posts  (로그인: writer1 / spring123!)
```

### 2-2. Step 진행 루틴 (Step당 1~1.5시간)

1. **문서 읽기**: `docs/test/education/FOR-Test-StepNN.md` (20~30분)
2. **example 따라잡기**: `src/test/java/com/testonboarding/stepNN/example/` 의 완성 테스트를 실행해보고, 주석을 읽으며 한 줄씩 이해 (20~30분)
3. **exercise 풀기**: `stepNN/exercise/` 의 `@Disabled`를 제거하고 TODO를 채워 테스트를 통과시키기 (20~30분)
4. **answer와 비교**: `stepNN/answer/` 의 모범답안과 내 코드를 비교하며 차이를 기록

### 2-3. 3종 패키지의 의미

| 패키지 | 역할 | 빌드 상태 |
|--------|------|----------|
| `example` | 해당 주제의 **베스트프랙티스 완성본** — 읽고 실행하며 학습 | 항상 통과 |
| `exercise` | **직접 작성하는 연습문제** — `@Disabled` 제거 후 TODO 채우기 | skipped (제출 전) |
| `answer` | exercise의 **모범답안** — 비교 학습용 | 항상 통과 |

> 💡 `exercise`를 풀다 막히면 30분 이상 붙잡지 말고 `answer`를 보세요.
> 보고 이해한 뒤 **answer를 닫고 다시 스스로 작성**하는 것이 가장 효율적입니다.

---

## 3. 커리큘럼 전체 지도

### 필수 코스 (Step 1~9) — 일주일 목표

| Step | 제목 | 이 Step을 배우는 이유 (앞 단계의 한계) | 핵심 도구 | 소요 |
|------|------|--------------------------------------|----------|------|
| 1 | 첫 테스트: 순수 JUnit5 + AssertJ | main()에 찍어보며 눈으로 확인하던 시절 탈출 | `@Test`, `assertThat`, `assertThatThrownBy`, `@ParameterizedTest`, `@Nested` | 1h |
| 2 | Service 비즈니스 로직 테스트 | Service는 DAO를 주입받는다 — DB 없이는 new조차 못 한다 | `@Mock`, `@InjectMocks`, BDDMockito, `verify`, `ArgumentCaptor` | 1.5h |
| 3 | DAO 테스트: @MybatisTest + H2 | Mock으로는 XML 속 SQL 오타·매핑 실수를 못 잡는다 | `@MybatisTest`, `@AutoConfigureTestDatabase`, `@Sql`, MS-SQL 페이징 | 1.5h |
| 4 | Controller 테스트: @WebMvcTest | URL 매핑·JSON 변환·상태코드는 별개의 세계다 | `MockMvc`, `@MockBean`, `jsonPath` | 1.5h |
| 5 | Validation + 예외 처리 테스트 | 잘못된 입력과 에러 응답 포맷은 누가 보장하나 | `@Valid`, `@RestControllerAdvice`, 400 응답 바디 검증 | 1h |
| 6 | Security 테스트: 인증과 인가 | "작성자 본인만 수정"을 어떻게 테스트하지? | `@WithMockUser`, `csrf()`, 401 vs 403 | 1.5h |
| 7 | Filter & Interceptor 테스트 | 필터/인터셉터 그 자체는 따로 검증해야 한다 | `MockHttpServletRequest`, `MockFilterChain`, `preHandle` | 1h |
| 8 | 통합/E2E: @SpringBootTest | 조각은 통과해도 전체가 한 몸으로 도는 증명이 없다 | `RANDOM_PORT`, `TestRestTemplate`, 세션 로그인 E2E, 테스트 피라미드 | 1.5h |
| 9 | 캡스톤: 댓글 기능 테스트 설계 | 정답지 없이 스스로 테스트 전략을 세울 수 있는가 | 종합 (요구사항 → 테스트 케이스 도출 → 전 레이어 작성) | 2h |
| 12* | View 테스트: Thymeleaf 화면 | API뿐 — 눈으로 볼 화면과 그 검증이 없다 | `view()`/`model()`/`content()` 렌더링 검증, EntryPoint 분기(401 vs 302), `@ModelAttribute`+`BindingResult`, PRG | 1.5h |

> *Step 12는 나중에 추가되어 번호가 12이지만 **필수 코스**입니다.
> **권장 학습 순서: Step 5(Validation)와 Step 6(Security) 사이** — Controller/검증을 배운 직후가 가장 효과적입니다.

### 심화 코스 (선택) — 필수 완주 후

| Step | 제목 | 동기 | 핵심 도구 | 소요 |
|------|------|------|----------|------|
| 10 | JWT 인증 필터 만들고 테스트하기 | 세션을 못 쓰는 환경(모바일/외부 API)이라면? | jjwt, 토큰 생성/만료/위조 테스트, Bearer E2E | 2h |
| 11 | 테스트 품질 끌어올리기 | 테스트가 늘수록 느려지고 준비 코드가 중복된다 | 커스텀 `@WithMockMember`, Fixture 패턴, 컨텍스트 캐싱 | 1.5h |
| 13 | ArchUnit — 아키텍처를 테스트로 봉인 | 리뷰에서 매번 잡는 구조 규칙, 사람 말고 빌드가 지키게 | `noClasses()/classes()` 규칙, 계층/네이밍/모듈 격리, because | 1.5h |
| 14 | Spring REST Docs — 테스트가 문서를 만든다 | 위키 문서는 작성한 날부터 썩는다 | `document()`, requestFields 계약 검증, RestDocumentationRequestBuilders | 1.5h |

> 심화 트랙의 흐름: 인증(10) → 품질(11) → 구조(13) → 문서(14).
> (Step 12는 필수 코스의 View 테스트 — 번호만 심화 사이에 있다)

---

## 4. 도메인 소개: 미니 게시판

모든 Step은 하나의 도메인 위에서 진행됩니다 — **회원(Member) + 게시판(Board) + 댓글(Comment, 캡스톤)**.

```
회원가입 → 로그인(세션) → 글 작성 → 글 수정/삭제(작성자 본인만) → 관리자 기능(/api/admin/**)
```

| 규칙 | 검증 위치 | 배우는 Step |
|------|----------|------------|
| 비밀번호 정책 (8자 이상, 특수문자 포함) | 순수 자바 Validator | Step 1 |
| 없는 글 조회 → 404 | Service 예외 + ExceptionHandler | Step 2, 5 |
| 작성자 본인만 수정/삭제 → 403 | Service 소유자 검증 | Step 2, 6 |
| 제목/내용 필수, 길이 제한 | Bean Validation | Step 5 |
| 미인증 글쓰기 → 401 | Spring Security | Step 6 |
| 관리자만 /api/admin/** | Interceptor + Security | Step 7 |

> DB는 H2를 **MODE=MSSQLServer**(MS-SQL 호환 모드)로 사용합니다. 실무 DB가 MS-SQL이므로
> 페이징(`OFFSET/FETCH`), IDENTITY 채번 등을 실무와 같은 문법으로 연습합니다.
> 단, H2 호환 모드 ≠ 진짜 MS-SQL — 차이점은 Step 3 문서의 비교표를 참고하세요.

---

## 5. 테스트 피라미드와 이 커리큘럼의 대응

```
        ▲  E2E (@SpringBootTest + TestRestTemplate)     ← Step 8   : 적게, 핵심 시나리오만
       ▲▲  슬라이스 (@WebMvcTest, @MybatisTest)          ← Step 3~7 : 레이어 경계 검증
      ▲▲▲  단위 (순수 JUnit, Mockito)                    ← Step 1~2 : 가장 많이, 가장 빠르게
```

| 종류 | 속도 | 뜨는 빈(Bean) | 언제 쓰나 |
|------|------|--------------|----------|
| 순수 단위 | ms | 없음 (Spring 무관) | 비즈니스 규칙, 정책, 유틸 |
| Mockito 단위 | ms | 없음 (Mock 주입) | Service 로직, 협력 객체 호출 검증 |
| `@MybatisTest` | ~1s | MyBatis + DataSource만 | SQL/매핑 정확성 |
| `@WebMvcTest` | ~2s | MVC 레이어만 | URL/JSON/상태코드/검증/인가 |
| `@SpringBootTest` | 수 초 | 전부 | 전체 흐름 E2E (소수 정예) |

---

## 6. 완주 체크리스트

- [ ] Step 1~8, 12 의 모든 exercise를 통과시켰다
- [ ] Step 9 캡스톤에서 Comment 전 레이어 테스트를 스스로 작성했다
- [ ] `.\gradlew :spring-test-onboarding:test` 가 그린이다
- [ ] 새 기능을 맡았을 때 "무엇을 단위/슬라이스/통합으로 검증할지" 스스로 말할 수 있다

---

## 7. 관련 문서

| 문서 | 경로 |
|------|------|
| Step별 교육 가이드 | `docs/test/education/FOR-Test-StepNN.md` |
| 캡스톤 요구사항 | `docs/test/education/FOR-Test-Step09-Requirements.md` |
| 어노테이션 치트시트 | `docs/test/skills/spring-test-annotations.md` |
| 모듈 규칙 | `spring-test-onboarding/CLAUDE.md` |
