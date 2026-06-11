# [Batch Step 3] JobParameters & JobInstance — 재실행 거부 사건

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `JobParametersBuilder`, `@JobScope` + `@Value("#{jobParameters[...]}")`(Late Binding), `JobInstanceAlreadyCompleteException`, `getUniqueJobParameters()`
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/parameters, test/java/com/batchflow/step03}/`

---

## 1. Before We Start — "왜 두 번째 실행이 거부되죠?!"

운영 배치의 실행 명령은 거의 항상 이런 모습입니다.

> "dailyGreetingJob을 **2026-06-11 기준으로** 실행해줘"

이 "기준값"이 **JobParameters**입니다. 그런데 같은 명령을 한 번 더 내리면:

```
JobInstanceAlreadyCompleteException:
A job instance already exists and is complete for parameters={targetDate=2026-06-11}
```

처음 보면 버그 같지만, **운영자가 가장 고마워해야 할 설계**입니다.
"6월 11일 정산"이 실수로 두 번 돌아 고객 잔액이 두 번 차감되는 사고 —
Spring Batch는 이것을 프레임워크 차원에서 원천 봉쇄합니다.

## 2. What We're Building

```
src/main/java/com/batchflow/job/parameters/DailyGreetingJobConfig.java
  └─ dailyGreetingJob: targetDate 파라미터를 @JobScope로 주입받는 Step 1개

src/test/java/com/batchflow/step03/
├── example/DailyGreetingJobTest.java        ← Late Binding / 재실행 거부 / 인스턴스 분리
├── exercise/JobParametersExerciseTest.java  ← 파라미터 장부 추적
└── answer/JobParametersAnswerTest.java
```

## 3. Core Concepts

### 3-1. JobInstance = Job 이름 + 파라미터 — Step 1 복선의 회수

```
BATCH_JOB_INSTANCE.JOB_KEY = hash(JobParameters)   ← Step 1에서 본 그 컬럼!

dailyGreetingJob + {targetDate=06-10}  → JobInstance A (6/10편)
dailyGreetingJob + {targetDate=06-11}  → JobInstance B (6/11편)
JobInstance B 성공 후 같은 파라미터    → 거부! (그 편은 이미 운항 완료)
```

| 개념 | 의미 | 비유 |
|------|------|------|
| Job | 정의 | 노선 (서울→제주) |
| JobInstance | Job + 파라미터의 논리 단위 | 특정 날짜의 항공편 |
| JobExecution | 인스턴스의 실행 시도 (1:N) | 그 편의 운항 시도 (결항 후 재운항) |

"실패한 인스턴스"는 같은 파라미터로 재실행할 수 **있습니다** (그게 재시작, Step 12).
거부되는 것은 **성공(COMPLETED)한** 인스턴스뿐입니다.

### 3-2. @JobScope와 Late Binding — 파라미터는 실행 시점에야 존재한다

```java
@Bean
@JobScope   // 이 Bean은 "Job이 실행되는 순간" 생성된다
public Step dailyGreetingStep(@Value("#{jobParameters['targetDate']}") String targetDate) { ... }

// Job 정의에서는 null을 넘긴다 — 실제 값은 실행 시점에 프록시가 채운다
.start(dailyGreetingStep(null))
```

왜 이런 곡예가 필요할까요? **컨텍스트가 뜨는 시점에는 파라미터가 없기** 때문입니다.
파라미터는 "실행할 때" 들어오므로, Bean 생성을 실행 시점으로 미루는 것(@JobScope)이
Late Binding입니다. @JobScope 없이 jobParameters를 참조하면 기동 자체가 실패합니다 —
exercise 후 직접 지워보세요 (Lessons Learned 예고).

### 3-3. 테스트에서 파라미터 다루기

```java
JobParameters params = new JobParametersBuilder()
        .addString("targetDate", "2026-06-11")
        .toJobParameters();
jobLauncherTestUtils.launchJob(params);                       // 운영처럼 명시적으로

