# Spring Batch Core Skills
## Spring Batch 핵심 개념 및 기본 패턴

---

## 🎯 이 스킬은 언제 사용하나요?

- Step 1-8 (Phase 1: 기초) 진행 시
- Job, Step, Tasklet의 기본 구조 이해가 필요할 때
- Flow, Decider, ExecutionContext 구현 시
- Spring Batch의 기본 아키텍처 이해가 필요할 때

---

## 📚 핵심 개념

### 1. Spring Batch 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Batch Architecture                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────┐                                                 │
│  │ JobLauncher│──────────────────────────────┐                  │
│  └────────────┘                              │                  │
│        │                                     ▼                  │
│        │ launch(Job, JobParameters)  ┌──────────────┐          │
│        │                             │ JobRepository │          │
│        ▼                             │  (메타데이터)  │          │
│  ┌──────────┐                        └──────────────┘          │
│  │   Job    │                               ▲                   │
│  └──────────┘                               │                   │
│        │                                    │ 상태 저장/조회     │
│        │ contains                           │                   │
│        ▼                                    │                   │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                  │
│  │  Step 1  │───▶│  Step 2  │───▶│  Step 3  │                  │
│  └──────────┘    └──────────┘    └──────────┘                  │
│        │                                                        │
│        │ Tasklet or Chunk                                       │
│        ▼                                                        │
│  ┌─────────────────────────────────────┐                       │
│  │  Tasklet          │    Chunk-based   │                       │
│  │  (단순 작업)       │  Reader/Processor│                       │
│  │                   │  /Writer         │                       │
│  └─────────────────────────────────────┘                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2. 핵심 도메인 객체

| 객체 | 설명 | 비유 |
|------|------|------|
| **Job** | 배치 작업의 최상위 단위 | 요리 레시피 |
| **JobInstance** | Job + JobParameters의 조합 | 특정 날짜의 요리 |
| **JobExecution** | JobInstance의 실행 시도 | 요리 시도 (성공/실패 가능) |
| **Step** | Job 내의 독립적인 작업 단위 | 레시피의 각 단계 |
| **StepExecution** | Step의 실행 시도 | 각 단계의 시도 |
| **ExecutionContext** | 실행 중 데이터 공유 공간 | 메모장 |

### 3. JobInstance vs JobExecution

```
JobParameters: { requestDate: "2025-01-15" }
        │
        ▼
  ┌──────────────────────────┐
  │     JobInstance #1       │  ← Job + Parameters로 유일하게 식별
  │  (dormantJob, 2025-01-15)│
  └──────────────────────────┘
        │
        │ 실행 시도들
        ▼
  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
  │ Execution 1 │  │ Execution 2 │  │ Execution 3 │
  │   FAILED    │  │   FAILED    │  │  COMPLETED  │
  │  (10:00)    │  │  (11:00)    │  │   (12:00)   │
  └─────────────┘  └─────────────┘  └─────────────┘

💡 동일한 JobParameters로 재실행하면 같은 JobInstance에 새 Execution이 생성됨
💡 COMPLETED된 JobInstance는 다시 실행할 수 없음 (이미 완료)
```

---

## 🔧 코드 패턴

