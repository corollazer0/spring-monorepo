# [Step 2] Service 비즈니스 로직 테스트 작성법 (Mockito)

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, BDDMockito(`given/willReturn/willAnswer`), `verify`, `never`, `ArgumentCaptor`
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step02/`

---

## 1. Before We Start — Step 1의 한계

Step 1의 `PasswordPolicyValidator`는 `new` 한 줄이면 테스트 준비가 끝났습니다.
그런데 실제 업무 코드의 주인공인 Service는 이렇게 생겼습니다.

```java
@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardDao boardDao;   // ← DB에 접근하는 협력 객체

    public PostResponse getPost(Long postId) {
        Post post = boardDao.findById(postId);
        ...
    }
}
```

`new BoardService(???)` — DAO 자리에 무엇을 넣어야 할까요?

- 진짜 DAO를 넣는다 → DB가 필요하다 → 느리고, 데이터 상태에 따라 결과가 달라진다
- `null`을 넣는다 → 첫 호출에서 NullPointerException

우리가 검증하고 싶은 건 **Service의 판단 로직**(없는 글이면 예외, 작성자가 아니면 거부)이지
SQL이 아닙니다. 그렇다면 DAO는 **"시키는 대로 행동하는 가짜"** 면 충분합니다.

비유하자면, 자동차의 에어백 ECU를 테스트할 때 실제로 차를 벽에 박지 않습니다.
충돌 센서 자리에 **신호 발생기(가짜 센서)** 를 꽂고 "충돌 신호가 오면 에어백을 터뜨리는가"만 봅니다.
Mockito가 바로 그 신호 발생기를 만들어주는 도구입니다.

---

## 2. What We're Building

```
src/main/java/com/testonboarding/
├── board/service/BoardService.java    ← 테스트 대상 (example)
├── member/service/MemberService.java  ← 테스트 대상 (exercise — 여러분이 작성)
├── board/dao/BoardDao.java            ← 인터페이스만 존재 (구현은 Step 3!)
└── common/exception/...               ← PostNotFound, NotPostOwner, DuplicateUsername

src/test/java/com/testonboarding/step02/
├── example/BoardServiceTest.java           ← 완성본
├── exercise/MemberServiceExerciseTest.java ← @Disabled 제거 후 직접 작성
└── answer/MemberServiceAnswerTest.java     ← 모범답안
```

> 💡 주목: `BoardDao`는 아직 **인터페이스만** 있고 구현(XML)이 없습니다.
> 그런데도 Service 테스트는 완벽하게 돌아갑니다 — Mock이 구현을 대신하니까요.
> "DAO가 완성될 때까지 Service 테스트를 못 한다"는 말은 틀린 말입니다.

검증할 비즈니스 규칙:

| 규칙 | 기대 동작 |
|------|----------|
| 없는 글 조회 | `PostNotFoundException` |
| 작성자 본인 수정/삭제 | 정상 수행 |
| 타인 글 수정/삭제 | `NotPostOwnerException` + **DB 변경 시도 없음** |
| 중복 아이디 가입 | `DuplicateUsernameException` + **저장 시도 없음** |
| 정상 가입 | **인코딩된** 비밀번호 + USER 권한으로 저장 |

---

## 3. Core Concepts

### 3-1. Mock 만들기와 주입: @Mock + @InjectMocks

```java
@ExtendWith(MockitoExtension.class)   // JUnit5에 Mockito를 연결 (Spring은 전혀 없음!)
class BoardServiceTest {

    @Mock
    private BoardDao boardDao;        // 가짜 DAO 생성

    @InjectMocks
    private BoardService boardService; // 위 Mock을 생성자에 넣어 진짜 Service 생성
}
```

`@InjectMocks`는 `new BoardService(boardDao)`를 대신 해주는 것뿐입니다.
**Service는 진짜, 협력 객체만 가짜** — 이것이 단위 테스트의 핵심 구도입니다.

### 3-2. Stubbing — Mock에게 시나리오 주입하기

```java
// "findById(1L)이 호출되면 이 post를 돌려줘"
given(boardDao.findById(1L)).willReturn(post);

