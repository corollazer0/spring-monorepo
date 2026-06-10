# [심화 Step 11] 테스트 품질 끌어올리기

> **소요 시간**: 약 1.5시간 (심화)
> **이번 Step의 도구**: 커스텀 `@WithMockMember`(`WithSecurityContextFactory`), Fixture/Object Mother 패턴, 컨텍스트 캐싱 이해
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/advanced/step11/` + `support/`

---

## 1. Before We Start — 테스트가 100개가 되면 생기는 일

테스트를 꾸준히 쓰다 보면 새로운 종류의 문제가 옵니다. 프로덕션 코드가 아니라
**테스트 코드 자체**의 문제입니다.

1. **준비 코드 중복**: `Member.builder()....build()` 다섯 줄이 테스트 40개에 복붙되어 있다.
   Member에 필드 하나 추가하면 40곳이 깨진다.
2. **도구의 한계**: `@WithMockUser`는 username/role 뿐 — 우리 `LoginMember`의
   memberId, nickname을 쓰는 코드는 테스트할 수 없다.
3. **느려지는 빌드**: 테스트가 늘수록 빌드가 기하급수로 느려진다 — 범인은 대부분
   "컨텍스트가 자꾸 새로 뜨는 것".

테스트 코드도 코드입니다 — 중복 제거, 추상화, 성능이 똑같이 적용됩니다.

---

## 2. What We're Building

```
src/test/java/com/testonboarding/
├── support/
│   ├── MemberFixture.java / PostFixture.java / CommentFixture.java  ← Object Mother
│   └── security/
│       ├── WithMockMember.java                       ← 커스텀 인증 어노테이션
│       └── WithMockMemberSecurityContextFactory.java ← 그 구현 (50줄 미만!)
└── advanced/step11/
    ├── example/WithMockMemberTest.java       ← 도메인 principal 주입 확인
    ├── example/FixtureRefactoringTest.java   ← before/after 비교
    ├── exercise/CommentFixtureExerciseTest.java ← CommentFixture 직접 제작
    └── answer/CommentFixtureAnswerTest.java
```

---

## 3. Core Concepts

### 3-1. 커스텀 @WithMockMember — 인증 어노테이션은 직접 만들 수 있다

`@WithMockUser`의 동작 원리는 단순합니다: `@WithSecurityContext`가 가리키는 팩토리가
테스트 직전에 SecurityContext를 만들어 심는 것. **그 팩토리를 우리가 쓰면 됩니다.**

```java
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMemberSecurityContextFactory.class)
public @interface WithMockMember {
    String username() default "writer1";
    String nickname() default "글쓴이일호";
    long memberId() default 1L;
    Role role() default Role.USER;
}
```

팩토리는 `LoginMember`(우리 도메인 principal)를 통째로 심으므로,
`principal.getMemberId()`를 쓰는 코드도 테스트할 수 있게 됩니다.
도메인 인증 모델이 커질수록(부서, 권한 목록...) 이 50줄의 가치가 커집니다.

### 3-2. Fixture / Object Mother — 준비 코드의 다이어트

```java
// Before: 시나리오와 무관한 잡음 5줄
Post post = Post.builder().postId(1L).writer("writer1")
        .title("원래 제목").content("원래 내용").build();

// After: 이 시나리오의 핵심(누가 썼나)만 보인다
Post post = PostFixture.post(1L, "writer1");

// 특정 필드가 중요하면 그것만 덮어쓴다
Post post = PostFixture.aPost().title("아주 특별한 제목").build();
```

핵심 효과는 짧아짐이 아니라 **신호 대 잡음비**입니다 — 읽는 사람이
"이 시나리오에서 중요한 값"만 보게 됩니다. 부수 효과로, 도메인에 필드가 추가될 때
수정 지점이 Fixture 한 곳으로 모입니다.

규약: 기본값이 채워진 builder를 주는 `aXxx()` + 자주 쓰는 형태의 `xxx(핵심 인자들)`.

### 3-3. 컨텍스트 캐싱 — 빌드 속도의 숨은 지배자

Spring 테스트는 컨텍스트(빈 전체)를 **설정이 같으면 재사용**합니다.
캐시 키 = 어노테이션 구성 + **@MockBean 대상 목록** + 프로퍼티 + 프로파일...

```
@WebMvcTest(BoardController) + @MockBean BoardService          → 컨텍스트 A
@WebMvcTest(BoardController) + @MockBean BoardService, XService → 컨텍스트 B (새로 뜸!)
```

테스트 클래스 10개가 전부 미묘하게 다른 @MockBean 조합이면 컨텍스트가 10번 뜹니다.
대책:

1. 통합 테스트의 구성을 **베이스 클래스나 관례로 통일**한다
2. `@DirtiesContext`(캐시 폭파)는 정말 필요한지 세 번 의심한다
3. 진단: 빌드 로그에서 Spring 기동 횟수를 세어본다

이 모듈도 @WebMvcTest들이 `@Import(SecurityConfig)` + 컨트롤러별 @MockBean 구성을
일관되게 유지해 캐시 적중을 높이고 있습니다.

### 3-4. 어디까지 추상화할까 — 테스트 가독성의 경계

Fixture와 헬퍼가 좋다고 해서 모든 것을 감추면 안 됩니다.

```java
// ⚠️ 과도한 추상화 — 테스트만 봐서는 뭘 검증하는지 알 수 없다
assertPostScenario(Scenario.OWNER_UPDATE_OK);
```

**given의 잡음은 숨기되, when/then은 그대로 드러내라** — 테스트는 명세서이기도 하니까요.

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.advanced.step11.*"
```