### 패턴 1: 기본 Job 구조 (Tasklet)

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HelloJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job helloJob() {
        return jobBuilderFactory.get("helloJob")
                .start(helloStep())
                .build();
    }

    @Bean
    public Step helloStep() {
        return stepBuilderFactory.get("helloStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Hello, Spring Batch!");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

### 패턴 2: JobParameters 활용

```java
@Bean
@JobScope  // JobParameters 사용 시 필수
public Step parameterStep(
        @Value("#{jobParameters['requestDate']}") String requestDate,
        @Value("#{jobParameters['version']}") Long version) {
    
    return stepBuilderFactory.get("parameterStep")
            .tasklet((contribution, chunkContext) -> {
                log.info(">>>>> requestDate: {}", requestDate);
                log.info(">>>>> version: {}", version);
                return RepeatStatus.FINISHED;
            })
            .build();
}

// 실행 시
JobParameters params = new JobParametersBuilder()
        .addString("requestDate", "2025-01-15")
        .addLong("version", 1L)
        .toJobParameters();
```

### 패턴 3: Flow 조건 분기

```java
@Bean
public Job flowJob() {
    return jobBuilderFactory.get("flowJob")
            .start(checkStep())
                .on("COMPLETED").to(successStep())  // COMPLETED면 successStep
            .from(checkStep())
                .on("FAILED").to(failStep())        // FAILED면 failStep
            .from(checkStep())
                .on("*").to(defaultStep())          // 그 외 defaultStep
            .end()
            .build();
}

@Bean
public Step checkStep() {
    return stepBuilderFactory.get("checkStep")
            .tasklet((contribution, chunkContext) -> {
                // 조건에 따라 ExitStatus 설정
                if (someCondition) {
                    contribution.setExitStatus(ExitStatus.COMPLETED);
                } else {
                    contribution.setExitStatus(ExitStatus.FAILED);
                }
                return RepeatStatus.FINISHED;
            })
            .build();
}
```

### 패턴 4: JobExecutionDecider

```java
@Bean
public Job deciderJob() {
    return jobBuilderFactory.get("deciderJob")
            .start(startStep())
            .next(decider())  // Decider로 분기
                .on("ODD").to(oddStep())
            .from(decider())
                .on("EVEN").to(evenStep())
            .end()
            .build();
}

@Bean
public JobExecutionDecider decider() {
    return (jobExecution, stepExecution) -> {
        int number = (int) (Math.random() * 100);
        if (number % 2 == 0) {
            return new FlowExecutionStatus("EVEN");
        } else {
            return new FlowExecutionStatus("ODD");
        }
    };
}
```

### 패턴 5: ExecutionContext 데이터 공유

```java
// Step 1: 데이터 저장
@Bean
public Step saveDataStep() {
    return stepBuilderFactory.get("saveDataStep")
            .tasklet((contribution, chunkContext) -> {
                // Step ExecutionContext (현재 Step에서만)
                ExecutionContext stepContext = chunkContext.getStepContext()
                        .getStepExecution().getExecutionContext();
                stepContext.putString("stepData", "stepValue");
                
                // Job ExecutionContext (모든 Step에서 공유)
                ExecutionContext jobContext = chunkContext.getStepContext()
                        .getStepExecution().getJobExecution().getExecutionContext();
                jobContext.putString("sharedData", "sharedValue");
                
                return RepeatStatus.FINISHED;
            })
            .build();
}

// Step 2: 데이터 조회
@Bean
public Step readDataStep() {
    return stepBuilderFactory.get("readDataStep")
            .tasklet((contribution, chunkContext) -> {
                ExecutionContext jobContext = chunkContext.getStepContext()
                        .getStepExecution().getJobExecution().getExecutionContext();
                
                String sharedData = jobContext.getString("sharedData");
                log.info(">>>>> Shared Data: {}", sharedData);
                
                return RepeatStatus.FINISHED;
            })
            .build();
}
```

### 패턴 6: 다중 Step Job

```java
@Bean
public Job multiStepJob() {
    return jobBuilderFactory.get("multiStepJob")
            .start(step1())
            .next(step2())
            .next(step3())
            .build();
}

// 또는 Flow 사용
@Bean
public Job flowBasedJob() {
    Flow flow1 = new FlowBuilder<SimpleFlow>("flow1")
            .start(step1())
            .next(step2())
            .build();
    
    Flow flow2 = new FlowBuilder<SimpleFlow>("flow2")
            .start(step3())
            .build();
    
    return jobBuilderFactory.get("flowBasedJob")
            .start(flow1)
            .next(flow2)
            .end()
            .build();
}
```

---

## ⚠️ 주의사항

### 1. @JobScope / @StepScope 이해

```java
// ❌ 잘못된 사용: JobParameters를 Scope 없이 사용
@Bean
public Step wrongStep(@Value("#{jobParameters['date']}") String date) {
    // NullPointerException 발생!
}

// ✅ 올바른 사용: @JobScope 또는 @StepScope 필수
@Bean
@JobScope
public Step correctStep(@Value("#{jobParameters['date']}") String date) {
    // 정상 동작
}
```

**이유**: JobParameters는 런타임에 결정되므로, Late Binding을 위해 Scope가 필요합니다.

### 2. ExitStatus vs BatchStatus

| 구분 | BatchStatus | ExitStatus |
|------|-------------|------------|
| 용도 | Step/Job의 상태 표현 | Flow 분기 조건 |
| 값 | COMPLETED, FAILED, STOPPED 등 (고정) | 커스텀 가능 |
| 설정 | 프레임워크가 자동 설정 | 개발자가 직접 설정 가능 |

```java
// ExitStatus 커스텀
contribution.setExitStatus(new ExitStatus("CUSTOM_STATUS"));

// Flow에서 사용
.on("CUSTOM_STATUS").to(customStep())
```

### 3. 동일 JobParameters 재실행 불가

```java
// ❌ 이미 COMPLETED된 Job은 같은 Parameters로 재실행 불가
JobParameters params = new JobParametersBuilder()
        .addString("date", "2025-01-15")
        .toJobParameters();
// 첫 번째 실행: COMPLETED
// 두 번째 실행: JobInstanceAlreadyCompleteException 발생!

// ✅ 해결책: 고유한 파라미터 추가
JobParameters params = new JobParametersBuilder()
        .addString("date", "2025-01-15")
        .addLong("timestamp", System.currentTimeMillis())  // 매번 다른 값
        .toJobParameters();
```

---

## 📋 체크리스트

### Job Config 클래스 작성 시

- [ ] `@Configuration`, `@Slf4j`, `@RequiredArgsConstructor` 적용
- [ ] `JobBuilderFactory`, `StepBuilderFactory` 주입
- [ ] Job/Step 이름을 명확하게 지정
- [ ] CHUNK_SIZE 상수 정의 (Chunk 기반 Step의 경우)

### JobParameters 사용 시

- [ ] `@JobScope` 또는 `@StepScope` 적용
- [ ] `@Value("#{jobParameters['paramName']}")` 문법 확인
- [ ] 필수 파라미터 누락 처리 로직

### Flow 구성 시

- [ ] 모든 분기 조건 커버 (`*` 와일드카드 활용)
- [ ] `end()` 호출 확인
- [ ] ExitStatus 값 오타 확인

### 테스트 시

- [ ] `@SpringBatchTest` + `@SpringBootTest` 적용
- [ ] `jobRepositoryTestUtils.removeJobExecutions()` 호출
- [ ] 고유한 JobParameters 사용

---

## 🔗 관련 스킬

- **Chunk 처리**: `@skills/spring-batch-chunk.md`
- **테스트 패턴**: `@skills/spring-batch-testing.md`
- **에러 처리**: `@skills/spring-batch-error.md`
