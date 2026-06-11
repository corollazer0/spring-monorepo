# [Batch Step 5] ExecutionContext — Step 간 데이터 공유

> **소요 시간**: 약 1시간
> **이번 Step의 도구**: Step/Job `ExecutionContext`, `ExecutionContextPromotionListener`, `BATCH_*_EXECUTION_CONTEXT` 직렬화
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/context, test/java/com/batchflow/step05}/`

---

## 1. Before We Start — "앞 Step의 결과를 어떻게 받죠?"

> "countStep이 **집계한 대상 건수**를 reportStep이 받아서 보고서에 써야 해요"

초보 시절 누구나 한 번쯤 시도하는 방법들:

```java
private int targetCount;        // ① 멤버 변수?  → 멀티스레드(Step 14)에서 죽음
public static int COUNT;        // ② static?     → 더 빨리 죽음. 재시작하면 0
```

근본 문제: 이 값들은 **JVM 메모리에만** 있습니다. 배치가 죽고 재시작하면 사라지고,
병렬 실행이면 서로 덮어씁니다. Spring Batch의 공식 통로는 **ExecutionContext** —
장부에 직렬화 저장되어 재시작에도 살아남는 "기록되는 상태"입니다.

비유: 각 Step에는 **개인 사물함**(Step EC)이 있고, Job에는 **공용 게시판**(Job EC)이
있습니다. 남의 사물함은 못 보지만, 게시판에 올린 것은 모두가 봅니다.
사물함의 내용을 게시판에 올려주는 사서가 **PromotionListener**입니다.

## 2. What We're Building

```
countStep                                    reportStep
  사물함: targetCount=42, secretNote ──┐        게시판에서 targetCount 읽기 → 42
        PromotionListener(keys=targetCount만!)
  ──────────────▶ Job 게시판: targetCount=42 ──────▶
```

```
src/main/java/com/batchflow/job/context/ShareContextJobConfig.java
src/test/java/com/batchflow/step05/
├── example/ShareContextJobTest.java     ← 승격 / 수신 / 장부 직렬화 확인
├── exercise/PromotionExerciseTest.java  ← 승격되지 않은 키의 운명 추적
└── answer/PromotionAnswerTest.java
```

## 3. Core Concepts

### 3-1. 쓰기와 읽기의 문법

```java
// [countStep] 자기 사물함에 쓰기
contribution.getStepExecution().getExecutionContext().putInt("targetCount", 42);

// [reportStep] 공용 게시판에서 읽기 (Step→Job으로 거슬러 올라간다)
chunkContext.getStepContext().getStepExecution()
        .getJobExecution().getExecutionContext().getInt("targetCount");
```

### 3-2. Promotion은 화이트리스트다

```java
ExecutionContextPromotionListener listener = new ExecutionContextPromotionListener();
listener.setKeys(new String[]{"targetCount"});   // 이것만 승격!
```

countStep은 `secretNote`도 사물함에 넣었지만 keys에 없으므로 게시판에 올라가지
**않습니다** — exercise에서 양쪽(있음/없음)을 직접 증명합니다.
"필요한 것만 공유"는 게으름이 아니라 설계입니다: 게시판이 비대해지면
직렬화 비용이 커지고(아래 함정), Step 간 결합도 늘어납니다.

### 3-3. 전부 장부에 남는다 — 재시작의 복선

example의 세 번째 테스트:

```sql
SELECT SHORT_CONTEXT FROM BATCH_JOB_EXECUTION_CONTEXT  -- {"targetCount":42,...}
```

ExecutionContext는 **BATCH_*_EXECUTION_CONTEXT 테이블에 직렬화**됩니다.
배치가 죽어도 이 기록은 남고, 재시작(Step 12) 때 그대로 복원됩니다 —
Reader가 "어디까지 읽었는지"를 기억하는 비밀도 바로 이것입니다.

### 3-4. 무엇을 넣고, 무엇을 넣지 말 것인가

| 넣어라 | 넣지 마라 |
|--------|----------|
| 건수, 기준값, 체크포인트 같은 **작은 상태** | 대량 데이터(처리 대상 목록 등) — 직렬화 폭탄 |
| 재시작 시 필요한 진행 정보 | DB에서 다시 조회 가능한 것 |

데이터 "전달"이 아니라 상태 "기록"이 본질입니다. 10만 건의 처리 대상을 넘기고
싶다면 답은 ExecutionContext가 아니라 **다음 Step이 직접 읽는 것**(Chunk, Step 6)입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step05.*"
```

