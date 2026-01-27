# FOR-BatchFlow-Step02: 첫 번째 Job 생성 - Hello Batch

> Job, Step, Tasklet의 기본 구조를 이해하고, 가장 간단한 배치 작업을 구현합니다.

---

## 🎬 Before We Start

"자, 이제 공장 설비(Step 1)는 다 갖췄으니, 실제로 요리를 만들어 볼까요?"

Step 1에서 Spring Batch의 인프라를 구축했다면, 이제는 **실제로 동작하는 배치 작업**을 만들어야 합니다.

**비유로 이해하기:**
- **Job** = 하나의 요리 레시피 (예: 김치찌개 만들기)
- **Step** = 레시피의 각 단계 (1. 재료 손질, 2. 볶기, 3. 끓이기)
- **Tasklet** = 각 단계에서 수행하는 구체적인 작업 (파를 송송 썰기)

Spring Batch의 기본 단위는 이 3개입니다. 이번 Step에서는 가장 간단한 형태의 Job을 만들어서, **"Hello, Spring Batch!"**를 출력하는 작업을 해볼 거예요.

"뭐야, 그냥 로그 찍는 거잖아?" 라고 생각할 수 있지만, **이게 배치 작업의 기본 뼈대**입니다. 이 구조를 이해하면, 나중에 1억 건의 데이터를 처리하는 복잡한 Job도 만들 수 있어요.

---

## 🏗️ What We're Building

### Step 2에서 구현할 것

```
┌───────────────────────────────────────────────────────────────┐
│                  HelloJob 구조                                 │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  HelloJobConfig.java                                           │
│  ├─ Job: helloJob                                              │
│  │   └─ Step: helloStep                                        │
│  │       └─ Tasklet: 로그 출력 작업                            │
│  │                                                             │
│  └─ 실행 결과:                                                  │
│      >>>>> Hello, Spring Batch!                                │
│      >>>>> Step 실행 완료                                       │
│                                                                │
└───────────────────────────────────────────────────────────────┘

흐름:
1. JobBuilderFactory로 Job 생성 (helloJob)
2. StepBuilderFactory로 Step 생성 (helloStep)
3. Tasklet에서 로그 출력
4. RepeatStatus.FINISHED 반환 → Step 종료
5. Job 완료
```

---

## 🧠 Core Concepts

### 1. Job, Step, Tasklet의 관계

```
┌─────────────────────────────────────────────────────────┐
│                      Job (요리 레시피)                   │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Step 1 (재료 손질)                               │  │
│  │  ├─ Tasklet: 양파 썰기                            │  │
│  │  └─ Tasklet: 고기 양념하기                        │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Step 2 (볶기)                                     │  │
│  │  └─ Tasklet: 재료 볶기                            │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Step 3 (끓이기)                                   │  │
│  │  └─ Tasklet: 찌개 끓이기                          │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

**핵심 포인트:**
- **Job**: 배치 작업의 최상위 단위. 하나의 완전한 작업 흐름
- **Step**: Job을 구성하는 독립적인 단계. 각 Step은 독립적으로 실행 가능
- **Tasklet**: Step 내에서 수행되는 단순 작업. `execute()` 메서드 한 번 실행

### 2. JobBuilderFactory와 StepBuilderFactory

```java
@Configuration
@RequiredArgsConstructor
public class HelloJobConfig {

    // Spring Batch가 자동으로 주입해주는 Factory들
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job helloJob() {
        return jobBuilderFactory
                .get("helloJob")      // Job 이름 지정 (필수!)
                .start(helloStep())   // 첫 번째 Step 지정
                .build();
    }

