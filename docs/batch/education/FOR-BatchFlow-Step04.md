# [Batch Step 4] Flow 제어 — 분기와 Decider

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `ExitStatus`, `.on()/.to()/.from()/.end()`, `contribution.setExitStatus()`, `JobExecutionDecider`, `FlowExecutionStatus`
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/flow, test/java/com/batchflow/step04}/`

---

## 1. Before We Start — 일직선으로는 부족하다

지금까지의 Job은 Step이 한 줄로만 흘렀습니다. 실무 요구는 다릅니다.

> "검증 Step이 **실패하면 복구 Step**으로, **성공하면 본처리**로 보내줘"
> "대상 건수가 0이면 메인 처리를 **건너뛰고** 알림만 보내줘"

철도에 비유하면 지금까지는 단선 철로였고, 이제 **분기기(전철기)** 를 놓을 차례입니다.
분기의 신호가 바로 Step 2에서 스쳐 지나간 **ExitStatus**입니다.

## 2. What We're Building

```
[conditionalFlowJob]                          [oddEvenDeciderJob]
checkStep ──ExitStatus=FAILED──▶ recoveryStep   numberLoadStep → (Decider) ─EVEN→ evenStep
    └──────그 외(*)───────────▶ mainStep                            └──ODD→ oddStep
```

```
src/test/java/com/batchflow/step04/
├── example/ConditionalFlowJobTest.java    ← on/to 분기 + 🚨 "FAILED인데 Job은 COMPLETED"
├── example/OddEvenDeciderJobTest.java     ← Decider 분기 (장부에 안 남는 판단자)
├── exercise/ConditionalFlowExerciseTest.java ← STATUS vs EXIT_CODE를 장부에서 증명
└── answer/ConditionalFlowAnswerTest.java
```

## 3. Core Concepts

### 3-1. 분기의 문법 — on/to/from/end

```java
jobBuilderFactory.get(JOB_NAME)
        .start(checkStep(null))
            .on("FAILED").to(recoveryStep())   // ExitStatus가 FAILED면 → 복구
        .from(checkStep(null))
            .on("*").to(mainStep())            // 그 외 전부 → 본처리
        .end()                                  // Flow 정의 끝 (빼먹으면 컴파일 에러!)
        .build();
```

- `.on("코드")`은 **ExitStatus 문자열**과 매칭된다 (BatchStatus가 아니다!)
- `"*"`(전부), `"COMPLETED*"`(패턴) 와일드카드 가능
- 같은 Step에서 여러 갈래를 내려면 `.from(그 Step)`으로 돌아온다

### 3-2. 🚨 이 Step 최대의 반전 — "FAILED로 분기했는데 Job은 COMPLETED"

checkStep은 예외를 던지지 않습니다. 정상 종료하면서 **흐름 제어 코드만** 칠합니다:

```java
contribution.setExitStatus(ExitStatus.FAILED);  // "다음 행선지는 복구"라는 신호일 뿐
return RepeatStatus.FINISHED;                    // Step 자체는 멀쩡히 종료
```

그 결과 장부에는 한 행에 **서로 다른 두 상태**가 남습니다 (exercise에서 직접 확인):

| BATCH_STEP_EXECUTION (checkStep) | 값 | 의미 |
|---|---|---|
| STATUS (BatchStatus) | COMPLETED | 프레임워크: "정상 종료했다" |
| EXIT_CODE (ExitStatus) | FAILED | 흐름 제어: "복구 경로로 보내라" |

그리고 분기 끝에 정의된 경로로 잘 흘러갔으므로 **Job의 최종 상태는 COMPLETED**입니다.
모니터링에서 "FAILED 분기를 탔는데 왜 Job이 성공이죠?"라는 질문 — 이제 답할 수 있습니다.

### 3-3. JobExecutionDecider — 판단 전문 컴포넌트

ExitStatus 조작 방식의 찜찜함: 분기 판단을 위해 "처리 없는 가짜 Step"이 끼어들고,
Step의 종료 코드를 본래 용도와 다르게 씁니다. 판단이 복잡해지면 **Decider**가 정석:

```java
@Bean
public JobExecutionDecider oddEvenDecider() {
    return (jobExecution, stepExecution) -> {
        Long number = jobExecution.getJobParameters().getLong("number");
        return (number % 2 == 0) ? new FlowExecutionStatus("EVEN")
                                 : new FlowExecutionStatus("ODD");
    };
}
```

| | ExitStatus 조작 (Step) | Decider |
|---|---|---|
| 정체 | 처리 + 신호 겸업 | **판단만** |
| 장부 기록 | STEP_EXECUTION에 남음 | **안 남음** (Step이 아니니까) |
| 반환 | ExitStatus | FlowExecutionStatus (임의 코드 자유) |
| 언제 | 처리 결과가 곧 분기 기준일 때 | 분기 로직이 독립적/복잡할 때 |

example B에서 실행 Step 목록에 decider가 **보이지 않는 것** 자체가 검증 포인트입니다.

### 3-4. 경로 검증의 표준 패턴

"어느 길을 탔는가"의 증명 = **실행된 Step 이름의 순서**:

```java
assertThat(jobExecution.getStepExecutions())
        .extracting("stepName")
        .containsExactly("checkStep", "recoveryStep");  // 순서까지!
