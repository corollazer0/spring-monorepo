# [Step 1] 첫 테스트: 순수 JUnit5 + AssertJ

> **소요 시간**: 약 1시간
> **이번 Step의 도구**: `@Test`, `@DisplayName`, `@BeforeEach`, `@Nested`, `@ParameterizedTest`, AssertJ(`assertThat`, `assertThatThrownBy`)
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step01/`

---

## 1. Before We Start — 우리는 지금까지 어떻게 코드를 확인해왔나

새 기능을 만들면 보통 이렇게 확인합니다.

```java
public static void main(String[] args) {
    PasswordPolicyValidator validator = new PasswordPolicyValidator();
    System.out.println(validator.isValid("spring123!"));   // true 나오나 눈으로 확인
    System.out.println(validator.isValid("short"));        // false 나오나 눈으로 확인
}
```

이 방식의 문제는 명확합니다.

1. **눈으로 확인한다** — 출력이 20줄이 되면 어느 줄이 틀렸는지 사람이 찾아야 합니다
2. **확인하고 지운다** — 다음 사람(또는 한 달 뒤의 나)은 같은 확인을 처음부터 다시 합니다
3. **수정하면 또 다시** — 코드를 고칠 때마다 모든 케이스를 수동으로 재확인해야 합니다

테스트 코드는 이 확인 작업을 **컴퓨터가 대신, 매번, 자동으로** 하게 만드는 것입니다.
비유하자면, main()으로 확인하는 것은 요리사가 매번 혀로 간을 보는 것이고,
테스트 코드는 **온도계와 레시피 체크리스트를 주방에 영구 설치**하는 것입니다.

---

## 2. What We're Building

Spring이 전혀 없는 순수 자바 클래스 `PasswordPolicyValidator`의 테스트를 작성합니다.

```
src/main/java/com/testonboarding/member/policy/
├── PasswordPolicyValidator.java   ← 테스트 대상 (example)
└── NicknamePolicyValidator.java   ← 테스트 대상 (exercise — 여러분이 작성)

src/test/java/com/testonboarding/step01/
├── example/PasswordPolicyValidatorTest.java          ← 완성본 — 읽고 실행하며 학습
├── exercise/NicknamePolicyValidatorExerciseTest.java ← @Disabled 제거 후 직접 작성
└── answer/NicknamePolicyValidatorAnswerTest.java     ← 모범답안 — 비교용
```

**왜 Spring 없이 시작하나요?**
`@SpringBootTest`부터 시작하면 "테스트"와 "Spring 컨테이너"가 한 덩어리로 보입니다.
사실 테스트의 본질(준비→실행→검증)은 Spring과 무관하고,
Spring 테스트는 그 위에 "준비 단계를 Spring이 도와주는 것"일 뿐입니다.
본질부터 잡으면 이후 모든 Step이 쉬워집니다.

---

## 3. Core Concepts

### 3-1. 테스트의 3단계: given → when → then

모든 테스트는 이 구조입니다. 예외가 없습니다.

```java
@Test
void isValid_모든정책만족_true반환() {
    // given : 상황 준비 (입력값, 객체 생성)
    String rawPassword = "spring123!";

    // when  : 딱 한 가지 행동 실행
    boolean result = validator.isValid(rawPassword);

    // then  : 결과 검증
    assertThat(result).isTrue();
}
```

| 단계 | 질문 | 위 예시 |
|------|------|--------|
| given | 어떤 상황에서? | 정책을 만족하는 비밀번호가 주어졌을 때 |
| when | 무엇을 하면? | isValid를 호출하면 |
| then | 어떻게 되어야 하나? | true가 나와야 한다 |

### 3-2. 테스트 메서드명 = 명세서

이 모듈의 규칙: **`{대상}_{시나리오}_{예상결과}`**

```java
void isValid_정책위반_false반환()      // 좋음: 이름만 읽어도 명세
void test1()                          // 나쁨: 무엇을 검증하는지 알 수 없음
void isValidTest()                    // 나쁨: 시나리오와 기대 결과가 없음
```

테스트가 실패했을 때 메서드명만 보고 "아, 정책 위반인데 false가 안 나왔구나"를
알 수 있어야 합니다. 테스트 코드는 **실행 가능한 명세서**입니다.

### 3-3. AssertJ — 단언은 한 가지 스타일로

```java
// ✅ 이 모듈의 표준: AssertJ
assertThat(result).isTrue();
assertThat(members).hasSize(3).extracting("name").contains("kim");