    @Bean
    public Step helloStep() {
        return stepBuilderFactory
                .get("helloStep")     // Step 이름 지정 (필수!)
                .tasklet((contribution, chunkContext) -> {
                    // 이 부분이 실제로 실행되는 코드
                    log.info(">>>>> Hello, Spring Batch!");
                    return RepeatStatus.FINISHED;  // Step 종료
                })
                .build();
    }
}
```

**비유로 이해하기:**
- `jobBuilderFactory.get("helloJob")` = "김치찌개 레시피"라는 이름의 빈 레시피 양식을 가져옴
- `.start(helloStep())` = 레시피의 첫 번째 단계를 지정 ("재료 손질부터 시작")
- `stepBuilderFactory.get("helloStep")` = "재료 손질"이라는 이름의 작업 단계 양식을 가져옴
- `.tasklet(...)` = 이 단계에서 수행할 구체적인 작업 내용을 작성

### 3. RepeatStatus의 의미

```java
return RepeatStatus.FINISHED;  // "이 작업 끝났어요, 다음으로 넘어가세요"
```

**RepeatStatus 종류:**
- `FINISHED`: Tasklet 종료. Step 완료
- `CONTINUABLE`: Tasklet 계속 반복 (같은 Tasklet을 다시 실행)

**실제 예시:**

```java
// FINISHED 예시 (일반적인 경우)
.tasklet((contribution, chunkContext) -> {
    log.info(">>>>> 데이터 처리 완료");
    return RepeatStatus.FINISHED;  // 한 번만 실행하고 종료
})

// CONTINUABLE 예시 (특수한 경우)
private int count = 0;

