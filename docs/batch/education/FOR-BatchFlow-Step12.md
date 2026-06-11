# [Batch Step 12] 재시작과 멱등성 — 죽었다 살아나기

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 실패 인스턴스의 같은 파라미터 재실행(=Restart), `saveState(false)`, JobInstance 1:N JobExecution, 장애 주입(SabotageProcessor — 교보재)
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/restart, test/java/com/batchflow/step12}/`

---

## 1. Before We Start — 새벽 4시, 5만 건째에서 죽었다

skip과 retry로도 못 막는 날이 옵니다 — DB 자체가 내려가 배치가 완전히 죽은 새벽.
아침에 DB가 복구됐고, 이제 다시 돌려야 합니다. 두 가지 공포:

1. "처음부터 다시? 이미 처리된 5만 건은?"
2. "Step 3에서 같은 파라미터 재실행은 거부된다고 했는데?"

답은 Step 3에 이미 숨어 있었습니다 — 거부되는 건 **성공(COMPLETED)한** 인스턴스뿐.
**실패(FAILED)한 인스턴스의 같은 파라미터 재실행 — 그것이 재시작(Restart)** 이고,
Spring Batch가 메타데이터 장부를 그렇게 집요하게 쓰는 이유의 종착지입니다.

## 2. What We're Building

통제된 죽음을 위해 **장애 주입 스위치**(SabotageProcessor — ⚠️운영 금지, 교보재!)를
끼운 휴면 전환 Job:

```
1막: 스위치 ON  → 실행 → 회원 26에서 사망 (chunk1의 21~24만 전환된 채 FAILED)
2막: 스위치 OFF (장애 복구)
3막: 🔑 같은 파라미터로 재실행 → 남은 6명만 처리 → COMPLETED
장부: JobInstance 1개 — JobExecution 2개 (FAILED → COMPLETED)
```

```
src/main/java/com/batchflow/
├── processor/SabotageProcessor.java          ← 장애 스위치 (static AtomicBoolean)
└── job/restart/RestartableDormantJobConfig.java  ← saveState(false)가 핵심!

src/test/java/com/batchflow/step12/
├── example/RestartableDormantJobTest.java    ← 3막 드라마 + 성공 인스턴스 거부
├── exercise/RestartLedgerExerciseTest.java   ← 장부에서 1:N 실증
└── answer/RestartLedgerAnswerTest.java
```

## 3. Core Concepts

### 3-1. Step 3 규칙의 완성판

| 인스턴스 상태 | 같은 파라미터 재실행 | 의미 |
|--------------|-------------------|------|
| COMPLETED | ❌ 거부 (AlreadyComplete) | 중복 처리 방지 |
| FAILED | ✅ **허용** | **재시작** — 새 JobExecution이 같은 인스턴스에 추가 |

"같은 파라미터"가 재시작의 열쇠입니다 — 파라미터를 바꾸면 **별개의 새 인스턴스**가
되어 "재시작"이 아니라 "그냥 새 실행"이 됩니다 (exercise의 채점 포인트).

### 3-2. chunk 커밋의 진가 — 죽어도 산 것은 산다

1막에서 FAILED였지만 **chunk1(21~24)의 전환은 살아있습니다** (DORMANT 19명).
Tasklet 하나였다면 전체 롤백 — Step 6에서 배운 "접시 단위 운반"의 보험금이
장애 날 지급되는 순간입니다.

### 3-3. 🔑 이 Step 최대의 함정 — 위치 저장 vs 상태 전이 쿼리

커서 리더는 기본적으로 "어디까지 읽었는지"를 EC에 저장하고(Step 5와 7의 복선),
재시작 시 그만큼 **건너뜁니다**. 좋은 기능 같지만, 우리 쿼리와 만나면 대참사:

```
쿼리: WHERE status='ACTIVE'  ← 처리할수록 대상이 줄어드는 "상태 전이 쿼리"
1차: 10명 중 4명 처리(위치=4 저장) 후 사망
재시작: 쿼리 재실행 → 남은 6명 반환 → 저장된 위치 4만큼 건너뜀 → 2명만 처리!!
   → 25~28 네 명이 영원히 누락 😱