```

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step04.*"
```

1. `ConditionalFlowJobConfig` — on/to/from/end 문법과 checkStep의 setExitStatus
2. example A — 두 경로, 그리고 **반전(FAILED인데 COMPLETED)**
3. example B — Decider가 장부에 안 남는 것 확인
4. **일부러 깨뜨려보기**: conditionalFlowJob에서 `.on("*")` 갈래를 지우면?
   (mode=ok 실행이 어떻게 끝나는지 — 갈 곳 없는 ExitStatus의 운명을 관찰 후 원복)

## 5. Testing — exercise 풀기

`step04/exercise/ConditionalFlowExerciseTest.java`의 TODO 1~4를 채우세요.
같은 장부 행에서 STATUS=COMPLETED와 EXIT_CODE=FAILED를 **동시에** 확인하는 순간이
이 Step의 졸업 시험입니다. (TODO 3에는 Step 3의 복병이 숨어 있습니다)

## 6. Lessons Learned

### 사례 1: "FAILED 분기를 탔는데 모니터링엔 성공이라고 떠요"

- **증상**: 복구 경로가 실행됐는데 Job은 COMPLETED — 운영자가 장애를 놓침
- **원인**: ExitStatus 분기는 "정의된 흐름" — 프레임워크 입장에선 계획대로 된 것
- **해결**: 복구 경로를 탔다는 사실이 "장애"라면 recoveryStep에서 알림을 쏘거나(Step 11의
  Listener), `.on("FAILED").fail()` 로 Job 자체를 실패 처리하는 선택지를 설계하라
- **교훈**: 분기 설계 시 "이 경로는 모니터링에 어떻게 보여야 하는가"까지가 설계다.

### 사례 2: BatchStatus로 분기하려다 한참 헤맴

- **증상**: `.on("COMPLETED")`이 안 먹는 것 같다? — 사실 ExitStatus 문자열 미스매치
- **교훈**: `.on()`의 매칭 대상은 **항상 ExitStatus 문자열**. 커스텀 코드("EVEN")도
  그래서 가능한 것이다.

### 시니어의 시선

> Flow가 화려해질수록 Job은 읽기 어려워집니다. 제 기준: **분기가 2단을 넘으면
> Job을 쪼개라.** "하나의 Job 안의 복잡한 Flow"보다 "단순한 Job 여러 개 + 스케줄러
> 오케스트레이션"이 운영하기 쉽습니다. Flow는 무기지만, 휘두를수록 무거워지는 무기입니다.

## 7. Key Takeaways

- 분기 문법: `.start().on().to() / .from().on().to() / .end()` — 매칭 대상은 ExitStatus
- setExitStatus(FAILED)는 신호일 뿐 — Step도 Job도 정상 종료할 수 있다 (STATUS≠EXIT_CODE)
- Decider = 처리 없는 판단 전문 — 장부에 남지 않는다
- 경로 검증 = Step 이름 목록 containsExactly
- 복구 경로의 모니터링 노출까지가 분기 설계다

## 8. Next Steps — 다음 Step의 문제

분기까지 익혔는데, 새로운 벽이 있습니다.

> "1번 Step이 **집계한 건수**를 2번 Step이 받아서 검증 기준으로 써야 해요"

Step끼리는 어떻게 데이터를 주고받을까요? 멤버 변수? (멀티스레드에서 죽음)
static? (더 죽음) — Spring Batch의 답은 **ExecutionContext**, 그리고 Step의 것을
Job 레벨로 끌어올리는 **PromotionListener**입니다. **Step 5**에서 다룹니다.
