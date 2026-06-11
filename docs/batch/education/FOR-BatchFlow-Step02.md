# [Batch Step 2] Hello Job — Job/Step/Tasklet 해부

> **소요 시간**: 약 1시간
> **이번 Step의 도구**: `JobBuilderFactory`/`StepBuilderFactory`(4.x), Tasklet, `RepeatStatus`, `@SpringBatchTest`, `JobLauncherTestUtils`, `removeJobExecutions()`
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/hello, test/java/com/batchflow/step02}/`

---

## 1. Before We Start — 판은 깔렸는데 Job이 없다

Step 1에서 장부 시스템을 확인했습니다. 이제 거기에 기록될 **첫 실행**을 만들 차례입니다.
가장 작은 배치는 세 층으로 이루어집니다.

```
Job      "오늘 밤의 실행 단위"        ─ 예: 일일 정산
 └─ Step   "그 안의 작업 단계"        ─ 예: 1)집계 2)저장 3)알림
     └─ Tasklet  "실제 코드 한 덩이"   ─ 예: 로그 한 줄, 파일 삭제, 단순 UPDATE
```

회사 업무에 비유하면 Job은 "프로젝트", Step은 "업무 단계", Tasklet은 "실제 손을 움직이는 일"입니다.
Tasklet은 단순 작업용이고, 대량 데이터 처리는 Chunk(Step 6)가 맡습니다 — 지금은 구조에 집중하세요.

## 2. What We're Building

```
src/main/java/com/batchflow/job/hello/HelloJobConfig.java   ← helloJob = helloStep 1개
src/test/java/com/batchflow/step02/
├── example/HelloJobTest.java          ← 실행 + Step 단독 실행 + 장부 기록 확인
├── exercise/HelloJobExerciseTest.java
└── answer/HelloJobAnswerTest.java
```

## 3. Core Concepts

### 3-1. Spring Batch 4.x 조립 방법 (이 모듈의 고정 문법!)

```java
@Bean
public Job helloJob() {
    return jobBuilderFactory.get("helloJob")   // Job 이름 = 장부의 JOB_NAME
            .start(helloStep())
            .build();
}

@Bean
public Step helloStep() {
    return stepBuilderFactory.get("helloStep")
            .tasklet((contribution, chunkContext) -> {
                log.info(">>>>> Hello, Spring Batch!");
                return RepeatStatus.FINISHED;   // "끝났다" — CONTINUABLE이면 또 호출된다
            })
            .build();
}
```

⚠️ 인터넷의 최신 예제는 대부분 **Batch 5.x** (`new JobBuilder("job", jobRepository)`) —
이 모듈(Boot 2.7 = Batch 4.3)에서는 컴파일조차 안 됩니다. Factory 주입 방식만 사용하세요.
(TestCraft의 WebSecurityConfigurerAdapter 사건과 같은 종류의 "버전 함정"입니다)

### 3-2. 배치 테스트의 표준 골격 — 전 Step에서 반복된다

```java
@SpringBatchTest                                              // 테스트 유틸 2종 자동 주입
@SpringBootTest(classes = {HelloJobConfig.class, TestBatchConfig.class})  // 필요한 것만!
class HelloJobTest {
    @Autowired JobLauncherTestUtils jobLauncherTestUtils;     // Job/Step 실행기
    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils; // 장부 청소기

    @BeforeEach
    void setUp() { jobRepositoryTestUtils.removeJobExecutions(); }  // 격리 — 필수!
}
```

- `classes`에 필요한 JobConfig만 명시 → 전체 컨텍스트를 띄우지 않아 빠르다 (TestCraft의 슬라이스 철학)
- `removeJobExecutions()` = 배치판 "롤백". 메타데이터는 트랜잭션 롤백으로 지워지지 않으므로
  직접 비운다 — 이게 없으면 카운트 검증이 흔들리고 Step 3의 재실행 거부에 걸린다
- `launchJob()`은 호출마다 **unique 파라미터**를 만들어준다 (왜 필요한지는 Step 3에서!)

### 3-3. 실행 한 번 = 장부 한 세트

example의 세 번째 테스트가 Step 1과 이번 Step을 연결합니다:

```
launchJob() 1회
  → BATCH_JOB_INSTANCE   1건 (helloJob + 파라미터)
  → BATCH_JOB_EXECUTION  1건 (이번 시도, COMPLETED)
  → BATCH_STEP_EXECUTION 1건 (helloStep, COMPLETED)