jobLauncherTestUtils.launchJob(getUniqueJobParameters());     // 매번 새 인스턴스 (Step 2의 비밀)
```

Step 2에서 `launchJob()`이 아무 일 없이 반복 실행됐던 이유 — 내부에서
`getUniqueJobParameters()`를 써서 **매번 다른 JobInstance**를 만들고 있었기 때문입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step03.*"
```

1. `DailyGreetingJobConfig` — @JobScope와 null 전달의 의미부터
2. example — Late Binding 증명 → **재실행 거부 사건** → 인스턴스 분리
3. **일부러 깨뜨려보기**: Step 메서드에서 `@JobScope`를 지우고 실행 —
   어떤 에러가, 언제(기동 시? 실행 시?) 나는지 관찰 후 원복

## 5. Testing — exercise 풀기

`step03/exercise/JobParametersExerciseTest.java`의 TODO 1~5를 채우세요.
파라미터가 장부(BATCH_JOB_EXECUTION_PARAMS)에 남는 것을 SQL로 확인하는 게 포인트 —
"이 배치 언제 어떤 기준으로 돌았어요?"라는 운영 질문의 답이 거기 있습니다.

## 6. Lessons Learned

### 사례 1: 운영에서 같은 날짜 배치를 재실행해야 하는데 거부된다

- **증상**: 데이터 보정 후 "오늘자 배치 다시 돌려주세요" — JobInstanceAlreadyComplete
- **원인**: 성공한 인스턴스의 재실행 거부 (정상 설계)
- **실무 해법**: 재실행용 구분 파라미터(예: `rerun=2차`)를 추가해 **새 인스턴스**로 실행.
  메타테이블을 지우는 것은 장부 소각 — 최후의 수단으로도 쓰지 마라
- **교훈**: 이 규칙과 싸우지 말고 파라미터 설계로 협력하라.

### 사례 2: @JobScope를 빼먹은 Late Binding

- **증상**: `EL1008E: ... jobParameters cannot be found` 또는 기동 실패
- **원인**: 싱글톤 Bean은 기동 시점에 만들어지는데 그때는 파라미터가 없다
- **해결**: 파라미터를 주입받는 Step/Tasklet Bean에는 @JobScope(@StepScope)
- **교훈**: "값이 언제 존재하는가"를 생각하면 스코프가 보인다.

### 사례 3: 테스트가 어제는 통과, 오늘은 실패

- **증상**: `addString("date", LocalDate.now()...)`를 쓴 테스트가 날짜가 바뀌자 거동 변화
- **교훈**: 테스트의 파라미터는 **고정값**으로 — 현재 시각 의존은 F.I.R.S.T의 R 위반.

### 시니어의 시선

> JobParameters 설계가 곧 배치의 운영성입니다. "무엇으로 실행 단위를 식별할 것인가
> (기준일? 지점코드? 회차?)"를 처음에 잘못 잡으면, 재실행 정책도 중복 방지도
> 전부 꼬입니다. 새 배치를 설계할 때 첫 질문은 항상 이것입니다 —
> **"이 Job의 JobInstance를 무엇으로 정의할 것인가?"**

## 7. Key Takeaways

- JobInstance = Job 이름 + 파라미터(JOB_KEY 해시) / JobExecution = 그 시도들(1:N)
- 성공한 인스턴스의 같은 파라미터 재실행은 거부된다 — 중복 처리 방지 장치
- 실패한 인스턴스는 재실행 가능 — 그것이 재시작(Step 12)
- 파라미터 주입은 @JobScope + SpEL — Late Binding (값은 실행 시점에 존재)
- 파라미터는 장부에 남는다 — 운영 추적의 근거

## 8. Next Steps — 다음 Step의 문제

지금까지의 Job은 Step이 **한 줄로만** 흘렀습니다. 그런데 실무 요구는 이렇습니다.

> "검증 Step이 **실패하면** 복구 Step으로, **성공하면** 본처리 Step으로 보내줘"

성공/실패에 따라 **흐름을 갈라야** 합니다. Step 2에서 스쳐 지난 ExitStatus가
드디어 주인공이 됩니다 — `.on("FAILED").to(...)` 와 JobExecutionDecider, **Step 4**입니다.