// "findById(99L)이 호출되면 null을 돌려줘" (없는 글 시나리오)
given(boardDao.findById(99L)).willReturn(null);
```

이것이 given 단계의 본질입니다 — **"어떤 상황에서"를 코드로 조작하는 것**.
진짜 DB라면 "없는 글" 상황을 만들려고 데이터를 지워야 하지만, Mock은 한 줄이면 됩니다.

### 3-3. 상태 검증 vs 행위 검증 — 언제 무엇을 쓰나

| | 상태 검증 | 행위 검증 |
|---|---------|----------|
| 질문 | 반환값/객체 상태가 맞는가? | 협력 객체를 올바르게 호출했는가? |
| 도구 | `assertThat(result)...` | `verify(mock)...` |
| 예시 | 조회 결과의 제목이 맞는가 | update가 호출됐는가 / **안 됐는가** |

둘 다 필요합니다. 특히 **"호출되지 않았어야 한다"** 는 행위 검증으로만 가능합니다:

```java
// 작성자가 아니면 예외 + "DB 변경이 시도조차 되지 않았다"까지 증명
then(boardDao).should(never()).update(any(Post.class));
```

예외만 검증하고 끝내면 "예외는 던지는데 update가 먼저 실행돼버린" 끔찍한 버그를 놓칩니다.

### 3-4. ArgumentCaptor — Mock에게 전달된 내용물 검사

`verify(boardDao).insert(any())`는 "호출됐다"만 알려줍니다.
**"무엇이 담겨서" 전달됐는지**는 ArgumentCaptor로 꺼내봐야 합니다.

```java
ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
verify(memberDao).insert(captor.capture());

assertThat(captor.getValue().getPassword()).isEqualTo("ENCODED");  // 평문 저장 버그 방지!
```

회원가입에서 비밀번호가 평문으로 저장되는 사고 — Captor 없이는 테스트로 못 잡습니다.

### 3-5. willAnswer — 부수효과 흉내내기

`boardDao.insert(post)`는 반환값이 없는 대신 **전달받은 post에 ID를 채워넣는** 부수효과가
있습니다(실제로는 MyBatis useGeneratedKeys의 동작). Mock으로 이 행동을 흉내낼 수 있습니다:

```java
willAnswer(invocation -> {
    Post saved = invocation.getArgument(0);
    saved.setPostId(10L);    // 진짜 DAO가 하는 일을 시뮬레이션
    return null;
}).given(boardDao).insert(any(Post.class));
```

### 3-6. 무엇을 Mock하고, 무엇은 진짜를 쓰나

`MemberService`에는 협력 객체가 셋 있습니다.

| 협력 객체 | Mock? | 이유 |
|----------|-------|------|
| `MemberDao` | ✅ Mock | DB 필요 — 느리고 외부 의존 |
| `PasswordEncoder` | ✅ Mock | 인터페이스 + "인코딩 결과를 그대로 저장하는가"가 관심사 |
| `PasswordPolicyValidator` | ❌ 진짜 | 순수 자바, 빠름, Mock하면 오히려 검증이 약해진다 |

**원칙: 느리거나(I/O), 통제 불가능한 것만 Mock한다.** 빠르고 결정적인 순수 로직은 진짜를 쓰는 게
더 강한 테스트입니다. "모든 의존성을 기계적으로 Mock"하면 테스트가 구현의 복사본이 돼버립니다.

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step02.*"
```

`BoardServiceTest`를 다음 순서로 읽으세요.

1. 클래스 Javadoc과 `@Mock`/`@InjectMocks` 선언부
2. `GetPost` — 가장 단순한 stubbing + 상태 검증
3. `CreatePost` — willAnswer(부수효과) + ArgumentCaptor
4. `UpdatePost` — **이 모듈의 백미**: 작성자 검증 + `never()` 행위 검증

읽은 뒤 일부러 깨뜨려보세요: `BoardService.validateOwner`에서 `!`를 지우면
(본인일 때 예외가 나게 만들면) 어떤 테스트들이 깨지는지 확인해보세요.

---

## 5. Testing — exercise 풀기