```

### 3-4. BatchStatus vs ExitStatus — 비슷해 보이는 두 상태

| | BatchStatus | ExitStatus |
|---|---|---|
| 누가 보나 | 프레임워크 (실행이 어떻게 끝났나) | 흐름 제어 (다음에 뭘 할까) |
| 값 | COMPLETED/FAILED/STOPPED... (enum) | 임의 문자열 코드 가능 |
| 쓰임 | 모니터링, 재시작 판단 | Step 4의 분기(.on("FAILED"))에 사용 |

지금은 "둘 다 COMPLETED"지만, Step 4에서 ExitStatus를 **조작해 흐름을 바꾸는** 순간
차이가 선명해집니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step02.*"
```

1. `HelloJobConfig` — 4.x 조립 문법과 Tasklet 람다
2. `HelloJobTest` — 실행 → Step 단독 실행 → 장부 확인 순으로
3. **일부러 깨뜨려보기**: Tasklet의 `RepeatStatus.FINISHED`를 `CONTINUABLE`로 바꾸고 실행 —
   로그가 어떻게 되나요? (무한 반복! 확인 후 즉시 원복 — Tasklet 반환값의 의미를 몸으로)

## 5. Testing — exercise 풀기

`step02/exercise/HelloJobExerciseTest.java`의 TODO 1~5를 채우세요.
같은 사실(Step 1개 실행)을 **JobExecution 객체**와 **장부(SQL)** 양쪽에서 검증해보는 것이 핵심입니다.

## 6. Lessons Learned

### 사례 1: removeJobExecutions를 빼먹은 테스트

- **증상**: 단독 실행은 통과, 전체 실행하면 카운트 검증(1건 기대)이 2~3건으로 실패
- **원인**: 같은 인메모리 DB를 쓰는 다른 테스트 클래스의 실행 기록이 남아있음
- **해결**: `@BeforeEach`에서 `removeJobExecutions()` — 이 모듈의 철칙
- **교훈**: 배치 테스트의 격리 장치는 트랜잭션 롤백이 아니라 **장부 청소**다.

### 사례 2: Batch 5.x 예제를 복사했더니 컴파일 에러

- **증상**: `JobBuilder`, `new StepBuilder(..., jobRepository)`가 없는 클래스라고 나옴
- **원인**: 검색 상위 결과가 Boot 3.x/Batch 5.x 기준
- **교훈**: Spring 생태계 검색은 **버전 확인이 먼저** — 이 모듈은 4.3.x Factory 방식 고정.

### 시니어의 시선

> "Hello 찍는 Job에 테스트가 왜 필요해요?"라는 질문을 받곤 합니다.
> 이 Step의 테스트는 Hello를 검증하는 게 아니라 **"Job 실행→장부 기록"이라는
> 배치의 핵심 계약**을 검증합니다. 이후 모든 Step의 테스트가 이 골격 위에 서고,
> 운영 장애 분석도 결국 이 장부를 읽는 일입니다.

## 7. Key Takeaways

- 구조: Job(실행 단위) → Step(작업 단계) → Tasklet(코드 한 덩이, FINISHED 반환)
- 4.x Factory 문법 고정 — 5.x 예제는 컴파일 불가 (버전 함정)
- 배치 테스트 골격: @SpringBatchTest + classes 명시 + removeJobExecutions
- 실행 1회 = INSTANCE/EXECUTION/STEP_EXECUTION 한 세트
- launchStep("이름")으로 Step 단독 실행 — 디버깅 무기

## 8. Next Steps — 다음 Step의 문제

테스트의 `launchJob()`은 매번 **unique 파라미터**를 몰래 만들어 줬습니다. 왜일까요?

운영처럼 "2026-06-11 기준으로 정산해"라고 **같은 파라미터**를 주고 두 번 실행하면...
**두 번째는 거부당합니다** (`JobInstanceAlreadyCompleteException`).
버그가 아니라 Spring Batch의 핵심 설계입니다 — 그 이유(중복 정산 방지!)와
파라미터를 다루는 법(@JobScope, Late Binding)을 **Step 3**에서 정면으로 다룹니다.
