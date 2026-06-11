# [Batch Step 9] ItemProcessor & Writer — 부품을 만들고 단위로 검증한다

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `ItemProcessor` 직접 구현(검증/변환/마스킹), `CompositeItemProcessor`, 순수 단위 테스트(Spring 無), `JdbcBatchItemWriter` 개념
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/processor, test/java/com/batchflow/step09}/`

---

## 1. Before We Start — 읽었으면 가공해야지

후보 10명을 손에 쥐었습니다(Step 7~8). 이제 할 일:

1. **검증**: 혹시 ACTIVE가 아닌 회원이 섞여 있으면 걸러낸다 (심층 방어)
2. **변환**: 상태를 DORMANT로 바꾸고 전환 일시를 찍는다
3. **쓰기**: DB에 묶음으로 반영한다

그리고 결정적인 깨달음 하나 — **ItemProcessor는 그냥 자바 클래스입니다.**
`process()`는 그냥 메서드입니다. Job도 DB도 컨텍스트도 필요 없습니다.
TestCraft Step 1~2에서 배운 **순수 단위 테스트의 세계**가 배치 한가운데서 다시 열립니다.

## 2. What We're Building

```
src/main/java/com/batchflow/processor/
├── ActiveOnlyValidationProcessor.java   ← 검증: ACTIVE 아니면 null(필터)
├── DormantConvertProcessor.java         ← 변환: DORMANT + 전환시각(생성자 주입!)
└── EmailMaskingProcessor.java           ← 마스킹 (exercise 대상)

src/test/java/com/batchflow/step09/      ← 전부 순수 단위 — ms 단위로 돈다!
├── example/DormantProcessorTest.java    ← 검증/변환/Composite 체인
├── exercise/EmailMaskingExerciseTest.java
└── answer/EmailMaskingAnswerTest.java
```

## 3. Core Concepts

### 3-1. Processor의 세 가지 시그니처 활용

| 반환 | 의미 | 카운트 |
|------|------|--------|
| 객체 (변환/그대로) | 다음 단계로 | WRITE로 향함 |
| **null** | 이 건은 조용히 버림 | FILTER_COUNT |
| 예외 throw | 처리 실패! | (Step 11의 Skip/Retry 영역) |

null과 예외의 구분이 운영 카운트의 정확성을 결정합니다 — "대상 아님"은 null,
"처리하다 죽음"은 예외.

### 3-2. 시각을 주입받는 변환기 — 결정적 테스트의 조건

```java
public DormantConvertProcessor(LocalDateTime dormantAt) { ... }   // now()를 내부에서 부르지 않는다!
```

내부에서 `LocalDateTime.now()`를 부르면 테스트가 "지금"에 묶입니다.
주입받으면 고정 시각으로 결정적으로 검증합니다 — TestCraft JWT Step의
"설정 주입이 테스트 가능성을 만든다"가 그대로 재등장합니다.

### 3-3. 변환 테스트의 양면 — 바뀐 것과 보존된 것

```java
assertThat(converted.getStatus()).isEqualTo(DORMANT);     // 바뀌어야 할 것
assertThat(converted.getName()).isEqualTo("회원21");       // 보존되어야 할 것!
```

변환 테스트에서 "바뀐 것"만 보면, 변환기가 실수로 다른 필드를 날리는 버그를 놓칩니다.

### 3-4. CompositeItemProcessor — 단일 책임을 체인으로

검증과 변환을 한 클래스에 욱여넣지 않고 **각자 단일 책임**으로 만들어 잇습니다:

```java
composite.setDelegates(Arrays.asList(validationProcessor, convertProcessor));
composite.afterPropertiesSet();   // 수동 조립 시 이 한 줄을 잊기 쉽다!
```

- 체인 중간에 null이 나오면 **거기서 끊긴다** — 뒤 프로세서는 호출되지 않음
  (example의 마지막 테스트: 기존 dormantAt이 덮어쓰이지 않았음으로 증명)
- 각 프로세서를 따로 테스트하고, 체인은 "연결"만 테스트 — 작게 나누면 검증도 작아진다

### 3-5. Writer 미리보기 — JdbcBatchItemWriter

쓰기는 다음 Step에서 조립하지만 개념만:

```java
new JdbcBatchItemWriterBuilder<Member>()
        .dataSource(dataSource)
        .sql("UPDATE member SET status = :status, dormant_at = :dormantAt WHERE member_id = :memberId")
        .beanMapped()    // :이름 ← getter 자동 매핑
        .build();