.tasklet((contribution, chunkContext) -> {
    count++;
    log.info(">>>>> 반복 실행 중: {}", count);

    if (count < 5) {
        return RepeatStatus.CONTINUABLE;  // 계속 반복
    } else {
        return RepeatStatus.FINISHED;     // 5번 반복 후 종료
    }
})
```

> **주의:** 대부분의 경우 `RepeatStatus.FINISHED`를 사용합니다. `CONTINUABLE`은 특수한 경우에만 사용하세요.

---

## 💻 Step-by-Step Implementation

### Step 1: HelloJobConfig 클래스 생성

**파일 위치:** `src/main/java/com/batchflow/job/hello/HelloJobConfig.java`

```java
package com.batchflow.job.hello;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j                       // Lombok: log 객체 자동 생성
@Configuration               // Spring: 이 클래스는 설정 클래스입니다
@RequiredArgsConstructor     // Lombok: final 필드의 생성자 자동 생성
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
                    log.info(">>>>> Step 실행 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

**코드 설명:**
1. `@Slf4j`: Lombok이 자동으로 `log` 객체를 생성 (로그 출력용)
2. `@Configuration`: Spring에게 "이 클래스는 Bean 설정 클래스입니다"라고 알림
3. `@RequiredArgsConstructor`: final 필드의 생성자를 자동 생성 (DI 주입)
4. `jobBuilderFactory.get("helloJob")`: "helloJob"이라는 이름의 Job 생성 시작
5. `.start(helloStep())`: helloStep을 첫 번째 Step으로 지정
6. `stepBuilderFactory.get("helloStep")`: "helloStep"이라는 이름의 Step 생성 시작
7. `.tasklet(...)`: Tasklet 로직 구현 (람다 표현식 사용)

### Step 2: 테스트 설정 클래스 생성

**파일 위치:** `src/test/java/com/batchflow/config/TestBatchConfig.java`

```java
package com.batchflow.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing    // 테스트 환경에서도 Batch 기능 활성화
@EnableAutoConfiguration  // 테스트 환경 자동 구성
public class TestBatchConfig {
    // 별도의 Bean 정의 없이 어노테이션만으로 충분
}
```

### Step 3: 테스트 코드 작성

**파일 위치:** `src/test/java/com/batchflow/job/hello/HelloJobConfigTest.java`

```java
package com.batchflow.job.hello;

import com.batchflow.config.TestBatchConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest  // Spring Batch 테스트 지원
@SpringBootTest(classes = {HelloJobConfig.class, TestBatchConfig.class})
class HelloJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 이전 Job 실행 기록 삭제
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @Test
    void helloJob_실행_성공() throws Exception {
        // when: Job 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then: 실행 결과 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
    }

    @Test
    void helloStep_단독실행_성공() throws Exception {
        // when: 특정 Step만 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("helloStep");

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
```

**테스트 코드 설명:**
- `@SpringBatchTest`: Spring Batch 테스트 유틸리티 자동 주입
- `JobLauncherTestUtils`: Job을 쉽게 실행할 수 있는 테스트 도구
- `JobRepositoryTestUtils`: Job 실행 기록을 관리하는 테스트 도구
- `removeJobExecutions()`: 이전 실행 기록 삭제 (테스트 격리)

---

## 🧪 Testing

### 테스트 실행

```bash
./gradlew :spring-batch-onboarding:test --tests HelloJobConfigTest
```

### 실행 결과

```
> Task :spring-batch-onboarding:test

2026-01-27 21:42:30 INFO  --- [main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=helloJob]] launched with the following parameters: [{}]
2026-01-27 21:42:30 INFO  --- [main] o.s.batch.core.job.SimpleStepHandler     : Executing step: [helloStep]
2026-01-27 21:42:30 INFO  --- [main] c.b.job.hello.HelloJobConfig             : >>>>> Hello, Spring Batch!
2026-01-27 21:42:30 INFO  --- [main] c.b.job.hello.HelloJobConfig             : >>>>> Step 실행 완료
2026-01-27 21:42:30 INFO  --- [main] o.s.b.c.l.support.SimpleJobLauncher      : Job: [SimpleJob: [name=helloJob]] completed with the following parameters: [{}] and the following status: [COMPLETED]

BUILD SUCCESSFUL in 50s
```

**로그 분석:**
1. `Job: [SimpleJob: [name=helloJob]] launched` → Job 시작
2. `Executing step: [helloStep]` → Step 실행 시작
3. `>>>>> Hello, Spring Batch!` → 우리가 작성한 로그 출력
4. `>>>>> Step 실행 완료` → 우리가 작성한 로그 출력
5. `Job: ... status: [COMPLETED]` → Job 정상 완료

---

## 🐛 Lessons Learned

### 버그 1: Job 이름을 지정하지 않으면?

**증상:**
```java
@Bean
public Job helloJob() {
    return jobBuilderFactory.get(null)  // 이름을 null로 지정
            .start(helloStep())
            .build();
}
```

**에러 메시지:**
```
java.lang.IllegalArgumentException: Job name must not be null or empty
```

**원인:**
Job 이름은 Spring Batch가 Job을 식별하는 유일한 키입니다. 이름이 없으면 어떤 Job인지 구분할 수 없어요.

**해결:**
```java
return jobBuilderFactory.get("helloJob")  // 반드시 이름 지정
```

**교훈:**
Job과 Step의 이름은 **필수**입니다. 이름은 JobRepository에 저장되어 실행 이력을 추적하는 데 사용됩니다.

---

### 버그 2: @Configuration을 빼먹으면?

**증상:**
```java
// @Configuration  <- 이걸 주석 처리하면?
@RequiredArgsConstructor
public class HelloJobConfig {
    ...
}
```

**에러 메시지:**
```
NoSuchBeanDefinitionException: No qualifying bean of type 'org.springframework.batch.core.Job'
```

**원인:**
`@Configuration`이 없으면 Spring이 이 클래스를 설정 클래스로 인식하지 못합니다. 따라서 `@Bean`이 붙은 메서드들도 무시되고, Job Bean이 등록되지 않아요.

**해결:**
```java
@Configuration  // 반드시 필요!
@RequiredArgsConstructor
public class HelloJobConfig {
```

**교훈:**
Spring Batch의 Job Config 클래스는 반드시 `@Configuration`을 붙여야 합니다.

---

### 버그 3: 테스트에서 removeJobExecutions()를 빼먹으면?

**증상:**
```java
@BeforeEach
void setUp() {
    // jobRepositoryTestUtils.removeJobExecutions();  <- 주석 처리
}
```

**에러 메시지:**
```
JobInstanceAlreadyCompleteException: A job instance already exists and is complete for parameters={}
```

**원인:**
Spring Batch는 **동일한 JobParameters로 이미 완료된 Job은 다시 실행하지 않습니다**. 테스트를 두 번째 실행할 때, 첫 번째 실행 기록이 남아있어서 "이미 완료된 Job입니다"라고 에러가 발생해요.

**해결:**
```java
@BeforeEach
void setUp() {
    jobRepositoryTestUtils.removeJobExecutions();  // 이전 실행 기록 삭제
}
```

**교훈:**
테스트 격리를 위해 각 테스트 전에 `removeJobExecutions()`를 호출해야 합니다.

---

### 시니어 개발자의 사고방식

**질문: "왜 Job과 Step을 나눠서 만들어야 하나요? 하나로 합치면 안 되나요?"**

좋은 질문입니다! 실제로 간단한 작업은 Job 안에 Step 하나만 있어도 충분해요.

하지만 실무에서는:
1. **재사용성**: Step을 독립적으로 만들면 다른 Job에서도 재사용 가능
2. **디버깅**: 어떤 Step에서 문제가 발생했는지 명확히 알 수 있음
3. **재시작**: Step 단위로 실패 지점부터 재시작 가능
4. **병렬 처리**: 독립적인 Step들을 병렬로 실행 가능 (나중에 배울 예정)

**예시:**
```java
// ❌ 나쁜 예: 모든 로직을 하나의 Step에
@Bean
public Step bigStep() {
    return stepBuilderFactory.get("bigStep")
            .tasklet((contribution, chunkContext) -> {
                // 데이터 검증
                // 데이터 처리
                // 데이터 저장
                // 알림 발송
                // 리포트 생성
                // ...  <- 이러면 디버깅 지옥
                return RepeatStatus.FINISHED;
            })
            .build();
}

// ✅ 좋은 예: 각 작업을 독립적인 Step으로
@Bean
public Job dataProcessJob() {
    return jobBuilderFactory.get("dataProcessJob")
            .start(validateStep())      // 1단계: 검증
            .next(processStep())        // 2단계: 처리
            .next(saveStep())           // 3단계: 저장
            .next(notifyStep())         // 4단계: 알림
            .next(reportStep())         // 5단계: 리포트
            .build();
}
```

각 Step이 독립적이면, 예를 들어 "알림 발송"에서 문제가 생겼을 때, 처음부터 다시 실행하지 않고 알림 Step부터 재실행할 수 있어요.

---

## 🎯 Key Takeaways

1. **Job, Step, Tasklet의 계층 구조**
   - Job은 배치 작업의 최상위 단위
   - Step은 Job을 구성하는 독립적인 작업 단계
   - Tasklet은 Step 내에서 수행되는 단순 작업

2. **JobBuilderFactory와 StepBuilderFactory**
   - Spring Batch가 자동으로 주입해주는 빌더 팩토리
   - `.get(이름)` 메서드로 Job/Step 생성 시작
   - 이름은 필수! (JobRepository에서 식별에 사용)

3. **RepeatStatus의 의미**
   - `FINISHED`: Tasklet 종료 (일반적인 경우)
   - `CONTINUABLE`: Tasklet 계속 반복 (특수한 경우)

4. **테스트 시 주의사항**
   - `@SpringBatchTest` + `@SpringBootTest` 조합
   - `removeJobExecutions()`로 이전 실행 기록 삭제
   - `JobLauncherTestUtils`로 쉽게 Job 실행

5. **실무 팁**
   - 작업을 적절히 Step으로 나누면 재사용성, 디버깅, 재시작이 쉬워짐
   - 너무 많은 Step으로 나누지 말고, 논리적으로 독립적인 단위로 나누기

---

## 🔗 Next Steps

Step 2에서는 가장 간단한 형태의 Job을 만들어봤습니다. 하지만 실무에서는 **같은 Job을 여러 번 실행**해야 하는 경우가 많아요.

예를 들어:
- "2025-01-15"의 일일 정산
- "2025-01-16"의 일일 정산
- "2025-01-17"의 일일 정산

같은 Job이지만, 날짜가 다르면 다른 Job으로 취급되어야 합니다. 어떻게 할까요?

**Step 3 예고: JobParameters와 JobInstance**
- JobParameters로 Job에 파라미터 전달
- JobInstance의 개념 이해
- @JobScope, @StepScope의 Late Binding
- 동일한 Job을 다른 파라미터로 여러 번 실행하기

다음 Step에서 만나요! 🚀
