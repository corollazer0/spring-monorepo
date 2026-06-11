# [Batch Step 11] 오류 제어 — Skip, Retry, Listener

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `faultTolerant()`, `skip()/skipLimit()`, `retry()/retryLimit()`, `SkipListener`, `JobExecutionListener`, ROLLBACK_COUNT 해석
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/errorhandling, test/java/com/batchflow/step11}/`

---

## 1. Before We Start — 1건 때문에 9,999건이 멈춘다

Step 10의 완벽해 보이는 Job에 비정상 데이터 하나를 흘리면 — chunk가 롤백되고
Job이 FAILED로 죽습니다. 새벽 배치에서 이건 이런 뜻입니다:

> "이메일 형식이 깨진 회원 1명 때문에, 나머지 9,999명의 휴면 전환이 안 됐습니다"

실무의 처방은 사고의 "종류"에 따라 다릅니다:

| 사고 | 처방 | 비유 |
|------|------|------|
| **데이터가 글러먹음** (형식 불량) | **Skip** — 격리하고 계속 | 검품 불량품을 라인 옆으로 빼고 컨베이어는 계속 |
| **지금만 안 됨** (네트워크 순단, 락 경합) | **Retry** — 다시 시도 | 전화가 안 받으면 잠시 후 재발신 |
| 무슨 일이든 일어났으면 | **Listener** — 기록하고 알려라 | 라인 옆의 사건 일지 + 호출 벨 |

## 2. What We're Building

```
skipDemoJob  : 1~10 처리, 4와 8이 예외 → skip(2건 격리) → 8건 완주, COMPLETED
retryDemoJob : item 5가 두 번 실패 후 성공 → retry → 10건 전부 완주

src/main/java/com/batchflow/
├── job/errorhandling/{SkipDemoJobConfig, RetryDemoJobConfig}.java
└── listener/{JobResultLoggingListener, SkipLoggingListener}.java

src/test/java/com/batchflow/step11/
├── example/{SkipDemoJobTest, RetryDemoJobTest}.java
├── exercise/SkipLedgerExerciseTest.java     ← 장부에서 카운트 등식 완성
└── answer/SkipLedgerAnswerTest.java
```

## 3. Core Concepts

### 3-1. faultTolerant — 오류 제어 모드의 스위치

```java
.faultTolerant()                          // 이 스위치를 켜야 아래가 동작한다
.skip(IllegalArgumentException.class)     // 이 예외는 "건너뛰어도 되는" 종류
.skipLimit(3)                             // 단, 한도까지만 — 초과는 시스템 문제로 간주, 실패
.retry(IllegalStateException.class)       // 이 예외는 "다시 해볼 만한" 종류
.retryLimit(3)
```

**예외 타입이 정책이다**: 어떤 예외를 skip하고 어떤 예외를 retry할지가 곧
오류 설계입니다. 모든 예외를 skip하면? — 시스템 장애(DB 다운)마저 조용히
1만 건을 버리는 배치가 됩니다. skipLimit이 안전핀인 이유.

### 3-2. Skip의 동작 원리 — ROLLBACK_COUNT가 말해주는 것

```
chunk(4,5,6) 처리 중 4에서 예외
  → chunk 통째로 롤백 (ROLLBACK_COUNT +1)
  → 1건씩 재처리: 4 → 또 예외 → skip 확정! / 5, 6 → 정상 commit
```

그래서 example의 카운트: skip 2건(4,8) = **롤백 2회**(각자 다른 chunk).
"롤백이 있는데 COMPLETED"는 skip이 일했다는 흔적입니다.

카운트 등식 완성판: **READ = WRITE + FILTER + SKIP** — exercise에서 장부로 증명합니다.

### 3-3. Retry의 대가 — chunk 동료들도 재처리된다

```
chunk(4,5,6)에서 5가 실패 → 롤백 → 재시도: 4(재처리!), 5, 6(재처리!)
```

재시도는 그 1건만 다시 하는 게 아닙니다 — **chunk가 통째로 다시 돕니다.**
example B의 EC 시도 횟수(3)와 롤백(2)이 그 증거. 따라서:

> ⚠️ **processor는 재처리에 안전(멱등)해야 한다** — "처리 횟수를 외부에 누적"하는
> 류의 로직(포인트 적립, 카운터 증가)이 processor에 있으면 재시도가 데이터를 망가뜨린다.

### 3-4. Skip ≠ Filter — 이제 완전한 구분

| | Filter (Step 6) | Skip (이번) |
|---|---|---|
| 정체 | **설계된 제외** (null 반환) | **사고의 격리** (예외) |
| 카운트 | FILTER_COUNT | SKIP_COUNT 계열 |
| 롤백 | 없음 | 있음 (격리 비용) |
| 기록 의무 | 선택 | **필수** — 사후 보정 근거 |

### 3-5. Listener — 기록 없는 Skip은 데이터 증발이다

```java
public void onSkipInProcess(Integer item, Throwable t) {
    // 실무: 오류 테이블 INSERT → 익일 보정 배치/수동 처리의 근거
    context.putString("skippedItems", existing + item);
}
```

- `SkipListener`: 무엇을 왜 건너뛰었는지 — **건너뛴 건 버린 게 아니라 "미뤄둔 것"**
- `JobExecutionListener.afterJob`: 알림(Slack/메일)의 표준 위치 —
  Step 4의 교훈("복구 경로의 모니터링 노출")이 여기서 해결됩니다

### 3-6. 운영 모니터링의 사각지대 — "성공했지만 완전하지 않은 실행"

exercise의 마지막 질문: EXIT_CODE=COMPLETED인데 SKIP_COUNT=2인 실행.
상태만 보는 모니터링은 이걸 "정상"으로 분류합니다. 구분 장치(skip>0 경고,
오류 테이블 리포트)까지가 오류 제어 설계입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step11.*"
```