1. `ShareContextJobConfig` — 사물함 쓰기 → 승격 → 게시판 읽기 동선 추적
2. example — 승격 확인 → 수신 확인 → 장부 직렬화 확인
3. **일부러 깨뜨려보기**: countStep의 `.listener(promotionListener())`를 지우면
   어떤 테스트가 어떤 예외로 깨지나요? (reportStep의 getInt가 던지는 예외 관찰 후 원복)

## 5. Testing — exercise 풀기

`step05/exercise/PromotionExerciseTest.java`의 TODO 1~5를 채우세요.
같은 키(secretNote)가 "Step 사물함에는 있고 Job 게시판에는 없음"을
양쪽에서 검증하는 것이 핵심입니다.

## 6. Lessons Learned

### 사례 1: ExecutionContext에 처리 대상 목록을 통째로

- **증상**: 배치가 갑자기 느려지고 BATCH_JOB_EXECUTION_CONTEXT가 비대해짐.
  심하면 직렬화 컬럼 길이 초과 에러
- **원인**: 수만 건의 ID 리스트를 컨텍스트로 "전달"하려 함
- **해결**: 컨텍스트에는 기준값/체크포인트만. 대량 데이터는 다음 Step이 직접 조회
- **교훈**: ExecutionContext는 택배가 아니라 메모지다.

### 사례 2: 직렬화 불가 객체를 넣어서 런타임 폭발

- **증상**: 실행 중 `NotSerializableException`
- **원인**: 커스텀 객체(Connection, 도메인 객체 등)를 컨텍스트에 넣음
- **해결**: 원시값/문자열 위주로. 객체가 필요하면 식별자만 넣고 다시 조회
- **교훈**: "장부에 적힌다"는 것은 "직렬화된다"는 뜻 — 적을 수 있는 것만 적어라.

### 시니어의 시선

> ExecutionContext를 잘 쓰는 팀과 못 쓰는 팀의 차이는 재시작 장애 때 드러납니다.
> 진행 상태를 컨텍스트에 기록해온 배치는 "죽은 지점부터" 다시 돌고,
> 멤버 변수로 버텨온 배치는 "처음부터 전부" 다시 돕니다 — 새벽 4시에 그 차이는
> 퇴근 시간의 차이입니다. 상태 설계는 곧 재시작 설계입니다.

## 7. Key Takeaways

- Step EC = 개인 사물함, Job EC = 공용 게시판, Promotion = 화이트리스트 승격
- 모든 컨텍스트는 장부에 직렬화 저장 — 재시작 복원의 기반 (Step 12 복선)
- 작은 상태만 기록하라 — 대량 데이터 전달 통로가 아니다
- 멤버 변수/static 공유는 멀티스레드·재시작에서 반드시 깨진다

## 8. Next Steps — 다음 Step의 문제

기초 체급(Job/파라미터/Flow/Context)을 모두 갖췄습니다. 이제 진짜 문제:

> "회원 **10만 명**의 상태를 갱신해줘"

Tasklet 하나로? — 전부 메모리에 올리다 OOM으로 죽거나, 5만 번째에서 죽으면
**전체가 롤백**됩니다(하나의 트랜잭션이니까). 대량 데이터에는 다른 체계가 필요합니다.

**읽고(reader) → 가공하고(processor) → 묶어서 쓴다(writer), 그리고 트랜잭션은
chunk 단위로 끊는다** — Spring Batch의 심장, **Chunk 모델**이 Step 6에서 시작됩니다.
(드디어 도메인 테이블도 등장합니다!)
