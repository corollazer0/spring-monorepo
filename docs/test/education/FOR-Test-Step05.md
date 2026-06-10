# [Step 5] Bean Validation + GlobalExceptionHandler 테스트

> **소요 시간**: 약 1시간
> **이번 Step의 도구**: `@Valid`, `@NotBlank`/`@Size`/`@Pattern`, `@RestControllerAdvice`, `@ExceptionHandler`, 순수 `Validator`(`Validation.buildDefaultValidatorFactory()`)
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step05/`

---

## 1. Before We Start — Step 4가 남긴 두 가지 찜찜함

**찜찜함 1**: 없는 글을 조회하면 `PostNotFoundException`이 HTTP 응답으로 번역되지 못하고
그대로 터져나왔습니다(Step 4의 cliffhanger 테스트). 실서버라면 사용자는 500 스택트레이스를 봅니다.

**찜찜함 2**: 제목이 빈 글쓰기 요청이 오면? 지금은 그대로 Service까지 흘러들어가 저장됩니다.

이 둘은 사실 같은 질문입니다 — **"정상 흐름이 아닐 때, 누가 어떻게 책임지는가?"**

- 잘못된 **입력**은 문 앞에서 거절한다 → **Bean Validation** (`@Valid`)
- 흐름 중 발생한 **예외**는 일관된 에러 응답으로 번역한다 → **GlobalExceptionHandler**

비유하면 공항입니다. 위험물은 **보안검색대**(Validation)가 탑승 전에 거르고,
기내에서 발생한 사고는 **승무원 매뉴얼**(ExceptionHandler)이 정해진 절차로 처리합니다.
승객(Service)은 정상 비행에만 집중하면 됩니다.

---

## 2. What We're Building

```
src/main/java/com/testonboarding/
├── common/exception/
│   ├── ErrorResponse.java           ← 에러 응답 규약 (status/message/fieldErrors)
│   └── GlobalExceptionHandler.java  ← @RestControllerAdvice: 예외→HTTP 번역기
├── board/dto/Post*Request.java      ← @NotBlank/@Size 추가
└── member/dto/MemberSignupRequest   ← @NotBlank/@Size/@Pattern 추가

src/test/java/com/testonboarding/step05/
├── example/MemberSignupRequestValidatorTest.java  ← A: 순수 Validator 단위 테스트
├── example/MemberSignupValidationMvcTest.java     ← B: 웹 계층 400 + fieldErrors
├── example/BoardExceptionHandlingTest.java        ← C: cliffhanger 해결 (404, 500)
├── exercise/PostValidationExerciseTest.java
└── answer/PostValidationAnswerTest.java
```

예외 → 상태코드 매핑 규약:

| 예외 | 상태코드 |
|------|---------|
| `MethodArgumentNotValidException` (@Valid 실패) | 400 + fieldErrors |
| `IllegalArgumentException` (비밀번호 정책 등) | 400 |
| `PostNotFoundException` | 404 |
| `NotPostOwnerException` | 403 |
| `DuplicateUsernameException` | 409 |
| 그 외 `BusinessException` | 400 |
| 그 외 전부 | 500 (**내부 정보 은닉**) |

---

## 3. Core Concepts

### 3-1. 형식 검증 vs 비즈니스 검증 — 책임의 분리

| | 형식 검증 | 비즈니스 검증 |
|---|----------|--------------|
| 질문 | 값의 모양이 맞는가 | 우리 도메인에서 허용되는가 |
| 예 | 필수, 길이, 패턴 | 중복 아이디, 비밀번호 정책, 작성자 본인 |
| 위치 | DTO의 어노테이션 | Service |
| 테스트 | 이 Step | Step 2 |

`MemberSignupRequest`를 보세요 — username의 형식(@Size/@Pattern)은 DTO에,
중복 검사는 Service에 있습니다. **"DB를 봐야 알 수 있는 검증"은 어노테이션의 일이 아닙니다.**

### 3-2. 검증 규칙은 두 단계로 테스트한다

**A. 순수 Validator 테스트** — 규칙 자체를 빠르게, 케이스 많이:

```java
validator = Validation.buildDefaultValidatorFactory().getValidator();  // Spring 불필요!
Set<ConstraintViolation<MemberSignupRequest>> violations = validator.validate(request);
assertThat(violations).hasSize(1);
assertThat(violations.iterator().next().getMessage()).isEqualTo("아이디는 영문 소문자와...");
```

**B. MockMvc 테스트** — 위반 시 "400 + 규약된 JSON"이 나가는지, 대표 케이스 1~2개:

```java
mockMvc.perform(post("/api/members")...)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"));
```

규칙이 10개로 늘면? 케이스는 A에 쌓고, B는 "배선이 연결됐는지"만 봅니다.
**무거운 테스트에 케이스를 쌓지 마라** — 테스트 피라미드의 실전 적용입니다.

### 3-3. "Service가 호출되지 않았다"가 핵심 검증이다

```java
then(memberService).should(never()).signup(any());
```

400이 나가는 것만 보면 부족합니다. **잘못된 입력이 비즈니스 로직에 도달하지 않았다**는
것까지 증명해야 "검증을 우회하는 회귀"를 잡습니다. (Step 2의 never()와 같은 철학)

### 3-4. @RestControllerAdvice도 결국 클래스다

```java
GlobalExceptionHandler handler = new GlobalExceptionHandler();   // 그냥 new!
ResponseEntity<ErrorResponse> response = handler.handleNotPostOwner(exception);
assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
```

"예외 → 상태코드" 매핑 표가 길어질수록, 매핑 검증은 이렇게 가볍게 직접 호출로 하고
MockMvc로는 "advice가 적용되는지"만 확인하면 충분합니다.

### 3-5. 500 응답의 보안 규칙 — 내부 정보를 숨겨라

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
    log.error(">>>>> [ERROR] ...", e);                              // 상세는 로그에만
    return errorResponse(INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다");  // 사용자엔 일반화
}
```

