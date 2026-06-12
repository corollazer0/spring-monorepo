# [심화 Step 13] ArchUnit — 아키텍처를 테스트로 봉인한다

> **소요 시간**: 약 1.5시간 (심화 — 필수 코스 + Step 10~11 완주 후)
> **이번 Step의 도구**: 🆕 ArchUnit (`ClassFileImporter`, `noClasses()/classes()` 규칙 빌더, `because`)
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/advanced/step13/`

---

## 1. Before We Start — 리뷰에서 매번 잡는 그 규칙

코드리뷰에서 이런 지적, 몇 번이나 반복했나요?

- "Controller에서 DAO를 직접 부르시면 안 돼요 — Service 거쳐야죠"
- "도메인에 Spring 어노테이션 넣지 마세요"
- "예외 클래스는 common.exception에 모으기로 했잖아요"

규칙은 문서(CLAUDE.md)에 있습니다. 그런데 문서는 **강제력이 없습니다** —
신규 입사자가 어겨도, 바쁜 금요일의 내가 어겨도, 머지되면 그만이죠.
동작은 테스트가 지키는데(Step 1~12), **구조는 누가 지키나요?**

ArchUnit의 답: 구조도 테스트다. 바이트코드를 읽어 의존 그래프를 만들고,
규칙을 단언한다 — 어기면 **빌드가 빨갛다.**

## 2. What We're Building

```
[계층]   Controller ↛ DAO (지름길 금지) / Service ↛ Controller (역류 금지)
        domain ↛ org.springframework (순수성)
[헌법]   ↛ com.batchflow / com.webflow (모듈 격리 — 루트 CLAUDE.md의 기계화!)
[규약]   ..controller.. = *Controller, ..dao.. = *Dao(인터페이스), ..service.. = *Service
        *Exception은 common.exception에 + RuntimeException 계열
```

```
advanced/step13/
├── example/ArchitectureRulesTest.java  ← 계층 3 + 헌법 1 + 규약 3
├── exercise/ArchRulesExerciseTest.java ← DTO 순수성 / policy Validator 규칙
└── answer/ArchRulesAnswerTest.java
```

새 프로덕션 코드가 **하나도 없다**는 점에 주목 — 검증 대상은 Step 1~12 동안
여러분이 이미 쌓아온 구조 그 자체입니다.

## 3. Core Concepts

### 3-1. 🆕 동작의 봉인 vs 구조의 봉인

| | 지금까지의 테스트 | ArchUnit |
|---|---|---|
| 봉인 대상 | 동작 ("이 입력이면 이 출력") | **구조** ("이 패키지는 저 패키지를 모른다") |
| 깨지는 순간 | 로직 회귀 | 아키텍처 침식 (지름길, 역류, 흩어짐) |
| 읽는 것 | 실행 결과 | **바이트코드의 의존 그래프** |

실행이 없으므로 Spring 컨텍스트도 DB도 필요 없습니다 — 순수 단위 테스트만큼
빠르고, 100% 결정적입니다.

### 3-2. 규칙 빌더 — 영어 문장처럼 읽힌다

```java
noClasses().that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..dao..")
        .because("Controller→Service→DAO 단방향 — 지름길은 검증과 트랜잭션을 건너뛴다")
        .check(productionClasses);
```

`..controller..`의 `..`는 "임의 깊이의 패키지" 와일드카드입니다.
**because가 핵심 습관** — 규칙이 깨졌을 때 후임자가 보는 에러 메시지에 "왜 이
규칙이 있는지"가 박힙니다. 규칙은 금지가 아니라 **이유가 있는 설계 결정**이어야
하니까요.

### 3-3. import는 한 번만 — 그리고 테스트는 빼고

```java
@BeforeAll
static void importClasses() {
    productionClasses = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.testonboarding");
}
```

클래스 스캔은 비쌉니다(수 초) — 테스트마다 하면 낭비라서 `@BeforeAll`에서 한 번만.
Step 11의 컨텍스트 캐싱과 같은 발상입니다. `DoNotIncludeTests`는 검증 대상을
프로덕션으로 한정합니다 — step04 테스트가 controller와 dao를 동시에 import하는 건
테스트의 당연한 권리니까요.

### 3-4. 봉인의 백미 — "Step 1이 가능했던 이유"의 구조화

Step 1에서 `PasswordPolicyValidator`를 `new`로 테스트할 수 있었던 것,
기억하나요? 그게 가능했던 건 **policy가 Spring을 모르기 때문**입니다.
exercise의 TODO 3이 바로 그 규칙입니다:

```java
noClasses().that().resideInAPackage("..policy..")
        .should().dependOnClassesThat().resideInAPackage("org.springframework..")