```

해법은 양자택일:

| 전략 | 방법 | 우리의 선택 |
|------|------|------------|
| A. 위치 복원 | 대상 집합을 불변으로 (스냅샷/기준시점 WHERE) + saveState(true) | |
| B. **WHERE 멱등** | 상태 전이 WHERE + **saveState(false)** — 재시작은 그냥 처음부터, 남은 것만 읽힘 | ✅ (Step 10의 자연 멱등성과 한 세트) |

**두 전략을 섞으면 안 됩니다** — 상태 전이 쿼리 + 위치 저장 = 누락. 이 함정은
실제 운영에서 가장 발견하기 어려운 부류입니다("재시작한 날만 일부 누락").

### 3-4. 장부에서 보는 재시작 — 1:N의 실증

```sql
SELECT COUNT(*) FROM BATCH_JOB_INSTANCE WHERE JOB_NAME='restartableDormantJob';  -- 1
SELECT STATUS FROM BATCH_JOB_EXECUTION ORDER BY JOB_EXECUTION_ID;  -- FAILED, COMPLETED
```

Step 3에서 외운 "항공편 1개, 운항 시도 N번"이 SQL 두 줄로 눈앞에 — exercise의 과제입니다.

### 3-5. static 스위치의 정리 책임

SabotageProcessor의 스위치는 static — 테스트 후 정리(@AfterEach OFF)하지 않으면
다른 테스트로 장애가 전염됩니다 (TestCraft Step 7의 ThreadLocal 교훈과 동일).

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step12.*"
```

1. Config — `saveState(false)` 주석을 정독 (이 한 줄이 이 Step의 절반)
2. example — 3막 드라마: FAILED(19명) → 재시작 → read 6 → COMPLETED(25명) → **같은 인스턴스 ID**
3. **일부러 깨뜨려보기**: `saveState(false)`를 지우고 example 실행 —
   재시작 후 DORMANT가 25가 아니라 몇 명이 되는지 직접 목격하라 (3-3의 대참사!) 후 원복

## 5. Testing — exercise 풀기

`step12/exercise/RestartLedgerExerciseTest.java`의 TODO 1~4를 채우세요.
함정 하나: 두 실행의 파라미터가 조금이라도 다르면 "재시작"이 아니라
"새 인스턴스 2개"가 됩니다 — 그러면 then(1)이 깨지면서 스스로 가르쳐줄 겁니다.

## 6. Lessons Learned

### 사례 1: "재시작한 날만 일부 데이터가 누락돼요"

- **증상**: 평소엔 멀쩡, 장애 후 재시작한 날만 수십 건 미처리
- **원인**: 3-3의 함정 — 상태 전이 쿼리 + 위치 저장의 조합
- **해결**: saveState(false) (또는 대상 집합 불변화)
- **교훈**: 재시작 정책은 리더의 쿼리 성격과 함께 설계해야 한다. 따로 정하면 사고.

### 사례 2: 급한 마음에 메타테이블을 지우고 재실행

- **증상**: FAILED 기록이 거슬려 BATCH_* 테이블을 비우고 "깨끗하게" 재실행 →
  이미 처리된 5만 건이 다시 처리됨 (중복 이체!)
- **교훈**: 장부는 재시작의 기억이다. 지우는 순간 배치는 기억상실에 걸린다.
  Step 1의 경고("장부 소각")가 여기서 실체가 된다.

### 사례 3: 재시작했는데 또 같은 곳에서 죽음

- **증상**: 데이터 자체가 문제(포이즌 데이터)라 재시작해도 같은 건에서 반복 사망
- **해결**: 재시작(환경 복구용)과 skip(데이터 격리용)은 다른 도구 — 포이즌 데이터는
  Step 11의 skip+기록으로 격리하고 재시작하라
- **교훈**: "왜 죽었는가"의 진단이 "어떻게 살릴까"보다 먼저다.

### 시니어의 시선

> 재시작 설계의 품질은 장애가 났을 때 운영자가 내려야 하는 **판단의 수**로 측정됩니다.
> 좋은 배치: "복구됐으면 같은 명령으로 다시 실행하세요" — 판단 0개.
> 나쁜 배치: "몇 건까지 됐는지 세어보고, 파라미터를 바꿔서, 단 이 Step은 빼고..." —
> 새벽 4시에 그 판단을 시키는 건 사고를 부르는 설계입니다.

## 7. Key Takeaways

- 재시작 = FAILED 인스턴스의 **같은 파라미터** 재실행 (COMPLETED는 거부 — Step 3 완성)
- chunk 커밋 덕에 죽어도 산 것은 산다 — 재시작은 남은 것만
- 🔑 상태 전이 쿼리 + 위치 저장 = 누락 — saveState(false)로 WHERE에 멱등성을 맡겨라
- JobInstance 1 : JobExecution N — 장부가 재시작의 기억이다 (지우지 마라!)
- static 스위치/상태는 @AfterEach 정리 — 격리는 셀프서비스

## 8. Next Steps — 졸업 시험

필수 개념을 모두 배웠습니다: Job 구조(1~5), Chunk와 읽기/가공/쓰기(6~10),
오류 제어와 재시작(11~12). 이제 정답지 없이 스스로 설계할 차례입니다.

**Step 13 캡스톤: 일일 정산 Job** — bank_transaction을 날짜 기준으로 집계해
settlement에 적재하는, 50-Step 커리큘럼의 최종 프로젝트(43~46)를 압축한 과제입니다.
요구사항 문서와 체크리스트만 주어집니다. 재실행 안전 설계까지가 과제입니다.