1. `support/security/` 두 파일 — @WithMockUser의 마법이 50줄로 풀린다
2. **WithMockMemberTest** — principal이 LoginMember로 심기는 것 확인
3. **FixtureRefactoringTest** — Step 2의 BoardServiceTest와 나란히 놓고 비교

---

## 5. Testing — exercise 풀기

`CommentFixtureExerciseTest`: `CommentFixture`를 **직접 만들어** 장황한 builder를
교체하세요. answer의 `support/CommentFixture.java`와 비교해보세요.

---

## 6. Lessons Learned

### 사례 1: 도메인에 필드 추가 → 테스트 40개 컴파일 에러

- **증상**: Member에 `email` 필드(빌더 필수값 아님에도 패턴 변경)를 추가하자 테스트 수십 개 수정
- **원인**: builder 체인이 테스트마다 복붙되어 있었다
- **해결**: Fixture로 모았다면 수정은 한 곳
- **교훈**: 준비 코드의 중복은 "미래의 수정 비용"이라는 이자를 낳는 빚이다.

### 사례 2: 테스트 1개 추가했는데 빌드가 30초 느려졌다

- **증상**: 새 @SpringBootTest 클래스 하나 추가 후 빌드 급감속
- **원인**: 기존과 다른 @MockBean 조합 → 새 컨텍스트 기동
- **해결**: 기존 클래스와 구성을 맞추거나, Mock이 정말 필요한지 재검토
- **교훈**: 통합 테스트를 추가할 땐 "새 컨텍스트가 뜨는가?"를 항상 자문하라.

### 시니어의 시선

> 테스트 코드의 품질에 투자하는 팀과 아닌 팀의 차이는 6개월 뒤에 나타납니다.
> 전자는 테스트가 자산이 되고, 후자는 "느리고 자주 깨지는 짐"이 되어
> 결국 @Disabled가 늘어나다 테스트 문화 자체가 죽습니다.
> support 패키지를 가꾸세요 — 그곳이 테스트 자산의 곳간입니다.

---

## 7. Key Takeaways

- @WithMockUser는 마법이 아니다 — @WithSecurityContext + 팩토리 50줄이면 도메인 전용 인증을 만든다
- Fixture(Object Mother): 기본값은 공장에, 시나리오 값은 테스트에 — 신호 대 잡음비를 높여라
- 컨텍스트 캐싱: @MockBean 구성이 다르면 새 컨텍스트 — 구성을 통일해 캐시를 지켜라
- given의 잡음은 숨기되 when/then은 드러내라 — 테스트는 명세서다

---

## 8. 수료를 축하합니다 🎓

TestCraft 전 과정(필수 9 + 심화 2)을 완주했습니다. 이제 여러분은:

- 새 기능의 테스트 전략을 계층별로 설계할 수 있고 (Step 9)
- 각 계층의 도구와 함정을 알고 있으며 (Step 1~8)
- 테스트 코드 자체를 자산으로 가꿀 수 있습니다 (Step 10~11)

마지막 당부 한 가지 — **다음에 코드를 작성할 때, 테스트를 먼저(혹은 같이) 쓰세요.**
일주일 동안 배운 모든 것은 습관이 되었을 때만 힘이 됩니다.
막히면 언제든 이 모듈로 돌아오세요. 치트시트(`docs/test/skills/spring-test-annotations.md`)가
가장 빠른 입구입니다.