```

이 테스트가 그린인 한, 누가 무심코 `@Component`를 붙여도 빌드가 막습니다 —
**순수 단위 테스트 가능성이 우연이 아니라 봉인된 설계**가 됩니다.
domain 순수성 규칙도 같은 이치입니다.

### 3-5. 모노레포 헌법의 기계화

루트 CLAUDE.md 1조: "모듈 간 상호 참조 금지." 지금까지 AI와 사람이 지켰다면,
이제부터는 한 줄의 규칙이 지킵니다:

```java
noClasses().should().dependOnClassesThat()
        .resideInAnyPackage("com.batchflow..", "com.webflow..")
```

문서의 규칙과 빌드의 규칙이 일치할 때, 문서는 비로소 신뢰를 얻습니다.

### 3-6. 도입의 현실 순서 — 측정 먼저, 봉인은 나중

기존 프로젝트에 ArchUnit을 들이댈 때의 함정: 규칙부터 쓰면 **위반 수백 건과 함께
빌드가 영원히 빨갛습니다.** 현실 순서는:

1. 규칙을 써서 **현황을 측정**한다 (몇 건 위반인가)
2. 위반을 정리한다 (또는 `FreezingArchRule`로 기존 위반만 동결)
3. 그 다음에 봉인한다 — 신규 위반만 잡도록

이 레포는 처음부터 규칙대로 지어져서 1~2단계가 생략됐을 뿐입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.advanced.step13.*"
```

1. `ArchitectureRulesTest` — 7개 규칙을 because와 함께 읽기
2. **일부러 깨뜨려보기 (이번 Step의 백미)**: MemberController에
   `private final MemberDao memberDao;` 한 줄을 추가하고 테스트를 돌려보세요 —
   에러 메시지에 **위반한 클래스·라인·because 사유**까지 박혀 나옵니다. 원복 필수!
3. 같은 방식으로 Member 도메인에 `@Component`를 붙여보기 → domain 순수성 규칙이 잡는다

## 5. Testing — exercise 풀기

`advanced/step13/exercise`의 TODO 1~3을 채우세요. TODO 3(policy의 Spring 비의존)이
이 Step의 본전입니다 — Step 1의 편안함을 구조로 봉인하는 일.

## 6. Lessons Learned

### 사례: 6개월 만에 무너진 계층

- **증상**: 새 기능마다 버그 — 원인 추적이 안 됨. 조사해보니 Controller 23곳이
  DAO를 직접 호출, 트랜잭션·소유자 검증 누락이 산재
- **원인**: 규칙은 위키에만 있었다 — 바쁠 때마다 "이번만" 지름길, 리뷰는 놓침
- **해결**: ArchUnit 도입 — 측정(23건) → 정리(2주) → 봉인. 이후 신규 위반 0
- **교훈**: 사람의 규율은 마감 앞에서 진다. 규칙은 빌드에게 맡겨라.

### 사례: 영원히 빨간 아키텍처 테스트

- **증상**: ArchUnit 도입 첫날부터 위반 400건 — 팀이 테스트를 @Disabled 처리
- **원인**: 측정 없이 이상적인 규칙부터 봉인 — 현실과 규칙의 간극 무시
- **해결**: FreezingArchRule로 기존 위반 동결 + 신규만 차단, 분기마다 동결분 상환
- **교훈**: 봉인은 마지막 단계다. 측정 → 정리(또는 동결) → 봉인의 순서를 지켜라.

### 시니어의 시선

> 아키텍처 문서와 코드가 다른 조직은 흔합니다. 문서와 "아키텍처 테스트"가 함께
> 있는 조직은 드뭅니다. 제가 새 팀에 가면 제일 먼저 보는 것 중 하나가 이것 —
> 구조 규칙이 어디에 사는가. 위키에 살면 죽은 규칙, 빌드에 살면 산 규칙입니다.

## 7. Key Takeaways

- 동작은 일반 테스트가, 구조는 ArchUnit이 봉인한다 — 바이트코드 분석, 실행 불필요
- 규칙엔 반드시 because — 깨진 순간의 에러 메시지가 곧 설계 문서
- import는 @BeforeAll 한 번 + DoNotIncludeTests (검증 대상은 프로덕션)
- 순수성 규칙(domain/policy ↛ Spring) = 순수 단위 테스트 가능성의 구조적 보장
- 모듈 격리 같은 "헌법"도 한 줄 규칙으로 기계화된다
- 기존 프로젝트 도입은 측정 → 정리/동결 → 봉인 순서로

## 8. Next Steps — 다음 Step의 문제

구조까지 봉인했습니다. 그런데 협업의 다른 축이 무너져 있네요 — 프런트 개발자가
묻습니다:

> "위키의 API 문서요, 응답에 nickname 필드가 있다는데 실제론 안 오는데요?"

문서는 작성한 날부터 썩기 시작합니다 — 코드는 변하는데 문서는 사람이 고쳐야
하니까. 테스트는 항상 코드와 일치하는데(안 그러면 빨가니까), **그럼 테스트가
문서를 만들면 되지 않나?**

**심화 Step 14: Spring REST Docs** — 테스트가 통과해야만 문서가 생성되는 세계입니다.
