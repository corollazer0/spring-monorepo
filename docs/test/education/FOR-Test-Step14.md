# [심화 Step 14] Spring REST Docs — 테스트가 문서를 만든다

> **소요 시간**: 약 1.5시간 (심화 — 필수 코스 + Step 13 완주 후 권장)
> **이번 Step의 도구**: 🆕 `@AutoConfigureRestDocs`, `document()`, requestFields/responseFields/pathParameters, RestDocumentationRequestBuilders
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/advanced/step14/`

---

## 1. Before We Start — 위키의 문서는 언제부터 거짓말이었나

프런트 개발자가 묻습니다:

> "API 문서엔 응답에 nickname이 있다는데, 실제론 안 오는데요?"

조사해보니 석 달 전 응답 DTO를 바꿀 때 위키를 안 고쳤습니다. **문서는 작성한
날부터 썩기 시작합니다** — 코드는 변하는데 문서는 사람이 기억해서 고쳐야 하니까.

그런데 곁에 절대 안 썩는 것이 하나 있죠 — **테스트**. 테스트는 코드와 어긋나는
순간 빨개지니까요. 그렇다면: **테스트가 문서를 만들게 하면?**
그것이 Spring REST Docs의 발상입니다 — 문서 생성의 전제 조건이 "테스트 통과".

## 2. What We're Building

```
@WebMvcTest + .andDo(document("member-signup", requestFields(...), ...))
   ↓ 테스트 통과 시에만
build/generated-snippets/member-signup/
├── http-request.adoc / http-response.adoc   ← 실제 요청/응답 그대로
├── request-fields.adoc                       ← 필드 설명 표
└── curl-request.adoc                         ← 재현 가능한 curl 명령
```

```
advanced/step14/
├── example/MemberApiDocsTest.java       ← 문서화 + 스니펫 실존 확인 + 누락 실패 데모
├── exercise/PostApiDocsExerciseTest.java ← 경로 변수 문서화 (1번 함정 포함)
└── answer/PostApiDocsAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 document() — 기록기이자 검증기

```java
mockMvc.perform(post("/api/members")...)
        .andExpect(status().isCreated())
        .andDo(document("member-signup",
                requestFields(
                        fieldWithPath("username").description("아이디 (4~20자)"),
                        ...)));
```

평소의 @WebMvcTest(Step 4)에 `.andDo(document(...))` 한 단계 — 이게 전부입니다.
그런데 document는 단순 기록기가 아닙니다. **requestFields에 적은 목록과 실제
페이로드가 1:1로 맞지 않으면 테스트가 실패합니다**:

- 실제론 있는데 문서에 없다 → 실패 (누락)
- 문서엔 있는데 실제론 없다 → 실패 (허위)

example의 두 번째 테스트가 이 동작의 데모입니다 — nickname을 빠뜨리자
에러 메시지가 **누락된 필드 이름을 정확히** 알려줍니다. 위키였다면 조용히
어긋난 채 석 달을 갔을 그 누락이, 여기선 커밋조차 못 합니다.

### 3-2. Swagger와의 차이 — 검증의 방향

| | Swagger(어노테이션 방식) | REST Docs |
|---|---|---|
| 작성 위치 | 프로덕션 코드에 @Operation 등 | **테스트 코드**에 document() |
| 코드 오염 | 프로덕션이 문서 어노테이션으로 뒤덮임 | 프로덕션 무변경 |
| 정확성 보장 | 어노테이션이 거짓말해도 모름 | **테스트가 통과해야 문서 생성** |

(Swagger/OpenAPI가 나쁘다는 게 아닙니다 — Try-it-out 같은 강점이 있죠.
"문서의 정확성을 무엇이 보장하는가"의 차이를 이해하는 것이 포인트.)

### 3-3. 🆕 1번 함정 — 경로 변수는 빌더가 다르다

```java
// ❌ MockMvcRequestBuilders.get("/api/posts/{postId}", 1L)
//    → pathParameters 사용 시 "urlTemplate not found" 실패!
// ✅ RestDocumentationRequestBuilders.get("/api/posts/{postId}", 1L)
```

이유: MockMvc 기본 빌더는 URL을 즉시 `/api/posts/1`로 풀어버려서 "어디가
변수였는지" 정보가 사라집니다. RestDocumentationRequestBuilders는 **URL 템플릿을
보존**해서 pathParameters가 문서화할 수 있게 합니다. exercise에서 직접 밟아보세요.

### 3-4. 산출물을 손으로 만져라 — 스니펫 실존 확인

example 테스트의 마지막 단언:

```java
assertThat(Files.exists(Paths.get("build", "generated-snippets",
        "member-signup", "http-request.adoc"))).isTrue();
```

"문서가 생긴다"를 말로 믿지 말고 파일로 확인합니다. 생성된 .adoc을 열어보면
실제 HTTP 요청/응답이 그대로 들어 있습니다 — 손으로 쓴 예시가 아니라
**테스트가 실제로 주고받은 것**이라서 절대 어긋날 수 없습니다.