1. SkipDemoJobConfig — faultTolerant 3종 세트와 리스너 배선
2. example A — skip 2/write 8/**rollback 2**의 삼각 검증
3. example B — 시도 3회/롤백 2회: "chunk 동료 재처리"의 증거
4. **일부러 깨뜨려보기**: `skipLimit(3)`을 `skipLimit(1)`로 — 4는 스킵되고 8에서
   한도 초과… Job이 어떻게 끝나는지 관찰 (FAILED — 안전핀의 동작!) 후 원복

## 5. Testing — exercise 풀기

`step11/exercise/SkipLedgerExerciseTest.java`의 TODO 1~5를 채우세요.
장부에서 카운트 등식(READ = WRITE + SKIP)을 완성하고, "성공이지만 skip이 있는
실행"을 어떻게 모니터링할지 자신의 아이디어를 적는 것까지가 과제입니다.

## 6. Lessons Learned

### 사례 1: 모든 예외를 skip했다가 1만 건 증발

- **증상**: DB 커넥션 풀 고갈(시스템 장애) 중에도 배치가 "성공" — 알고 보니 1만 건 skip
- **원인**: `.skip(Exception.class)` — 데이터 사고와 시스템 사고를 구분하지 않음
- **해결**: skip은 데이터성 예외만 좁게, skipLimit은 현실적인 작은 값으로
- **교훈**: skip 대상 예외 타입이 곧 정책이다. 넓은 skip은 침묵하는 장애.

### 사례 2: retry에 BackOff 없이 폭격

- **증상**: 외부 API 순단 시 재시도가 0ms 간격으로 몰아쳐 상대 시스템을 더 넘어뜨림
- **해결**: `.backOffPolicy(...)` (Fixed/Exponential) — 재시도 사이에 숨 고르기
- **교훈**: 재시도는 예의 있게. 간격 없는 retry는 DDoS다. (이 데모에는 생략했지만
  외부 연동 retry엔 필수 — WebFlow 모듈의 외부 API Step에서 재회한다)

### 사례 3: processor의 비멱등 로직 + retry = 중복 적립

- **증상**: 재시도가 발생한 날만 일부 회원 포인트가 2배 적립
- **원인**: processor에서 외부 적립 API 호출 — chunk 재처리 때 또 호출됨
- **해결**: 외부 부수효과는 writer로(쓰기 1회 보장 지점), processor는 계산만
- **교훈**: 3-3의 경고는 실화다. "재처리돼도 안전한가"를 processor 체크리스트에.

### 시니어의 시선

> 오류 제어 설계의 본질은 기술이 아니라 **분류**입니다. 이 배치에서 일어날 수 있는
> 예외를 전부 나열하고 — 데이터 사고(skip+기록), 일시 장애(retry+backoff),
> 시스템 장애(즉시 실패+알림) 세 바구니에 나누는 회의 한 시간이,
> 새벽 장애 대응 한 달을 줄입니다.

## 7. Key Takeaways

- faultTolerant + skip/retry — 예외 "타입"의 분류가 곧 오류 정책
- Skip 원리: chunk 롤백 → 1건씩 재처리로 범인 격리 (ROLLBACK_COUNT의 의미)
- Retry는 chunk 동료까지 재처리 — processor 멱등성 필수
- 카운트 등식 완성: READ = WRITE + FILTER + SKIP
- 기록 없는 Skip 금지(SkipListener) / afterJob = 알림의 표준 위치
- "성공이지만 skip>0"의 모니터링 사각지대를 설계로 메워라

## 8. Next Steps — 다음 Step의 문제

skip과 retry로도 못 막는 날이 옵니다 — DB가 내려가서 배치가 **5만 건째에서
완전히 죽은** 새벽. 아침에 DB가 복구됐습니다. 자, 다시 돌립니다.

> "처음부터? 이미 처리된 5만 건은? ...그런데 Step 3에서 '같은 파라미터 재실행은
> 거부된다'고 하지 않았나?"

거부되는 건 **성공한** 인스턴스뿐이었죠. **실패한 인스턴스의 같은 파라미터 재실행 —
그것이 재시작(Restart)** 입니다. 어디서부터 다시 도는지, 무엇이 그걸 가능케 하는지
(힌트: Step 5의 ExecutionContext) — **Step 12**에서 직접 죽이고 살려봅니다.