```

chunk 묶음을 **JDBC batch update 한 방**으로 — 건건이 UPDATE보다 압도적으로 빠릅니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step09.*"
```

실행 시간을 보세요 — **Spring이 없으니 ms 단위**입니다. Step 6~8의 Job 테스트와
체감 비교해보면, "케이스는 단위에 쌓아라"가 왜 진리인지 몸으로 느껴집니다.

**일부러 깨뜨려보기**: example의 Composite 조립에서 `afterPropertiesSet()`을
주석 처리하면? (delegates 검증이 생략된 채 동작하거나 NPE — 직접 관찰 후 원복)

## 5. Testing — exercise 풀기

`step09/exercise/EmailMaskingExerciseTest.java`의 TODO 1~5를 채우세요.
정상/경계값(2자)/불량 입력(필터) — 세 부류를 모두 다루는 것이 채점 기준입니다.

## 6. Lessons Learned

### 사례 1: Processor 테스트를 Job 통합으로만 했던 팀

- **증상**: 마스킹 규칙 하나 바꿀 때마다 Job 통합 테스트 수정 + 수십 초 대기.
  케이스 추가가 귀찮아져 경계값 테스트가 사라짐
- **해결**: 규칙 케이스는 순수 단위로 내리고(ms), Job 테스트는 조립 검증만
- **교훈**: 테스트 피라미드는 배치에도 그대로 적용된다.

### 사례 2: now()가 박힌 변환기

- **증상**: "전환 시각이 정확히 새벽 3시 배치 시작 시각이어야 한다"는 요구를
  검증할 방법이 없음 — 테스트마다 시각이 달라짐
- **해결**: 시각 주입 (JobParameters → @StepScope Bean 생성자로, Step 10에서 연결)
- **교훈**: now()는 경계(주입 지점)에서 한 번만. 로직 내부에 박지 마라.

### 시니어의 시선

> Processor는 배치에서 비즈니스 로직이 사는 곳입니다. 그런데 흔한 실수가
> "Processor에서 DB 조회"입니다 — 건건이 SELECT가 나가면서 배치가 기어갑니다.
> 조회가 필요하면 리더 SQL에서 JOIN으로 가져오거나, Step 시작 때 한 번에 캐싱하세요.
> **Processor는 계산만, I/O는 Reader/Writer에** — 이 경계가 배치 성능의 절반입니다.

## 7. Key Takeaways

- Processor = 순수 자바 → new + process() 직접 호출로 ms 단위 단위 테스트
- null(필터) / 예외(실패, Step 11)의 구분이 카운트 정확성을 만든다
- 시각/설정은 주입 — now() 내장은 비결정적 테스트의 근원
- 변환 테스트는 "바뀐 것 + 보존된 것" 양면 검증
- Composite 체인: 단일 책임 프로세서를 잇고, null이면 중단 (afterPropertiesSet 잊지 말기)

## 8. Next Steps — 다음 Step의 문제

부품이 모두 준비됐습니다 — 리더(7~8), 검증/변환 프로세서(9), 그리고 Writer 설계도.

이제 **조립**입니다: 진짜로 회원 10명의 상태를 DORMANT로 바꾸는
**휴면회원 전환 Job**을 완성하고, "정말 DB가 바뀌었는지"까지 통합 검증합니다.
그리고 처음으로 마주하게 됩니다 — **배치는 진짜로 커밋한다**는 사실이
테스트 격리에 어떤 숙제를 주는지. → **Step 10**