// ❌ 사용 금지: JUnit 기본 Assertions
Assertions.assertEquals(true, result);   // 기대값/실제값 순서를 매번 헷갈림
```

AssertJ는 `assertThat(실제값).검증()` 순서가 고정이라 읽기 쉽고,
실패 메시지가 훨씬 친절하며, 컬렉션/예외 검증이 강력합니다.

### 3-4. 예외 검증 — 타입만 보지 말고 "왜"까지

```java
assertThatThrownBy(() -> validator.validate("abc1!"))
        .isInstanceOf(IllegalArgumentException.class)   // 어떤 예외가
        .hasMessageContaining("8자 이상");               // 왜 터졌는지까지
```

메시지를 검증하지 않으면 "길이 미달인데 '숫자 없음' 메시지가 나가는 버그"를 못 잡습니다.
사용자에게 보이는 안내 문구도 동작의 일부입니다.

### 3-5. @ParameterizedTest — 복붙 대신 표

입력만 다르고 검증이 같은 테스트를 4개 복붙하지 마세요.

```java
@ParameterizedTest(name = "[{index}] \"{0}\" 은 정책 위반")
@ValueSource(strings = {"short1!", "spring1234", "springboot!", "spring 123!"})
void isValid_정책위반_false반환(String rawPassword) {
    assertThat(validator.isValid(rawPassword)).isFalse();
}
```

실행하면 입력값별로 한 줄씩 따로 리포트되어, 어떤 입력이 깨졌는지 즉시 보입니다.
입력+기대값 쌍이 필요하면 `@CsvSource`, null/빈값은 `@NullAndEmptySource`를 씁니다.

### 3-6. F.I.R.S.T — 좋은 테스트의 5가지 성질

| 약자 | 의미 | 실천 |
|------|------|------|
| **F**ast | 빠르다 | 순수 단위 테스트는 ms 단위 — 수백 개라도 수 초 |
| **I**solated | 격리된다 | `@BeforeEach`로 매번 새 객체 — 실행 순서 무관 |
| **R**epeatable | 반복 가능 | 언제 어디서 돌려도 같은 결과 (현재 시각/랜덤 의존 금지) |
| **S**elf-validating | 자가 검증 | 결과는 통과/실패 뿐 — 사람이 출력을 읽고 판단하지 않는다 |
| **T**imely | 제때 작성 | 코드와 함께(또는 먼저) 작성 |

---

## 4. Step-by-Step — example 따라잡기

### 4-1. 테스트 실행해보기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step01.*"
```

`PasswordPolicyValidatorTest`가 통과하고, exercise는 **skipped**로 표시됩니다(아직 @Disabled 상태).

### 4-2. example 코드 읽기

`step01/example/PasswordPolicyValidatorTest.java`를 열고 다음 순서로 읽으세요.

1. 클래스 Javadoc — 이 테스트가 보여주려는 5가지
2. `@BeforeEach setUp()` — 왜 매번 새로 만드는가 (테스트 격리)
3. `@Nested IsValid` — 성공 1개 + 파라미터화된 실패 4개 + null/빈값
4. `@Nested Validate` — 예외 타입과 메시지 검증

IDE에서 테스트를 실행하면 `@DisplayName`과 `@Nested` 덕분에
결과 창이 **접을 수 있는 문서**처럼 보입니다. 이것이 테스트 리포트의 가독성입니다.

### 4-3. 일부러 깨뜨려보기 (중요!)

`PasswordPolicyValidator`의 `MIN_LENGTH`를 8에서 10으로 바꾸고 테스트를 돌려보세요.

```
PasswordPolicyValidatorTest > isValid_모든정책만족_true반환 FAILED
    org.opentest4j.AssertionFailedError:
    Expecting value to be true but was false
```