`step02/exercise/MemberServiceExerciseTest.java`의 `@Disabled`를 지우고 TODO 1~10을 채우세요.

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step02.exercise.*"
```

주의: 비밀번호는 **정책을 만족하는 값**(예: `"spring123!"`)을 쓰세요.
`PasswordPolicyValidator`는 Mock이 아닌 진짜라서, 정책 위반 비밀번호를 주면
`DuplicateUsernameException`이 아니라 `IllegalArgumentException`이 터집니다 — 이걸 직접
겪어보는 것도 좋은 학습입니다.

---

## 6. Lessons Learned

### 사례 1: UnnecessaryStubbingException — "안 쓰는 시나리오를 준비했다"

```
org.mockito.exceptions.misusing.UnnecessaryStubbingException:
Unnecessary stubbings detected.
```

- **증상**: 테스트 로직은 맞는 것 같은데 위 예외로 실패
- **원인**: `given(...)`으로 stubbing한 호출이 실제로는 한 번도 실행되지 않음
  (예: 중복 아이디 테스트에서 `passwordEncoder.encode`까지 stubbing — 중복이면 encode 전에 예외가 나므로 불필요)
- **해결**: 그 테스트에서 실제로 일어나는 호출만 stubbing
- **교훈**: Mockito는 "참견쟁이"가 아니라 조력자다 — 이 예외는 **테스트가 시나리오를
  잘못 이해하고 있다**는 신호다. 끄지 말고(lenient) 원인을 고쳐라.

### 사례 2: Mock이 Mock을 검증하는 무의미한 테스트

```java
given(boardDao.findById(1L)).willReturn(post);
Post result = boardDao.findById(1L);          // Service를 안 거치고 Mock을 직접 호출!
assertThat(result).isEqualTo(post);           // 당연히 통과 — Mockito가 잘 동작한다는 것만 증명
```

- **증상**: 항상 통과하지만 Service 코드를 다 지워도 통과한다
- **원인**: when 단계에서 테스트 대상(Service)이 아닌 Mock을 직접 호출
- **교훈**: when 단계에는 반드시 **테스트 대상의 메서드**가 와야 한다.

### 사례 3: 모든 것을 Mock해서 아무것도 검증 못 하는 테스트

PasswordPolicyValidator까지 Mock으로 만들면 "정책 위반 비밀번호도 가입되는 버그"를 영영 못 잡습니다.
Mock은 **경계(I/O)** 에만. 순수 로직은 진짜로.

### 시니어의 시선

> Mockito를 배우면 모든 걸 Mock하고 싶어지는 시기가 옵니다. 그 시기를 빨리 지나치세요.
> 단위 테스트의 목적은 "Mock 사용"이 아니라 **빠르고 결정적인 피드백**입니다.
> 그리고 기억하세요 — Mock 테스트가 아무리 많아도 **SQL이 맞는지는 아무도 모릅니다.**
> 그것이 다음 Step의 주제입니다.

---

## 7. Key Takeaways

- Service 테스트의 구도: **테스트 대상은 진짜, 협력 객체는 Mock** (`@Mock` + `@InjectMocks`)
- given = stubbing으로 상황 조작, then = 상태 검증(assertThat) + 행위 검증(verify)
- "호출되지 않았어야 한다"(`never()`)는 예외 테스트의 단짝이다
- 전달된 객체의 내용은 `ArgumentCaptor`로 검증한다 (평문 비밀번호 버그!)
- Mock은 느리거나 통제 불가능한 경계에만 — 순수 로직은 진짜를 쓴다

---

## 8. Next Steps — 다음 Step의 문제

Step 2에서 우리는 `BoardDao`를 Mock으로 대체했습니다. 그래서 이런 코드가 XML에 숨어 있어도:

```xml
<select id="findById" resultType="Post">
    SELECT post_id, writer, title, content, created_at
    FROM post
    WHERE psot_id = #{postId}   <!-- 오타! psot_id -->
</select>
```

**Step 2의 테스트는 전부 통과합니다.** Mock은 XML을 쳐다보지도 않으니까요.
SQL 오타, 컬럼-필드 매핑 실수, 페이징 문법 오류 — 이것들은 **진짜 DB에 SQL을 날려봐야만** 잡힙니다.

그럼 다시 처음으로 돌아가서 매번 무거운 DB를 띄워야 할까요? 아닙니다.
**Step 3에서 @MybatisTest + 인메모리 H2**로 "가볍게, 그러나 진짜 SQL로" 검증하는 법을 배웁니다.