### 3-5. 스니펫에서 완성 문서로 (이 Step의 범위 밖, 방향만)

스니펫은 문서의 "조각"입니다. 실무 마무리는:

1. `src/docs/asciidoc/api.adoc`에 목차를 쓰고 `include::{snippets}/member-signup/...`
2. asciidoctor Gradle 플러그인이 HTML로 변환, bootJar의 static에 포함

이 모듈은 **스니펫 생성 + 계약 검증**까지만 다룹니다 — 핵심 가치(썩지 않는 문서)는
거기서 이미 완성되고, HTML 변환은 빌드 설정 문제라서요. 키워드:
`org.asciidoctor.jvm.convert` 플러그인.

### 3-6. Mock 스텁 설계 주의 — null 필드는 문서를 깨뜨린다

PostResponse를 스텁할 때 모든 필드를 채우는 이유: null 필드는 JSON 직렬화에서
빠질 수 있고, 그러면 responseFields와 어긋나 실패합니다. **문서화 테스트의
픽스처는 "대표적인 완전한 응답"**이어야 합니다 — Step 11의 Fixture 패턴이
여기서 또 빛을 발합니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.advanced.step14.*"
```

1. `MemberApiDocsTest` 실행 → `spring-test-onboarding/build/generated-snippets/`
   폴더를 **직접 열어** .adoc 파일들을 읽어보기 (curl-request.adoc이 백미)
2. 누락 실패 테스트 — 에러 메시지에서 누락 필드명 확인
3. **일부러 깨뜨려보기**: MemberSignupRequest에 필드를 하나 추가하면 어떤 일이?
   (문서화 테스트가 즉시 실패 — "API가 바뀌면 문서 테스트가 알려준다"의 체감)

## 5. Testing — exercise 풀기

`advanced/step14/exercise`의 TODO 1~3을 채우세요. ⚠️ 주석의 1번 함정
(RestDocumentationRequestBuilders)을 일부러 틀려보고 에러를 읽은 뒤 고치면
오래 기억에 남습니다. 끝나면 `build/generated-snippets/post-get/`을 눈으로 확인.

## 6. Lessons Learned

### 사례: 석 달 묵은 거짓말 문서

- **증상**: 프런트가 위키 문서대로 개발 → 통합 때 필드 불일치 6건 발견, 일정 지연
- **원인**: DTO 변경 시 위키 갱신 누락 — 문서와 코드를 잇는 강제 장치 부재
- **해결**: REST Docs 전환 — 문서화 테스트가 빨가면 머지 불가
- **교훈**: 사람의 기억은 동기화 장치가 아니다. 문서의 정확성은 빌드가 보장하게 하라.

### 사례: 한 시간을 잡아먹은 urlTemplate not found

- **증상**: pathParameters 추가하자 "urlTemplate not found" — 코드를 아무리 봐도 멀쩡
- **원인**: import가 MockMvcRequestBuilders — 기본 빌더는 URL 템플릿을 풀어버린다
- **해결**: RestDocumentationRequestBuilders로 교체 (static import 한 줄 차이!)
- **교훈**: static import 함정은 컴파일러가 못 잡는다 — 이 Step의 1번 함정으로 기억하라.

### 시니어의 시선

> API 문서의 신뢰도를 묻는 제 질문은 하나입니다 — "그 문서, 뭐가 보장하나요?"
> "정기적으로 업데이트해요"는 0점, "어노테이션으로 생성돼요"는 절반,
> "문서화 테스트가 통과해야 생성돼요"가 만점입니다. 보장의 주체가
> 사람 → 코드 → 테스트로 갈수록 문서는 계약에 가까워집니다.

## 7. Key Takeaways

- REST Docs = 테스트가 통과해야만 생성되는 문서 — 썩을 방법이 없다
- document()는 기록기 + 검증기: 필드 누락/허위 모두 테스트 실패 (계약의 봉인)
- 경로 변수 문서화는 RestDocumentationRequestBuilders (1번 함정 — URL 템플릿 보존)
- 프로덕션 코드 무변경 — 문서화 관심사는 테스트에만 산다
- 문서화 픽스처는 "완전한 대표 응답"으로 (null 필드는 계약을 깨뜨린다)
- HTML 변환(asciidoctor)은 선택 — 핵심 가치는 스니펫+검증에서 이미 완성

## 8. Next Steps — TestCraft 졸업

심화 트랙이 완성됐습니다 — **인증(10) → 품질(11) → 구조(13) → 문서(14)**.
동작·구조·계약·문서가 전부 테스트의 보호 아래 있습니다.

이제 이 레포 밖으로: 여러분 팀의 실제 프로젝트에서
- 리뷰 때마다 반복되는 구조 지적 하나를 ArchUnit 규칙으로,
- 가장 자주 어긋나는 API 문서 하나를 REST Docs로

옮겨보세요. 배운 것이 무기가 되는 순간입니다.
(다음 여정: [BatchFlow](../../batch/curriculum/01-BatchFlow-Essential-Curriculum.md) —
화면 없는 세계의 테스트로.)