**테스트가 실패하는 경험**이 테스트의 존재 이유입니다 — 누군가 정책을 실수로 바꾸면
사람이 아니라 빌드가 먼저 알아챕니다. 확인했으면 다시 8로 되돌리세요.

---

## 5. Testing — exercise 풀기

1. `step01/exercise/NicknamePolicyValidatorExerciseTest.java`를 연다
2. 클래스 위 `@Disabled(...)` 한 줄을 지운다
3. TODO 1~7을 채운다 (막히면 example을 다시 보세요)
4. 통과 확인:

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step01.exercise.*"
```

5. `answer/NicknamePolicyValidatorAnswerTest.java`와 비교 — 특히
   **예외 메시지까지 검증했는지**, **@ParameterizedTest를 활용했는지**를 보세요.

---

## 6. Lessons Learned — 초보자가 실제로 겪는 문제들

### 사례 1: "테스트가 통과하긴 하는데, 아무것도 검증을 안 하고 있었다"

```java
@Test
void isValid_정상닉네임_true반환() {
    validator.isValid("테스터123");   // 호출만 하고 assertThat이 없음!
}
```

- **증상**: 항상 통과한다 (검증이 없으니 실패할 수 없다)
- **원인**: when만 있고 then이 없음
- **해결**: 모든 테스트에는 최소 1개의 단언이 있어야 한다
- **교훈**: "통과하는 테스트"가 아니라 **"깨질 수 있는 테스트"** 를 만들어야 한다.
  의심되면 프로덕션 코드를 일부러 틀리게 바꿔보라 — 테스트가 안 깨지면 그 테스트는 가짜다.

### 사례 2: 한 테스트에 시나리오를 몰아넣기

```java
@Test
void 닉네임검증_전부() {
    assertThat(validator.isValid("테스터")).isTrue();
    assertThat(validator.isValid("a")).isFalse();
    assertThat(validator.isValid("nick!")).isFalse();   // 여기서 실패하면?
    assertThat(validator.isValid("admin1")).isFalse();  // 이 줄은 실행도 안 됨
}
```

- **증상**: 실패 시 "어떤 규칙이 깨졌는지" 리포트만 봐서는 알 수 없고, 첫 실패 이후 검증은 실행조차 안 된다
- **해결**: 시나리오당 테스트 1개, 반복 입력은 @ParameterizedTest
- **교훈**: 테스트는 가늘고 많게. 하나의 테스트는 하나의 사실만 증명한다.

### 시니어의 시선

> 테스트 코드의 진짜 가치는 "지금 맞다"의 증명이 아니라 **"미래의 변경이 안전하다"** 의 보증입니다.
> 6개월 뒤 누군가 비밀번호 정책을 바꿀 때, 이 테스트들이 영향 범위를 즉시 알려줍니다.
> 그래서 테스트 메서드명에 공을 들이는 겁니다 — 실패 목록 자체가 영향 분석 보고서가 되니까요.

---

## 7. Key Takeaways

- 테스트 = given(준비) → when(실행) → then(검증), 메서드명은 `{대상}_{시나리오}_{예상결과}`
- 단언은 AssertJ로 통일: `assertThat`, `assertThatThrownBy` (+메시지 검증)
- 반복 입력은 `@ParameterizedTest`, 그룹화는 `@Nested`, 격리는 `@BeforeEach`
- 깨질 수 없는 테스트는 테스트가 아니다 — 일부러 깨뜨려서 확인하라
- 의존성 없는 순수 자바 클래스가 테스트하기 가장 쉽다

---

## 8. Next Steps — 다음 Step의 문제

`PasswordPolicyValidator`는 `new` 한 줄이면 됐습니다. 그런데 실제 Service는 이렇게 생겼습니다.

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardDao boardDao;   // DB에 접근하는 DAO가 필요하다!

    public PostResponse getPost(Long postId) { ... }
}
```

`new BoardService(???)` — DAO 자리에 뭘 넣죠? 진짜 DAO는 DB가 있어야 동작합니다.
그럼 Service 로직 하나 테스트하자고 매번 DB를 띄워야 할까요?

**Step 2에서 Mockito로 이 문제를 해결합니다** — "진짜처럼 행동하는 가짜"를 만드는 기술입니다.