example C의 두 번째 테스트가 이걸 봉인합니다 — 응답에 `"DB connection lost!"`가
노출되면 테스트가 깨집니다. **에러 메시지 노출은 보안 사고**입니다(내부 구조 힌트 제공).

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step05.*"
```

1. **example A** — 순수 Validator: 위반 0건/1건/2건 케이스, 메시지까지 검증
2. **example B** — 400 + fieldErrors JSON + `never()` 검증
3. **example C** — Step 4 cliffhanger의 해결을 직접 확인:
   `step04`의 보존된 테스트(예외가 터짐)와 `step05` example C(404로 번역)를 **나란히 띄워놓고** 비교하세요.
   같은 상황, advice 유무로 갈리는 결말 — 이 대비가 이번 Step의 전부입니다.

---

## 5. Testing — exercise 풀기

`step05/exercise/PostValidationExerciseTest.java`의 TODO 1~8을 채우세요.

- TODO 1~5: PostCreateRequest의 순수 Validator 테스트 (201자 문자열 만들기 힌트 포함)
- TODO 6~8: 핸들러 직접 단위 테스트 — NotPostOwnerException → 403

---

## 6. Lessons Learned

### 사례 1: @Valid를 빼먹은 엔드포인트

- **증상**: DTO에 어노테이션을 다 달았는데 검증이 동작하지 않고 그대로 저장됨
- **원인**: Controller 파라미터에 `@Valid`가 없음 — 어노테이션 선언만으로는 아무 일도 안 일어난다
- **해결**: `@Valid @RequestBody` + **example B 같은 웹 계층 테스트가 이 누락을 잡는다**
  (순수 Validator 테스트 A는 이 버그를 못 잡는다! — 그래서 B가 필요하다)
- **교훈**: A와 B는 서로 다른 버그를 잡는다. 한쪽만으로는 구멍이 생긴다.

### 사례 2: 에러 메시지가 그대로 노출된 운영 사고

운영 중 NullPointerException의 스택트레이스가 사용자 화면에 그대로 출력 —
내부 패키지 구조와 클래스명이 노출되고, 보안 점검에서 지적됐습니다.
`Exception.class` 핸들러(최후의 보루) + "메시지 일반화" 테스트가 있었다면 배포 전에 잡혔습니다.

### 사례 3: 닉네임 빈 값에 위반이 2건?

`""`는 `@NotBlank`와 `@Size(min=2)`를 **동시에** 위반합니다 — Bean Validation은
모든 규칙을 한 번에 평가합니다. `hasSize(1)`로 단정했다가 깨지는 경우가 흔하니,
**위반 건수는 직접 실행해 확인하고 단정**하세요. (example A의 세 번째 테스트)

### 시니어의 시선

> 에러 응답은 "장애 시의 API"입니다. 정상 응답만 계약(jsonPath)으로 봉인하고
> 에러 응답은 방치하는 경우가 많은데, 프론트엔드 입장에선 에러 처리야말로
> 포맷이 흔들리면 안 되는 영역입니다. ErrorResponse라는 **단일 규약** + 그걸 봉인하는
> 테스트 — 이 한 쌍이 "에러 처리 코드가 화면마다 제각각"이 되는 미래를 막아줍니다.

---

## 7. Key Takeaways

- 형식 검증은 DTO(@Valid + 어노테이션), 비즈니스 검증은 Service — 책임을 섞지 마라
- 검증 규칙은 순수 Validator로 많이(A), 웹 배선은 MockMvc로 대표 케이스만(B)
- 검증 실패 시 `never()`로 "Service 미호출"까지 증명하라
- @RestControllerAdvice도 new 해서 단위 테스트할 수 있다
- 500 응답에 내부 메시지를 노출하지 마라 — 그리고 그것을 테스트로 봉인하라

---

## 8. Next Steps — 다음 Step의 문제

에러 응답까지 정리됐습니다. 그런데 아직 정면으로 다루지 않은 거대한 영역이 있습니다.

```java
// 이 규칙을 어떻게 테스트하지?
"로그인 안 한 사용자는 글을 못 쓴다"          → 401
"작성자가 아닌 사용자는 남의 글을 못 고친다"   → 403
"ADMIN만 관리 기능에 접근할 수 있다"
```

테스트마다 진짜로 회원가입하고 로그인해서 세션을 받아야 할까요? 너무 무겁습니다.
그리고 Step 4부터 계속 붙여온 정체불명의 `with(csrf())` — 이게 대체 뭘까요?

**Step 6에서 spring-security-test의 @WithMockUser로 "로그인한 척"하는 법**과
401 vs 403의 구분, csrf의 정체를 정면으로 다룹니다.
