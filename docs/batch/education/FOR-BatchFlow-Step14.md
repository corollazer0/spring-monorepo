# [심화 Step 14] Multi-threaded Step & Parallel Flow — 시간을 사는 기술

> **소요 시간**: 약 1.5시간 (심화 — 필수 트랙 완주 후)
> **이번 Step의 도구**: `.taskExecutor()/.throttleLimit()`, thread-safe reader(페이징), `FlowBuilder.split()`, 병렬 테스트의 결정성 원칙
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/parallel, test/java/com/batchflow/advanced/step14}/`

---

## 1. Before We Start — 새벽 배치 시간이 모자라다

데이터는 매년 늘고 배치 윈도우(새벽 1~5시)는 그대로입니다. 4시간짜리 배치를
1시간으로 — 병렬화의 두 가지 차원이 있습니다:

| | Multi-threaded Step | Parallel Flow (split) |
|---|---|---|
| 무엇을 병렬로 | **한 Step 안**의 chunk들 | **서로 무관한 Step들** 통째로 |
| 언제 | 한 업무가 대량일 때 | 독립 업무 여러 개일 때 |
| 비유 | 한 컨베이어에 일꾼 4명 | 컨베이어 자체를 2개 |

## 2. What We're Building

```
multiThreadScanJob : 회원 50명 스캔을 4스레드가 chunk(10)씩 나눠 든다
parallelFlowJob    : 회원 통계 / 거래 통계 Step을 동시에 (split)
```

```
src/test/java/com/batchflow/advanced/step14/
├── example/MultiThreadScanJobTest.java   ← 정확성(50건) + 병렬성(스레드 증거)
├── example/ParallelFlowJobTest.java      ← 두 Step, 서로 다른 flow-* 스레드
├── exercise/ParallelOrderExerciseTest.java ← 결정적/비결정적의 구분
└── answer/ParallelOrderAnswerTest.java
```

## 3. Core Concepts

### 3-1. Multi-threaded Step — 한 줄의 스위치, 무거운 전제

```java
.taskExecutor(multiThreadTaskExecutor())  // 병렬화 스위치
.throttleLimit(4)                          // 동시 chunk 수 상한
```

전제 조건 (어기면 데이터가 깨진다!):
1. **reader가 thread-safe** — `JdbcPagingItemReader` ✅ / 커서 리더 ❌ (Step 8의 복선 회수!)
2. 처리 **순서 무관**한 업무
3. `saveState(false)` — 여러 스레드의 "위치"를 하나로 저장할 수 없다 →
   재시작은 전체 재실행 설계(상태 전이 WHERE 등, Step 12)와 짝지어라

### 3-2. split — 독립 Flow의 동시 실행

```java
Flow splitFlow = new FlowBuilder<Flow>("splitFlow")
        .split(new SimpleAsyncTaskExecutor("flow-"))
        .add(memberStatFlow(), transactionStatFlow())
        .build();
```

전제는 **독립성**: 두 Flow가 같은 데이터를 쓰면(write) 경합 — split의 안전은
설계(분리)에서 온다. split은 모든 Flow가 끝나야 다음으로 진행한다(join).

### 3-3. 병렬 테스트의 황금률 — 결정적인 것만 단정하라

| 결정적 (단정 OK) | 비결정적 (단정 금지!) |
|------------------|----------------------|
| 카운트 합계 (read 50) | 처리/완료 **순서** |
| 스레드 수의 **경계** (1~4) | 정확한 스레드 수 ("정확히 4") |
| 모든 Step 실행됨 (InAnyOrder) | 어떤 항목이 어느 스레드에 |

example B의 `containsExactlyInAnyOrder`, answer의 `hasSizeBetween(1, 4)` —
비결정성을 끌어안는 단언 선택이 "가끔만 실패하는 테스트"를 막는다.

### 3-4. 병렬성의 "증거" 수집 — 교보재 패턴

스레드 이름을 `ConcurrentHashMap.newKeySet()`(static)에 수집해 검증했다.
static 수집함은 테스트 격리의 적 — `@BeforeEach clear()`가 세트다
(SabotageProcessor와 같은 교보재 규약: 운영 코드 금지, 정리 의무).

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.advanced.step14.*"
```

1. MultiThreadScanJobConfig — 스위치 두 줄과 전제 조건 주석
2. example A — **로그에서 batch-mt-1~4가 묶음을 나눠 드는 모습**을 눈으로
3. **일부러 깨뜨려보기**: 페이징 리더를 Step 7의 커서 리더로 바꿔 꽂으면?
   (READ_COUNT가 50이 아니게 되거나 예외 — thread-unsafe의 실체. 원복!)

## 5. Testing — exercise 풀기

`advanced/step14/exercise/ParallelOrderExerciseTest.java`의 TODO 1~4를 채우세요.
"정확히 4 스레드"라고 단정하면 안 되는 이유를 자기 말로 적는 것까지가 과제입니다.

## 6. Lessons Learned

### 사례 1: 병렬화했더니 가끔 합계가 틀린다

- **원인**: 커서 리더(thread-unsafe)에 taskExecutor를 꽂음 — 여러 스레드가 한 커서를 당김
- **교훈**: 병렬화 전 첫 질문은 "reader가 thread-safe인가". Step 8 표를 다시 보라.

### 사례 2: 4배 빨라질 줄 알았는데 1.2배

- **원인**: 병목이 CPU가 아니라 DB(단일 커넥션 풀, 같은 테이블 락)였다
- **교훈**: 병렬화는 병목이 "처리"일 때만 약이다. 먼저 측정하라 — 병목이 DB면
  스레드를 늘려봤자 줄만 길어진다.

### 사례 3: 가끔만 실패하는 병렬 테스트

- **원인**: 완료 순서/스레드 배정 같은 비결정적 사실을 단정
- **교훈**: 3-3의 황금률. flaky 테스트는 없느니만 못하다 — 경계와 합계만 단정하라.

### 시니어의 시선

> 병렬화는 마지막 카드입니다. 그 전에 확인할 것: 쿼리 튜닝(인덱스), chunk/fetch
> 크기, 불필요한 처리 제거. 이 셋으로 안 되면 그때 스레드 — 그리고 스레드로도
> 안 되면 다음 Step의 Partitioning입니다. 복잡도는 빌리는 것이고 이자가 붙습니다.

## 7. Key Takeaways

- Multi-threaded Step: taskExecutor 한 줄 + thread-safe reader(페이징) + saveState(false)
- split: 독립 Flow의 동시 실행 — 안전은 독립성(설계)에서
- 병렬 테스트는 결정적인 것(합계, 경계, InAnyOrder)만 단정
- static 수집함 교보재 = clear() 정리 의무
- 병렬화 전에 측정 — 병목이 DB면 스레드는 답이 아니다

## 8. Next Steps — 다음 Step의 문제

스레드를 4개, 8개… 한 JVM 안에서의 병렬에는 천장이 있습니다. 데이터가 천만 건이라면?

발상의 전환: **데이터 자체를 쪼개라** — "1~100만은 1번 일꾼, 100만~200만은 2번 일꾼…"
범위를 나누는 설계자(Partitioner)와 각자의 범위만 처리하는 일꾼(Worker Step)의 구조,
**Partitioning**이 마지막 Step 15입니다.
