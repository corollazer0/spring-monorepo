# Spring Batch 온보딩 커리큘럼
/## "BatchFlow" - 금융권 배치 처리 마스터 프로젝트

> ⚠️ **이 문서는 전체 50-Step 참조/심화 자료입니다.**
> 실제 학습은 **[필수 트랙 커리큘럼 (01 문서)](./01-BatchFlow-Essential-Curriculum.md)** 을 기준으로 진행하세요.
> 필수 트랙(Step 1~13 + 심화 14~17)은 이 문서의 핵심을 문제주도형으로 압축한 것이며,
> 각 필수 Step이 이 문서의 어느 Step에 해당하는지 매핑표가 01 문서에 있습니다.
> (참고: 이 문서의 DB 예시는 MySQL 기준으로 작성되어 있으나, 실제 모듈은 H2 **MODE=MSSQLServer**로 동작합니다)

---

## 문서 정보

| 항목 | 내용 |
|------|------|
| 문서 버전 | 1.0.0 |
| 작성일 | 2025-01 |
| 기술 스택 | Spring Boot 2.7.17, Spring Batch 4.3.x, Java 1.8 |
| 대상 | Spring Batch 미경험 개발자 |
| 목표 | 실무 수준의 배치 시스템 설계 및 구현 능력 확보 |

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [도메인 이해: 왜 배치인가?](#2-도메인-이해-왜-배치인가)
3. [Phase 1: 기초 다지기 (Step 1-8)](#phase-1-기초-다지기-week-1-2)
4. [Phase 2: 핵심 패턴 (Step 9-18)](#phase-2-핵심-패턴-week-3-4)
5. [Phase 3: 오류 제어와 안정성 (Step 19-26)](#phase-3-오류-제어와-안정성-week-5-6)
6. [Phase 4: 성능 최적화 (Step 27-35)](#phase-4-성능-최적화-week-7-8)
7. [Phase 5: 운영과 모니터링 (Step 36-42)](#phase-5-운영과-모니터링-week-9-10)
8. [Phase 6: 실전 프로젝트 (Step 43-50)](#phase-6-실전-프로젝트-week-11-12)

---

# 1. 프로젝트 개요

## 1.1 학습 철학

```
┌─────────────────────────────────────────────────────────────────┐
│                    BatchFlow 학습 철학                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   "문제를 먼저 경험하고, 해결책을 찾아가는 여정"                │
│                                                                 │
│   ┌─────────┐      ┌─────────┐      ┌─────────┐                │
│   │ 문제    │ ──▶  │ 해결    │ ──▶  │ 개선    │                │
│   │ 인식    │      │ 구현    │      │ 리팩토링│                │
│   └─────────┘      └─────────┘      └─────────┘                │
│        │                │                │                      │
│        ▼                ▼                ▼                      │
│   "왜 필요한가?"   "어떻게 하는가?"  "더 좋은 방법은?"          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 1.2 프로젝트 구조

```
spring-batch-onboarding/
├── build.radle
├── settings.gradle
├── README.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/batchflow/
│   │   │       ├── BatchFlowApplication.java
│   │   │       │
│   │   │       ├── config/                    # 배치 설정
│   │   │       │   ├── BatchConfig.java
│   │   │       │   ├── DataSourceConfig.java
│   │   │       │   └── AsyncConfig.java
│   │   │       │
│   │   │       ├── job/                       # Job 정의
│   │   │       │   ├── settlement/            # 일일 정산
│   │   │       │   ├── dormant/               # 휴면 회원 전환
│   │   │       │   ├── notification/          # 대량 알림 발송
│   │   │       │   └── report/                # 리포트 생성
│   │   │       │
│   │   │       ├── domain/                    # 도메인 모델
│   │   │       │   ├── entity/
│   │   │       │   ├── repository/
│   │   │       │   └── dto/
│   │   │       │
│   │   │       ├── reader/                    # 커스텀 Reader
│   │   │       ├── processor/                 # 커스텀 Processor
│   │   │       ├── writer/                    # 커스텀 Writer
│   │   │       │
│   │   │       ├── listener/                  # 리스너
│   │   │       │   ├── job/
│   │   │       │   ├── step/
│   │   │       │   └── chunk/
│   │   │       │
│   │   │       ├── partitioner/               # 파티셔너
│   │   │       ├── tasklet/                   # Tasklet
│   │   │       └── util/                      # 유틸리티
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── schema-batch.sql
│   │
│   └── test/
│       └── java/
│           └── com/batchflow/
│               ├── job/
│               ├── reader/
│               └── integration/
│
└── docs/
    ├── README_Step01.md
    ├── README_Step02.md
    └── ...
```

## 1.3 기술 스택

| 카테고리 | 기술 | 버전 | 용도 |
|----------|------|------|------|
| Framework | Spring Boot | 2.7.17 | 애플리케이션 프레임워크 |
| Batch | Spring Batch | 4.3.x | 배치 처리 |
| Database | H2/MySQL | 8.0.x | 데이터 저장소 |
| ORM | Spring Data JPA | 2.7.x | 데이터 접근 |
| Test | JUnit 5 | 5.8.x | 단위/통합 테스트 |
| Test | @SpringBatchTest | 4.3.x | 배치 테스트 |

---

# 2. 도메인 이해: 왜 배치인가?

## 2.1 배치 처리의 필요성

```
┌─────────────────────────────────────────────────────────────────┐
│                 실시간 처리 vs 배치 처리                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  실시간 처리 (Online/OLTP)           배치 처리 (Batch)          │
│  ┌─────────────────────┐            ┌─────────────────────┐    │
│  │ • 즉시 응답 필요    │            │ • 지연 처리 허용    │    │
│  │ • 건별 트랜잭션     │            │ • 대량 데이터 처리  │    │
│  │ • 사용자 대기       │            │ • 무인 자동 실행    │    │
│  │ • 짧은 트랜잭션     │            │ • 긴 트랜잭션 허용  │    │
│  └─────────────────────┘            └─────────────────────┘    │
│                                                                 │
│  예) 계좌 이체, 상품 조회            예) 일일 정산, 이자 계산   │
│      잔액 확인, 로그인                   휴면 전환, 리포트 생성 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 2.2 금융권 배치 처리 사례

| 업무 | 처리 시점 | 데이터 규모 | 특징 |
|------|----------|------------|------|
| 일일 정산 | 매일 자정 | 수백만 건 | 정확성 필수, 실패 시 재처리 |
| 이자 계산 | 매월 말일 | 수천만 건 | 복잡한 계산 로직 |
| 휴면 회원 전환 | 매일 새벽 | 수십만 건 | 1년 미접속 계좌 대상 |
| 카드 명세서 발송 | 매월 특정일 | 수백만 건 | 외부 시스템 연동 |
| 연체 관리 | 매일 오전 | 수십만 건 | SMS/이메일 발송 연계 |

## 2.3 Spring Batch 핵심 용어

```
┌─────────────────────────────────────────────────────────────────┐
│                 Spring Batch 핵심 개념                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  JobLauncher ──▶ Job ──▶ Step ──▶ Tasklet or Chunk             │
│       │           │        │                                    │
│       │           │        └── ItemReader                       │
│       │           │        └── ItemProcessor                    │
│       │           │        └── ItemWriter                       │
│       │           │                                             │
│       │           └── JobInstance (Job + JobParameters)         │
│       │           └── JobExecution (실행 인스턴스)              │
│       │                                                         │
│       └── JobParameters (실행 파라미터)                         │
│                                                                 │
│  JobRepository: 모든 메타데이터 저장소                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 용어 정의

| 용어 | 정의 | 비유 |
|------|------|------|
| **Job** | 하나의 배치 작업 단위 | 하루 업무 전체 |
| **Step** | Job을 구성하는 독립적인 단계 | 업무의 각 단계 |
| **Tasklet** | 단순 작업 처리 방식 | 한 번에 처리하는 작업 |
| **Chunk** | 데이터를 묶음 단위로 처리 | 서류를 뭉치로 처리 |
| **JobInstance** | Job + JobParameters의 조합 | 특정 날짜의 업무 |
| **JobExecution** | JobInstance의 실제 실행 | 업무 수행 기록 |
| **ExecutionContext** | 실행 간 데이터 공유 저장소 | 메모장 |

---

# Phase 1: 기초 다지기 (Week 1-2)

---

## [Step 1] 프로젝트 초기화

### 1. 시나리오 및 요구사항

- **상황(Context)**: 팀에서 신규 배치 시스템을 구축하기로 결정했습니다. 먼저 Spring Batch 학습을 위한 프로젝트 환경을 구성해야 합니다.

- **문제(Problem)**: Spring Batch를 처음 접하는 개발자가 어디서부터 시작해야 할지 막막합니다.

- **미션(Mission)**: Spring Boot 2.7.17 기반의 Spring Batch 프로젝트를 생성하고, 기본 설정을 완료하세요.

### 2. 학습 목표 (Key Concepts)

- `@EnableBatchProcessing` 어노테이션의 역할
- Spring Batch의 메타데이터 테이블 구조
- H2 In-Memory DB를 활용한 개발 환경 구성

### 3. 구현 가이드 (Technical Guide)

**build.gradle**
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '2.7.17'
    id 'io.spring.dependency-management' version '1.0.15.RELEASE'
}

group = 'com.batchflow'
version = '1.0.0'
sourceCompatibility = '1.8'

repositories {
    mavenCentral()
}

dependencies {
    // Spring Batch
    implementation 'org.springframework.boot:spring-boot-starter-batch'
    
    // Database
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'mysql:mysql-connector-java'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.batch:spring-batch-test'
}

test {
    useJUnitPlatform()
}
```

**application.yml**
```yaml
spring:
  batch:
    # Spring Batch 메타데이터 테이블 자동 생성
    jdbc:
      initialize-schema: always
    job:
      # 애플리케이션 시작 시 Job 자동 실행 비활성화
      enabled: false
  
  datasource:
    url: jdbc:h2:mem:batchdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
    properties:
      hibernate:
        format_sql: true

logging:
  level:
    org.springframework.batch: DEBUG
```

**BatchFlowApplication.java**
```java
package com.batchflow;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing  // Spring Batch 기능 활성화
public class BatchFlowApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BatchFlowApplication.class, args);
    }
}
```

**Commit Message**: `feat: [Step 1] Spring Batch 프로젝트 초기화 및 기본 설정`

### 4. 마크다운 문서화 요구사항

`docs/README_Step01.md` 작성:
- `@EnableBatchProcessing`이 자동으로 등록하는 Bean 목록 정리
- Spring Batch 메타데이터 테이블 6개의 역할 설명
- `spring.batch.jdbc.initialize-schema` 옵션 설명

---

## [Step 2] 첫 번째 Job 생성 - Hello Batch

### 1. 시나리오 및 요구사항

- **상황(Context)**: 환경 설정은 완료되었습니다. 이제 실제로 동작하는 가장 간단한 배치 Job을 만들어 봅니다.

- **문제(Problem)**: Job, Step의 관계와 기본 구조를 이해해야 합니다.

- **미션(Mission)**: "Hello, Spring Batch!"를 출력하는 간단한 Job을 생성하고 실행하세요.

### 2. 학습 목표 (Key Concepts)

- `Job`, `Step`, `Tasklet`의 기본 구조
- `JobBuilderFactory`, `StepBuilderFactory` 사용법
- Tasklet의 `RepeatStatus` 반환값 의미

### 3. 구현 가이드 (Technical Guide)

**HelloJobConfig.java**
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HelloJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job helloJob() {
        return jobBuilderFactory.get("helloJob")  // Job 이름 지정
                .start(helloStep())                // 첫 번째 Step 지정
                .build();
    }
    
    @Bean
    public Step helloStep() {
        return stepBuilderFactory.get("helloStep")  // Step 이름 지정
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Hello, Spring Batch!");
                    log.info(">>>>> Step 실행 완료");
                    return RepeatStatus.FINISHED;  // Tasklet 종료
                })
                .build();
    }
}
```

**실행 방법 (테스트 코드)**
```java
package com.batchflow.job.hello;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes = {HelloJobConfig.class, TestBatchConfig.class})
class HelloJobConfigTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Test
    void helloJob_실행_성공() throws Exception {
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        
        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}
```

**TestBatchConfig.java**
```java
package com.batchflow.job.hello;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class TestBatchConfig {
}
```

**Commit Message**: `feat: [Step 2] Hello Batch - 첫 번째 Job/Step/Tasklet 구현`

### 4. 마크다운 문서화 요구사항

`docs/README_Step02.md` 작성:
- Job, Step, Tasklet의 관계 다이어그램
- `RepeatStatus.FINISHED` vs `RepeatStatus.CONTINUABLE` 차이
- JobBuilderFactory와 StepBuilderFactory가 제공하는 주요 메서드

---

## [Step 3] Job 실행 이해 - JobInstance와 JobParameters

### 1. 시나리오 및 요구사항

- **상황(Context)**: 동일한 Job을 다시 실행하니 "A job instance already exists and is complete" 에러가 발생합니다.

- **문제(Problem)**: Spring Batch는 동일한 Job + Parameters 조합은 한 번만 실행을 허용합니다. 이 동작 원리를 이해해야 합니다.

- **미션(Mission)**: JobParameters를 활용하여 동일 Job을 여러 번 실행할 수 있도록 구성하세요.

### 2. 학습 목표 (Key Concepts)

- `JobInstance`, `JobParameters`, `JobExecution`의 관계
- `@JobScope`, `@StepScope`의 역할
- Late Binding (지연 바인딩) 개념

### 3. 구현 가이드 (Technical Guide)

**JobParametersJobConfig.java**
```java
package com.batchflow.job.parameters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JobParametersJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job jobParametersJob() {
        return jobBuilderFactory.get("jobParametersJob")
                .start(jobParametersStep(null))  // null은 실행 시점에 주입됨
                .build();
    }
    
    @Bean
    @JobScope  // Job 실행 시점에 Bean 생성 (Late Binding)
    public Step jobParametersStep(
            @Value("#{jobParameters['requestDate']}") String requestDate) {
        
        return stepBuilderFactory.get("jobParametersStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> requestDate: {}", requestDate);
                    
                    // ChunkContext를 통한 JobParameters 접근
                    String version = chunkContext.getStepContext()
                            .getJobParameters()
                            .get("version")
                            .toString();
                    log.info(">>>>> version: {}", version);
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

**테스트 코드**
```java
@Test
void jobParameters_전달_확인() throws Exception {
    // given
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("requestDate", LocalDate.now().toString())
            .addLong("version", 1L)
            .toJobParameters();
    
    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
    
    // then
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
}
```

**Commit Message**: `feat: [Step 3] JobInstance/JobParameters 이해 및 Late Binding 적용`

### 4. 마크다운 문서화 요구사항

`docs/README_Step03.md` 작성:
- JobInstance, JobParameters, JobExecution 관계도
- `@JobScope`와 `@StepScope`의 차이점
- Late Binding이 필요한 이유와 활용 사례

---

## [Step 4] Job 흐름 제어 - Flow와 조건 분기

### 1. 시나리오 및 요구사항

- **상황(Context)**: 특정 Step의 실행 결과에 따라 다음 Step을 다르게 실행해야 합니다. 예를 들어, 데이터 검증 Step이 실패하면 에러 처리 Step으로 분기해야 합니다.

- **문제(Problem)**: 단순한 순차 실행만으로는 복잡한 비즈니스 로직을 표현할 수 없습니다.

- **미션(Mission)**: Step의 ExitStatus에 따라 조건 분기하는 Flow를 구현하세요.

### 2. 학습 목표 (Key Concepts)

- `Flow`와 `FlowBuilder`의 사용법
- `ExitStatus`와 `BatchStatus`의 차이
- `.on()`, `.to()`, `.from()`, `.end()`, `.fail()` 메서드

### 3. 구현 가이드 (Technical Guide)

**FlowJobConfig.java**
```java
package com.batchflow.job.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FlowJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job flowJob() {
        return jobBuilderFactory.get("flowJob")
                .start(validateStep())
                    .on("FAILED")           // validateStep이 FAILED이면
                    .to(failureHandleStep()) // failureHandleStep 실행
                    .on("*")                // failureHandleStep 결과와 상관없이
                    .end()                  // Job 종료
                .from(validateStep())
                    .on("*")                // FAILED 외의 모든 경우
                    .to(processStep())      // processStep 실행
                    .next(successStep())    // 이후 successStep 실행
                    .on("*")
                    .end()
                .end()  // Job 빌드 종료
                .build();
    }
    
    @Bean
    public Step validateStep() {
        return stepBuilderFactory.get("validateStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 데이터 검증 시작");
                    
                    // 검증 로직 (시뮬레이션)
                    boolean isValid = true;  // false로 변경하여 분기 테스트
                    
                    if (!isValid) {
                        log.error(">>>>> 데이터 검증 실패!");
                        contribution.setExitStatus(ExitStatus.FAILED);
                    } else {
                        log.info(">>>>> 데이터 검증 성공");
                    }
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step processStep() {
        return stepBuilderFactory.get("processStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 데이터 처리 중...");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step successStep() {
        return stepBuilderFactory.get("successStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 처리 완료!");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step failureHandleStep() {
        return stepBuilderFactory.get("failureHandleStep")
                .tasklet((contribution, chunkContext) -> {
                    log.error(">>>>> 에러 핸들링 수행...");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

**Commit Message**: `feat: [Step 4] Flow를 활용한 조건 분기 Job 구현`

### 4. 마크다운 문서화 요구사항

`docs/README_Step04.md` 작성:
- Flow 분기 다이어그램 작성
- `ExitStatus`와 `BatchStatus`의 차이 정리
- `.on()` 패턴 매칭 규칙 (*, ?, 와일드카드)

---

## [Step 5] Step 재사용 - Flow를 활용한 모듈화

### 1. 시나리오 및 요구사항

- **상황(Context)**: 여러 Job에서 공통으로 사용하는 Step 시퀀스가 있습니다. 예를 들어, "파일 다운로드 → 검증 → 적재" 과정이 여러 Job에서 반복됩니다.

- **문제(Problem)**: 동일한 Step 조합을 매번 복사-붙여넣기하면 유지보수가 어렵습니다.

- **미션(Mission)**: 공통 Step 시퀀스를 Flow로 모듈화하고 여러 Job에서 재사용하세요.

### 2. 학습 목표 (Key Concepts)

- `Flow`의 독립적 정의와 재사용
- `JobExecutionDecider`를 활용한 프로그래매틱 분기
- `FlowBuilder`의 고급 사용법

### 3. 구현 가이드 (Technical Guide)

**CommonFlowConfig.java**
```java
package com.batchflow.job.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CommonFlowConfig {
    
    private final StepBuilderFactory stepBuilderFactory;
    
    /**
     * 파일 처리 공통 Flow
     * 다운로드 → 검증 → 적재 순으로 실행
     */
    @Bean
    public Flow fileProcessFlow() {
        return new FlowBuilder<Flow>("fileProcessFlow")
                .start(downloadStep())
                .next(validateFileStep())
                .next(loadStep())
                .build();
    }
    
    @Bean
    public Step downloadStep() {
        return stepBuilderFactory.get("downloadStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [공통] 파일 다운로드 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step validateFileStep() {
        return stepBuilderFactory.get("validateFileStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [공통] 파일 검증 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step loadStep() {
        return stepBuilderFactory.get("loadStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [공통] 파일 적재 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

**FlowReuseJobConfig.java**
```java
package com.batchflow.job.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FlowReuseJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final Flow fileProcessFlow;  // 공통 Flow 주입
    
    @Bean
    public Job dailyReportJob() {
        return jobBuilderFactory.get("dailyReportJob")
                .start(fileProcessFlow)           // 공통 Flow 실행
                .next(generateReportStep())       // 이후 리포트 생성
                .end()
                .build();
    }
    
    @Bean
    public Job monthlySettlementJob() {
        return jobBuilderFactory.get("monthlySettlementJob")
                .start(fileProcessFlow)           // 동일한 공통 Flow 재사용
                .next(calculateSettlementStep())  // 이후 정산 처리
                .end()
                .build();
    }
    
    @Bean
    public Step generateReportStep() {
        return stepBuilderFactory.get("generateReportStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 일일 리포트 생성 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step calculateSettlementStep() {
        return stepBuilderFactory.get("calculateSettlementStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 월간 정산 계산 완료");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

**Commit Message**: `feat: [Step 5] 공통 Flow 모듈화 및 재사용 패턴 구현`

### 4. 마크다운 문서화 요구사항

`docs/README_Step05.md` 작성:
- Flow 재사용의 장점과 주의사항
- Flow vs Step 분리 기준
- 실무에서의 공통 Flow 설계 팁

---

## [Step 6] 의사결정 분기 - JobExecutionDecider

### 1. 시나리오 및 요구사항

- **상황(Context)**: Step의 실행 결과뿐 아니라, 외부 조건(현재 시간, 시스템 상태, 데이터 건수 등)에 따라 분기해야 합니다.

- **문제(Problem)**: `ExitStatus`만으로는 복잡한 비즈니스 조건을 표현하기 어렵습니다.

- **미션(Mission)**: `JobExecutionDecider`를 구현하여 홀수/짝수 일자에 따라 다른 Step을 실행하세요.

### 2. 학습 목표 (Key Concepts)

- `JobExecutionDecider` 인터페이스 구현
- 프로그래매틱 분기 로직 설계
- 커스텀 `FlowExecutionStatus` 정의

### 3. 구현 가이드 (Technical Guide)

**OddEvenDecider.java**
```java
package com.batchflow.job.decider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

import java.time.LocalDate;

@Slf4j
public class OddEvenDecider implements JobExecutionDecider {
    
    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, 
                                       StepExecution stepExecution) {
        
        int dayOfMonth = LocalDate.now().getDayOfMonth();
        log.info(">>>>> 현재 날짜: {}일", dayOfMonth);
        
        if (dayOfMonth % 2 == 0) {
            return new FlowExecutionStatus("EVEN");  // 짝수
        } else {
            return new FlowExecutionStatus("ODD");   // 홀수
        }
    }
}
```

**DeciderJobConfig.java**
```java
package com.batchflow.job.decider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DeciderJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job deciderJob() {
        return jobBuilderFactory.get("deciderJob")
                .start(startStep())
                .next(oddEvenDecider())      // Decider 실행
                    .on("ODD")               // ODD이면
                    .to(oddDayStep())        // 홀수 Step 실행
                .from(oddEvenDecider())
                    .on("EVEN")              // EVEN이면
                    .to(evenDayStep())       // 짝수 Step 실행
                .end()
                .build();
    }
    
    @Bean
    public JobExecutionDecider oddEvenDecider() {
        return new OddEvenDecider();
    }
    
    @Bean
    public Step startStep() {
        return stepBuilderFactory.get("startStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> Start Step 실행");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step oddDayStep() {
        return stepBuilderFactory.get("oddDayStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 홀수일 처리 로직 실행");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step evenDayStep() {
        return stepBuilderFactory.get("evenDayStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 짝수일 처리 로직 실행");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

**Commit Message**: `feat: [Step 6] JobExecutionDecider를 활용한 동적 분기 구현`

### 4. 마크다운 문서화 요구사항

`docs/README_Step06.md` 작성:
- `JobExecutionDecider` vs `ExitStatus` 분기 비교
- 실무 활용 사례 (시간대별 분기, 데이터 건수 분기 등)
- Decider 테스트 방법

---

## [Step 7] ExecutionContext - 실행 컨텍스트 공유

### 1. 시나리오 및 요구사항

- **상황(Context)**: 첫 번째 Step에서 조회한 데이터 건수를 다음 Step에서 사용해야 합니다. Step 간에 데이터를 어떻게 전달할까요?

- **문제(Problem)**: 각 Step은 독립적으로 실행되므로 직접 데이터를 주고받기 어렵습니다.

- **미션(Mission)**: `ExecutionContext`를 활용하여 Step 간 데이터를 공유하세요.

### 2. 학습 목표 (Key Concepts)

- `StepExecutionContext` vs `JobExecutionContext` 차이
- `ExecutionContextPromotionListener` 활용
- 데이터 공유 시 주의사항 (직렬화 가능 타입)

### 3. 구현 가이드 (Technical Guide)

**ExecutionContextJobConfig.java**
```java
package com.batchflow.job.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.listener.ExecutionContextPromotionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ExecutionContextJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job executionContextJob() {
        return jobBuilderFactory.get("executionContextJob")
                .start(countStep())
                .next(processWithCountStep())
                .build();
    }
    
    @Bean
    public Step countStep() {
        return stepBuilderFactory.get("countStep")
                .tasklet((contribution, chunkContext) -> {
                    // 데이터 건수 조회 (시뮬레이션)
                    int dataCount = 12345;
                    
                    // StepExecutionContext에 저장
                    ExecutionContext stepContext = chunkContext.getStepContext()
                            .getStepExecution()
                            .getExecutionContext();
                    stepContext.putInt("dataCount", dataCount);
                    
                    log.info(">>>>> 데이터 건수 저장: {}", dataCount);
                    
                    return RepeatStatus.FINISHED;
                })
                // StepContext → JobContext로 승격
                .listener(promotionListener())
                .build();
    }
    
    @Bean
    public ExecutionContextPromotionListener promotionListener() {
        ExecutionContextPromotionListener listener = 
                new ExecutionContextPromotionListener();
        listener.setKeys(new String[]{"dataCount"});  // 승격할 키 지정
        return listener;
    }
    
    @Bean
    public Step processWithCountStep() {
        return stepBuilderFactory.get("processWithCountStep")
                .tasklet((contribution, chunkContext) -> {
                    // JobExecutionContext에서 읽기
                    ExecutionContext jobContext = chunkContext.getStepContext()
                            .getStepExecution()
                            .getJobExecution()
                            .getExecutionContext();
                    
                    int dataCount = jobContext.getInt("dataCount");
                    log.info(">>>>> 전달받은 데이터 건수: {}", dataCount);
                    log.info(">>>>> {} 건 처리 시작...", dataCount);
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

**Commit Message**: `feat: [Step 7] ExecutionContext를 활용한 Step 간 데이터 공유`

### 4. 마크다운 문서화 요구사항

`docs/README_Step07.md` 작성:
- StepExecutionContext vs JobExecutionContext 범위 다이어그램
- PromotionListener의 역할과 사용 시점
- ExecutionContext에 저장 가능한 데이터 타입 제한

---

## [Step 8] Batch 메타데이터 테이블 분석

### 1. 시나리오 및 요구사항

- **상황(Context)**: Job이 실행된 후 H2 콘솔에서 `BATCH_` 테이블들을 확인하니 다양한 데이터가 저장되어 있습니다.

- **문제(Problem)**: 각 테이블의 역할과 데이터 구조를 이해해야 운영 시 문제 분석이 가능합니다.

- **미션(Mission)**: Spring Batch 메타데이터 테이블 6개의 역할을 분석하고 문서화하세요.

### 2. 학습 목표 (Key Concepts)

- `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION`, `BATCH_JOB_EXECUTION_PARAMS`
- `BATCH_STEP_EXECUTION`, `BATCH_STEP_EXECUTION_CONTEXT`
- `BATCH_JOB_EXECUTION_CONTEXT`
- 테이블 간 관계 이해

### 3. 구현 가이드 (Technical Guide)

**메타데이터 테이블 구조**

```sql
-- BATCH_JOB_INSTANCE: Job의 논리적 실행 단위
-- Job 이름 + JobParameters 조합으로 유니크
SELECT * FROM BATCH_JOB_INSTANCE;

-- BATCH_JOB_EXECUTION: Job의 실제 실행 기록
-- 하나의 JobInstance가 여러 번 실행될 수 있음 (재실행 시)
SELECT * FROM BATCH_JOB_EXECUTION;

-- BATCH_JOB_EXECUTION_PARAMS: JobParameters 저장
SELECT * FROM BATCH_JOB_EXECUTION_PARAMS;

-- BATCH_STEP_EXECUTION: Step 실행 기록
SELECT * FROM BATCH_STEP_EXECUTION;

-- BATCH_JOB_EXECUTION_CONTEXT: Job 레벨 ExecutionContext
SELECT * FROM BATCH_JOB_EXECUTION_CONTEXT;

-- BATCH_STEP_EXECUTION_CONTEXT: Step 레벨 ExecutionContext
SELECT * FROM BATCH_STEP_EXECUTION_CONTEXT;
```

**테이블 관계 다이어그램**

```
┌─────────────────────────────────────────────────────────────────┐
│              Spring Batch 메타데이터 테이블 관계                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  BATCH_JOB_INSTANCE                                            │
│  ┌─────────────────┐                                           │
│  │ JOB_INSTANCE_ID │────┐                                      │
│  │ JOB_NAME        │    │                                      │
│  │ JOB_KEY         │    │ (1:N)                                │
│  └─────────────────┘    │                                      │
│                         ▼                                      │
│  BATCH_JOB_EXECUTION ◀──┘       BATCH_JOB_EXECUTION_PARAMS    │
│  ┌─────────────────┐            ┌─────────────────┐           │
│  │ JOB_EXECUTION_ID│────────────│ EXECUTION_ID    │           │
│  │ STATUS          │            │ KEY_NAME        │           │
│  │ START_TIME      │            │ STRING_VAL      │           │
│  │ END_TIME        │            └─────────────────┘           │
│  └─────────────────┘                                          │
│         │                                                      │
│         │ (1:N)                                                │
│         ▼                                                      │
│  BATCH_STEP_EXECUTION                                          │
│  ┌─────────────────┐                                           │
│  │ STEP_EXECUTION_ID                                           │
│  │ STEP_NAME       │                                           │
│  │ READ_COUNT      │                                           │
│  │ WRITE_COUNT     │                                           │
│  │ COMMIT_COUNT    │                                           │
│  │ ROLLBACK_COUNT  │                                           │
│  └─────────────────┘                                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**메타데이터 조회 유틸리티**
```java
package com.batchflow.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchMetadataUtil {
    
    private final JobExplorer jobExplorer;
    
    public void printJobExecutionHistory(String jobName) {
        List<Long> instanceIds = jobExplorer.getJobInstances(jobName, 0, 10)
                .stream()
                .map(instance -> instance.getInstanceId())
                .toList();
        
        for (Long instanceId : instanceIds) {
            List<JobExecution> executions = 
                    jobExplorer.getJobExecutions(
                            jobExplorer.getJobInstance(instanceId));
            
            for (JobExecution execution : executions) {
                log.info("Instance: {}, Execution: {}, Status: {}", 
                        instanceId, 
                        execution.getId(), 
                        execution.getStatus());
            }
        }
    }
}
```

**Commit Message**: `docs: [Step 8] Batch 메타데이터 테이블 분석 및 문서화`

### 4. 마크다운 문서화 요구사항

`docs/README_Step08.md` 작성:
- 6개 메타데이터 테이블의 역할 상세 설명
- 테이블 간 관계 ER 다이어그램
- 실무에서 자주 사용하는 조회 쿼리 모음

---

# Phase 2: 핵심 패턴 (Week 3-4)

---

## [Step 9] Chunk 모델 이해 - 첫 번째 Chunk 기반 Step

### 1. 시나리오 및 요구사항

- **상황(Context)**: 지금까지는 Tasklet으로 단순 작업을 처리했습니다. 이제 실제로 DB에서 대량 데이터를 읽어 가공하고 저장해야 합니다.

- **문제(Problem)**: 100만 건의 데이터를 한 번에 메모리에 올리면 OOM(Out Of Memory)이 발생합니다.

- **미션(Mission)**: Chunk 기반 처리 모델을 이해하고, 1000건씩 끊어서 처리하는 Step을 구현하세요.

### 2. 학습 목표 (Key Concepts)

- `ItemReader`, `ItemProcessor`, `ItemWriter` 인터페이스
- Chunk 크기 설정과 트랜잭션 경계
- Chunk 지향 처리의 장점

### 3. 구현 가이드 (Technical Guide)

**엔티티 및 Repository**
```java
// Member.java
package com.batchflow.domain.entity;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String email;
    
    @Enumerated(EnumType.STRING)
    private MemberStatus status;  // ACTIVE, DORMANT
    
    private LocalDateTime lastLoginAt;
    
    private LocalDateTime createdAt;
}

// MemberStatus.java
public enum MemberStatus {
    ACTIVE, DORMANT
}
```

**ChunkJobConfig.java**
```java
package com.batchflow.job.chunk;

import com.batchflow.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ChunkJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    private static final int CHUNK_SIZE = 10;
    
    @Bean
    public Job chunkJob() {
        return jobBuilderFactory.get("chunkJob")
                .start(chunkStep())
                .build();
    }
    
    @Bean
    public Step chunkStep() {
        return stepBuilderFactory.get("chunkStep")
                .<Member, Member>chunk(CHUNK_SIZE)  // <Input, Output>
                .reader(memberReader())
                .processor(memberProcessor())
                .writer(memberWriter())
                .build();
    }
    
    @Bean
    public ItemReader<Member> memberReader() {
        // 테스트용 더미 데이터 생성
        List<Member> members = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            members.add(Member.builder()
                    .id((long) i)
                    .name("User" + i)
                    .email("user" + i + "@test.com")
                    .status(MemberStatus.ACTIVE)
                    .lastLoginAt(LocalDateTime.now().minusDays(i * 10))
                    .build());
        }
        return new ListItemReader<>(members);
    }
    
    @Bean
    public ItemProcessor<Member, Member> memberProcessor() {
        return member -> {
            log.info(">>>>> Processing: {}", member.getName());
            // 1년 이상 미접속 시 휴면 처리
            if (member.getLastLoginAt().isBefore(
                    LocalDateTime.now().minusYears(1))) {
                member.setStatus(MemberStatus.DORMANT);
            }
            return member;
        };
    }
    
    @Bean
    public ItemWriter<Member> memberWriter() {
        return members -> {
            log.info(">>>>> Writing {} members", members.size());
            for (Member member : members) {
                log.info("  - {}: {}", member.getName(), member.getStatus());
            }
        };
    }
}
```

**Chunk 처리 흐름 다이어그램**
```
┌─────────────────────────────────────────────────────────────────┐
│                    Chunk 지향 처리 흐름                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                    │
│  │  Reader │    │Processor│    │  Writer │                    │
│  └────┬────┘    └────┬────┘    └────┬────┘                    │
│       │              │              │                          │
│       ▼              ▼              ▼                          │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                    Transaction                          │  │
│  │  ┌───┐                                                  │  │
│  │  │ 1 │──▶ process ──┐                                   │  │
│  │  ├───┤              │                                   │  │
│  │  │ 2 │──▶ process ──┤                                   │  │
│  │  ├───┤              │    ┌──────────┐                   │  │
│  │  │ 3 │──▶ process ──┼──▶ │  write   │                   │  │
│  │  ├───┤              │    │ (chunk)  │                   │  │
│  │  │...│──▶ process ──┤    └──────────┘                   │  │
│  │  ├───┤              │                                   │  │
│  │  │10 │──▶ process ──┘                                   │  │
│  │  └───┘                                                  │  │
│  │  (chunk size = 10)           commit                     │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
│  * Reader는 한 건씩 읽음                                       │
│  * Processor는 한 건씩 처리                                    │
│  * Writer는 chunk 단위로 일괄 처리                             │
│  * 트랜잭션은 chunk 단위로 커밋                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Commit Message**: `feat: [Step 9] Chunk 기반 처리 모델 구현 - Reader/Processor/Writer`

### 4. 마크다운 문서화 요구사항

`docs/README_Step09.md` 작성:
- Tasklet vs Chunk 모델 비교
- Chunk 크기 결정 기준 (메모리, 트랜잭션 시간)
- Chunk 처리 흐름도 상세 설명

---

## [Step 10] ItemReader 구현 - JdbcCursorItemReader

### 1. 시나리오 및 요구사항

- **상황(Context)**: 이제 실제 DB에서 데이터를 읽어야 합니다. MySQL에서 100만 건의 회원 정보를 읽어 처리해야 합니다.

- **문제(Problem)**: `ListItemReader`는 메모리에 모두 올리므로 대용량 처리에 부적합합니다.

- **미션(Mission)**: `JdbcCursorItemReader`를 사용하여 DB Cursor 기반으로 데이터를 읽으세요.

### 2. 학습 목표 (Key Concepts)

- `JdbcCursorItemReader` 설정 방법
- `RowMapper` 구현
- Cursor vs Paging 방식 비교

### 3. 구현 가이드 (Technical Guide)

**JdbcCursorReaderJobConfig.java**
```java
package com.batchflow.job.reader;

import com.batchflow.domain.entity.Member;
import com.batchflow.domain.entity.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.ResultSet;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JdbcCursorReaderJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    
    private static final int CHUNK_SIZE = 1000;
    
    @Bean
    public Job jdbcCursorReaderJob() {
        return jobBuilderFactory.get("jdbcCursorReaderJob")
                .start(jdbcCursorReaderStep())
                .build();
    }
    
    @Bean
    public Step jdbcCursorReaderStep() {
        return stepBuilderFactory.get("jdbcCursorReaderStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(jdbcCursorMemberReader())
                .writer(memberPrintWriter())
                .build();
    }
    
    @Bean
    public JdbcCursorItemReader<Member> jdbcCursorMemberReader() {
        return new JdbcCursorItemReaderBuilder<Member>()
                .name("jdbcCursorMemberReader")
                .dataSource(dataSource)
                .sql("SELECT id, name, email, status, last_login_at, created_at " +
                     "FROM member " +
                     "WHERE status = 'ACTIVE' " +
                     "ORDER BY id")
                .rowMapper((rs, rowNum) -> Member.builder()
                        .id(rs.getLong("id"))
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .status(MemberStatus.valueOf(rs.getString("status")))
                        .lastLoginAt(rs.getTimestamp("last_login_at").toLocalDateTime())
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build())
                .fetchSize(CHUNK_SIZE)  // DB에서 한 번에 가져올 크기
                .build();
    }
    
    @Bean
    public ItemWriter<Member> memberPrintWriter() {
        return members -> {
            for (Member member : members) {
                log.info(">>>>> Member: {} ({})", member.getName(), member.getEmail());
            }
        };
    }
}
```

**Cursor vs Paging 비교**
```
┌─────────────────────────────────────────────────────────────────┐
│              Cursor vs Paging 방식 비교                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Cursor 방식 (JdbcCursorItemReader)                            │
│  ┌───────────────────────────────────────┐                     │
│  │ • DB Connection 유지                   │                     │
│  │ • 스트리밍 방식으로 한 건씩 fetch       │                     │
│  │ • 네트워크 오버헤드 적음                │                     │
│  │ • Connection 점유 시간 김               │                     │
│  │ • Thread-safe 하지 않음 (병렬처리 불가) │                     │
│  └───────────────────────────────────────┘                     │
│                                                                 │
│  Paging 방식 (JdbcPagingItemReader)                            │
│  ┌───────────────────────────────────────┐                     │
│  │ • 페이지 단위로 쿼리 실행               │                     │
│  │ • Connection 짧게 점유                  │                     │
│  │ • Thread-safe (병렬처리 가능)           │                     │
│  │ • 네트워크 왕복 많음                    │                     │
│  │ • 정렬 + OFFSET 성능 이슈 가능          │                     │
│  └───────────────────────────────────────┘                     │
│                                                                 │
│  선택 기준:                                                     │
│  • 단일 스레드 + 대용량 → Cursor                               │
│  • 멀티 스레드 + 병렬처리 → Paging                             │
│  • Connection 타임아웃 이슈 → Paging                           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Commit Message**: `feat: [Step 10] JdbcCursorItemReader 구현 - DB Cursor 기반 읽기`

### 4. 마크다운 문서화 요구사항

`docs/README_Step10.md` 작성:
- JdbcCursorItemReader 설정 옵션 상세 설명
- fetchSize 튜닝 가이드
- Cursor 방식의 장단점

---

## [Step 11] ItemReader 구현 - JdbcPagingItemReader

### 1. 시나리오 및 요구사항

- **상황(Context)**: Cursor 방식으로 처리하니 DB Connection을 오래 점유하여 타임아웃이 발생합니다.

- **문제(Problem)**: 네트워크 지연이나 처리 시간이 길어지면 Connection이 끊어집니다.

- **미션(Mission)**: `JdbcPagingItemReader`를 사용하여 페이지 단위로 데이터를 읽으세요.

### 2. 학습 목표 (Key Concepts)

- `JdbcPagingItemReader` 설정
- `PagingQueryProvider` 구현
- 페이징 성능 최적화 (정렬 키 인덱스)

### 3. 구현 가이드 (Technical Guide)

**JdbcPagingReaderJobConfig.java**
```java
package com.batchflow.job.reader;

import com.batchflow.domain.entity.Member;
import com.batchflow.domain.entity.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JdbcPagingReaderJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    
    private static final int CHUNK_SIZE = 1000;
    private static final int PAGE_SIZE = 1000;
    
    @Bean
    public Job jdbcPagingReaderJob() throws Exception {
        return jobBuilderFactory.get("jdbcPagingReaderJob")
                .start(jdbcPagingReaderStep())
                .build();
    }
    
    @Bean
    public Step jdbcPagingReaderStep() throws Exception {
        return stepBuilderFactory.get("jdbcPagingReaderStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(jdbcPagingMemberReader(null))
                .writer(pagingMemberWriter())
                .build();
    }
    
    @Bean
    @StepScope
    public JdbcPagingItemReader<Member> jdbcPagingMemberReader(
            @Value("#{jobParameters['status']}") String status) throws Exception {
        
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("status", status != null ? status : "ACTIVE");
        
        return new JdbcPagingItemReaderBuilder<Member>()
                .name("jdbcPagingMemberReader")
                .dataSource(dataSource)
                .pageSize(PAGE_SIZE)
                .queryProvider(createQueryProvider())
                .parameterValues(parameterValues)
                .rowMapper((rs, rowNum) -> Member.builder()
                        .id(rs.getLong("id"))
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .status(MemberStatus.valueOf(rs.getString("status")))
                        .lastLoginAt(rs.getTimestamp("last_login_at").toLocalDateTime())
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build())
                .build();
    }
    
    @Bean
    public PagingQueryProvider createQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean factory = 
                new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource);
        factory.setSelectClause("SELECT id, name, email, status, last_login_at, created_at");
        factory.setFromClause("FROM member");
        factory.setWhereClause("WHERE status = :status");
        
        // 정렬 키 설정 (반드시 인덱스가 있어야 성능 보장)
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);
        factory.setSortKeys(sortKeys);
        
        return factory.getObject();
    }
    
    @Bean
    public ItemWriter<Member> pagingMemberWriter() {
        return members -> {
            log.info(">>>>> {} 건 처리 완료", members.size());
        };
    }
}
```

**Commit Message**: `feat: [Step 11] JdbcPagingItemReader 구현 - 페이지 기반 읽기`

### 4. 마크다운 문서화 요구사항

`docs/README_Step11.md` 작성:
- PagingQueryProvider의 역할과 DB별 구현체
- 페이징 쿼리 최적화 팁 (인덱스, 정렬 키)
- pageSize vs chunkSize 설정 가이드

---

## [Step 12] ItemReader 구현 - JpaPagingItemReader

### 1. 시나리오 및 요구사항

- **상황(Context)**: 프로젝트에서 JPA를 사용하고 있습니다. JDBC가 아닌 JPA 방식으로 데이터를 읽고 싶습니다.

- **문제(Problem)**: Entity 관계 매핑과 JPQL을 활용해야 합니다.

- **미션(Mission)**: `JpaPagingItemReader`를 사용하여 JPA Entity 기반으로 데이터를 읽으세요.

### 2. 학습 목표 (Key Concepts)

- `JpaPagingItemReader` 설정
- JPQL 쿼리 작성
- JPA와 배치 처리 시 주의사항 (영속성 컨텍스트)

### 3. 구현 가이드 (Technical Guide)

**JpaPagingReaderJobConfig.java**
```java
package com.batchflow.job.reader;

import com.batchflow.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JpaPagingReaderJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    
    private static final int CHUNK_SIZE = 1000;
    
    @Bean
    public Job jpaPagingReaderJob() {
        return jobBuilderFactory.get("jpaPagingReaderJob")
                .start(jpaPagingReaderStep())
                .build();
    }
    
    @Bean
    public Step jpaPagingReaderStep() {
        return stepBuilderFactory.get("jpaPagingReaderStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(jpaPagingMemberReader(null))
                .writer(jpaMemberWriter())
                .build();
    }
    
    @Bean
    @StepScope
    public JpaPagingItemReader<Member> jpaPagingMemberReader(
            @Value("#{jobParameters['status']}") String status) {
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", status != null ? 
                MemberStatus.valueOf(status) : MemberStatus.ACTIVE);
        
        return new JpaPagingItemReaderBuilder<Member>()
                .name("jpaPagingMemberReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT m FROM Member m " +
                           "WHERE m.status = :status " +
                           "ORDER BY m.id ASC")
                .parameterValues(parameters)
                .build();
    }
    
    @Bean
    public ItemWriter<Member> jpaMemberWriter() {
        return members -> {
            for (Member member : members) {
                log.info(">>>>> Member: {} ({})", 
                        member.getName(), member.getStatus());
            }
        };
    }
}
```

**JPA 배치 처리 주의사항**
```java
// ⚠️ 주의: JpaPagingItemReader는 트랜잭션마다 새 EntityManager 생성
// 따라서 Lazy Loading이 chunk 경계를 넘어가면 LazyInitializationException 발생

// ✅ 해결책 1: Fetch Join 사용
@Bean
public JpaPagingItemReader<Member> memberWithOrdersReader() {
    return new JpaPagingItemReaderBuilder<Member>()
            .name("memberWithOrdersReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT DISTINCT m FROM Member m " +
                       "LEFT JOIN FETCH m.orders " +
                       "ORDER BY m.id")
            .pageSize(CHUNK_SIZE)
            .build();
}

// ✅ 해결책 2: DTO Projection 사용
@Bean
public JpaPagingItemReader<MemberDto> memberDtoReader() {
    return new JpaPagingItemReaderBuilder<MemberDto>()
            .name("memberDtoReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT new com.batchflow.dto.MemberDto(" +
                       "m.id, m.name, m.email) " +
                       "FROM Member m ORDER BY m.id")
            .pageSize(CHUNK_SIZE)
            .build();
}
```

**Commit Message**: `feat: [Step 12] JpaPagingItemReader 구현 - JPA Entity 기반 읽기`

### 4. 마크다운 문서화 요구사항

`docs/README_Step12.md` 작성:
- JpaPagingItemReader vs JdbcPagingItemReader 비교
- 영속성 컨텍스트와 배치 처리 관계
- LazyInitializationException 해결 방법

---

## [Step 13] ItemWriter 구현 - JpaItemWriter

### 1. 시나리오 및 요구사항

- **상황(Context)**: 읽어온 데이터를 가공 후 DB에 저장해야 합니다.

- **문제(Problem)**: 대량 데이터를 건건이 저장하면 성능이 심각하게 저하됩니다.

- **미션(Mission)**: `JpaItemWriter`를 사용하여 배치 단위로 효율적으로 저장하세요.

### 2. 학습 목표 (Key Concepts)

- `JpaItemWriter` 설정
- `JdbcBatchItemWriter`와의 차이
- Bulk Insert 최적화

### 3. 구현 가이드 (Technical Guide)

**JpaWriterJobConfig.java**
```java
package com.batchflow.job.writer;

import com.batchflow.domain.entity.Member;
import com.batchflow.domain.entity.MemberHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JpaWriterJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    
    private static final int CHUNK_SIZE = 1000;
    
    @Bean
    public Job jpaWriterJob() {
        return jobBuilderFactory.get("jpaWriterJob")
                .start(jpaWriterStep())
                .build();
    }
    
    @Bean
    public Step jpaWriterStep() {
        return stepBuilderFactory.get("jpaWriterStep")
                .<Member, MemberHistory>chunk(CHUNK_SIZE)
                .reader(memberReaderForHistory())
                .processor(memberToHistoryProcessor())
                .writer(memberHistoryWriter())
                .build();
    }
    
    @Bean
    public JpaPagingItemReader<Member> memberReaderForHistory() {
        return new JpaPagingItemReaderBuilder<Member>()
                .name("memberReaderForHistory")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT m FROM Member m ORDER BY m.id")
                .pageSize(CHUNK_SIZE)
                .build();
    }
    
    @Bean
    public ItemProcessor<Member, MemberHistory> memberToHistoryProcessor() {
        return member -> MemberHistory.builder()
                .memberId(member.getId())
                .memberName(member.getName())
                .status(member.getStatus())
                .snapshotAt(LocalDateTime.now())
                .build();
    }
    
    @Bean
    public JpaItemWriter<MemberHistory> memberHistoryWriter() {
        JpaItemWriter<MemberHistory> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
```

**JdbcBatchItemWriter (대용량 Insert 최적화)**
```java
@Bean
public JdbcBatchItemWriter<MemberHistory> jdbcBatchMemberHistoryWriter() {
    return new JdbcBatchItemWriterBuilder<MemberHistory>()
            .dataSource(dataSource)
            .sql("INSERT INTO member_history " +
                 "(member_id, member_name, status, snapshot_at) " +
                 "VALUES (:memberId, :memberName, :status, :snapshotAt)")
            .beanMapped()
            .build();
}
```

**Commit Message**: `feat: [Step 13] JpaItemWriter/JdbcBatchItemWriter 구현 - 배치 저장`

### 4. 마크다운 문서화 요구사항

`docs/README_Step13.md` 작성:
- JpaItemWriter vs JdbcBatchItemWriter 성능 비교
- Bulk Insert 최적화 설정 (JDBC batch size)
- Update/Delete 처리를 위한 커스텀 Writer

---

## [Step 14] ItemProcessor 구현 - CompositeItemProcessor

### 1. 시나리오 및 요구사항

- **상황(Context)**: 데이터 가공 로직이 복잡해지면서 하나의 Processor에 너무 많은 책임이 집중됩니다.

- **문제(Problem)**: 검증 → 변환 → 필터링 등 여러 처리를 단계별로 분리하고 싶습니다.

- **미션(Mission)**: `CompositeItemProcessor`를 사용하여 여러 Processor를 체이닝하세요.

### 2. 학습 목표 (Key Concepts)

- `CompositeItemProcessor` 체이닝
- `ItemProcessor`에서 null 반환의 의미 (필터링)
- 단일 책임 원칙(SRP) 적용

### 3. 구현 가이드 (Technical Guide)

**CompositeProcessorJobConfig.java**
```java
package com.batchflow.job.processor;

import com.batchflow.domain.entity.Member;
import com.batchflow.domain.entity.MemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Arrays;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CompositeProcessorJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    
    private static final int CHUNK_SIZE = 100;
    
    @Bean
    public Job compositeProcessorJob() {
        return jobBuilderFactory.get("compositeProcessorJob")
                .start(compositeProcessorStep())
                .build();
    }
    
    @Bean
    public Step compositeProcessorStep() {
        return stepBuilderFactory.get("compositeProcessorStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(compositeProcessorReader())
                .processor(compositeProcessor())
                .writer(compositeProcessorWriter())
                .build();
    }
    
    @Bean
    public JpaPagingItemReader<Member> compositeProcessorReader() {
        return new JpaPagingItemReaderBuilder<Member>()
                .name("compositeProcessorReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT m FROM Member m ORDER BY m.id")
                .pageSize(CHUNK_SIZE)
                .build();
    }
    
    @Bean
    public CompositeItemProcessor<Member, Member> compositeProcessor() {
        CompositeItemProcessor<Member, Member> processor = 
                new CompositeItemProcessor<>();
        
        processor.setDelegates(Arrays.asList(
                validationProcessor(),    // 1. 검증
                dormantFilterProcessor(), // 2. 필터링 (조건 미충족 시 제외)
                dormantStatusProcessor()  // 3. 상태 변경
        ));
        
        return processor;
    }
    
    /**
     * 1단계: 데이터 검증
     */
    private ItemProcessor<Member, Member> validationProcessor() {
        return member -> {
            if (member.getEmail() == null || member.getEmail().isEmpty()) {
                log.warn(">>>>> 이메일 없는 회원 발견: {}", member.getId());
                return null;  // null 반환 시 해당 아이템은 Writer로 전달되지 않음
            }
            return member;
        };
    }
    
    /**
     * 2단계: 휴면 대상 필터링 (1년 이상 미접속)
     */
    private ItemProcessor<Member, Member> dormantFilterProcessor() {
        return member -> {
            if (member.getLastLoginAt() == null) {
                return null;
            }
            
            LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
            if (member.getLastLoginAt().isAfter(oneYearAgo)) {
                // 1년 이내 접속한 회원은 제외
                return null;
            }
            
            log.info(">>>>> 휴면 대상 회원: {} (최종 접속: {})", 
                    member.getName(), member.getLastLoginAt());
            return member;
        };
    }
    
    /**
     * 3단계: 휴면 상태로 변경
     */
    private ItemProcessor<Member, Member> dormantStatusProcessor() {
        return member -> {
            member.setStatus(MemberStatus.DORMANT);
            log.info(">>>>> 휴면 전환 완료: {}", member.getName());
            return member;
        };
    }
    
    @Bean
    public ItemWriter<Member> compositeProcessorWriter() {
        return members -> {
            log.info(">>>>> {} 명 휴면 전환 저장 완료", members.size());
        };
    }
}
```

**Commit Message**: `feat: [Step 14] CompositeItemProcessor 구현 - Processor 체이닝`

### 4. 마크다운 문서화 요구사항

`docs/README_Step14.md` 작성:
- CompositeItemProcessor 처리 흐름
- null 반환을 통한 필터링 패턴
- Processor 분리 기준과 설계 원칙

---

## [Step 15-18] 실전 예제: 휴면 회원 전환 Job 완성

### Step 15: 휴면 회원 전환 Job 통합

**커밋**: `feat: [Step 15] 휴면 회원 전환 Job 통합 구현`

### Step 16: 휴면 전환 알림 발송 Step 추가

**커밋**: `feat: [Step 16] 휴면 전환 알림 발송 Step 구현`

### Step 17: 단위 테스트 작성

**커밋**: `test: [Step 17] 휴면 회원 전환 Job 단위 테스트`

### Step 18: 통합 테스트 및 문서화

**커밋**: `test: [Step 18] 휴면 회원 전환 Job 통합 테스트 및 문서화`

---

# Phase 3: 오류 제어와 안정성 (Week 5-6)

---

## [Step 19] Skip 처리 - 오류 건 건너뛰기

### 1. 시나리오 및 요구사항

- **상황(Context)**: 100만 건 처리 중 1건의 데이터 오류로 전체 Job이 실패합니다.

- **문제(Problem)**: 금융 시스템에서 오류 1건 때문에 전체 마감이 지연되면 안 됩니다.

- **미션(Mission)**: Skip 기능을 적용하여 오류 건은 건너뛰고 정상 건만 처리하세요.

### 2. 학습 목표 (Key Concepts)

- `.faultTolerant()` 활성화
- `.skip()`, `.skipLimit()`, `.noSkip()` 설정
- `SkipListener`를 통한 Skip 건 로깅

### 3. 구현 가이드 (Technical Guide)

**SkipJobConfig.java**
```java
package com.batchflow.job.skip;

import com.batchflow.domain.entity.Member;
import com.batchflow.listener.MemberSkipListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SkipJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    private static final int CHUNK_SIZE = 5;
    
    @Bean
    public Job skipJob() {
        return jobBuilderFactory.get("skipJob")
                .start(skipStep())
                .build();
    }
    
    @Bean
    public Step skipStep() {
        return stepBuilderFactory.get("skipStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(skipMemberReader())
                .processor(skipMemberProcessor())
                .writer(skipMemberWriter())
                
                // Skip 설정 활성화
                .faultTolerant()
                .skip(IllegalArgumentException.class)  // Skip 대상 예외
                .skipLimit(3)                          // 최대 Skip 허용 횟수
                .noSkip(NullPointerException.class)    // Skip 금지 예외
                
                // Skip 발생 시 로깅
                .listener(new MemberSkipListener())
                .build();
    }
    
    @Bean
    public ItemReader<Member> skipMemberReader() {
        return new ListItemReader<>(Arrays.asList(
                Member.builder().id(1L).name("User1").build(),
                Member.builder().id(2L).name("Error").build(),  // 오류 유발
                Member.builder().id(3L).name("User3").build(),
                Member.builder().id(4L).name("Error").build(),  // 오류 유발
                Member.builder().id(5L).name("User5").build()
        ));
    }
    
    @Bean
    public ItemProcessor<Member, Member> skipMemberProcessor() {
        return member -> {
            if ("Error".equals(member.getName())) {
                throw new IllegalArgumentException(
                        "잘못된 회원 데이터: " + member.getId());
            }
            log.info(">>>>> Processing: {}", member.getName());
            return member;
        };
    }
    
    @Bean
    public ItemWriter<Member> skipMemberWriter() {
        return members -> {
            for (Member member : members) {
                log.info(">>>>> Writing: {}", member.getName());
            }
        };
    }
}
```

**MemberSkipListener.java**
```java
package com.batchflow.listener;

import com.batchflow.domain.entity.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;

@Slf4j
public class MemberSkipListener implements SkipListener<Member, Member> {
    
    @Override
    public void onSkipInRead(Throwable t) {
        log.error(">>>>> [Skip] Read 중 Skip 발생: {}", t.getMessage());
    }
    
    @Override
    public void onSkipInProcess(Member item, Throwable t) {
        log.error(">>>>> [Skip] Process 중 Skip 발생 - Member: {}, Error: {}", 
                item.getId(), t.getMessage());
        // TODO: Skip 건 별도 테이블 저장 또는 알림 발송
    }
    
    @Override
    public void onSkipInWrite(Member item, Throwable t) {
        log.error(">>>>> [Skip] Write 중 Skip 발생 - Member: {}, Error: {}", 
                item.getId(), t.getMessage());
    }
}
```

**Commit Message**: `feat: [Step 19] Skip 기능 구현 - 오류 건 건너뛰기`

### 4. 마크다운 문서화 요구사항

`docs/README_Step19.md` 작성:
- Skip 정책 설계 가이드
- skipLimit 초과 시 동작
- Skip된 데이터 후처리 방안

---

## [Step 20] Retry 처리 - 일시적 오류 재시도

### 1. 시나리오 및 요구사항

- **상황(Context)**: 외부 API 호출 시 일시적인 네트워크 오류가 발생합니다.

- **문제(Problem)**: 일시적 오류로 Skip 처리하면 정상 데이터도 누락됩니다.

- **미션(Mission)**: Retry 기능을 적용하여 일시적 오류 시 재시도하세요.

### 2. 학습 목표 (Key Concepts)

- `.retry()`, `.retryLimit()` 설정
- `RetryListener`를 통한 재시도 모니터링
- `BackOffPolicy`를 통한 재시도 간격 설정

### 3. 구현 가이드 (Technical Guide)

**RetryJobConfig.java**
```java
package com.batchflow.job.retry;

import com.batchflow.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RetryJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    private static final int CHUNK_SIZE = 5;
    
    // 재시도 시뮬레이션을 위한 카운터
    private final AtomicInteger attemptCount = new AtomicInteger(0);
    
    @Bean
    public Job retryJob() {
        return jobBuilderFactory.get("retryJob")
                .start(retryStep())
                .build();
    }
    
    @Bean
    public Step retryStep() {
        // BackOff 정책: 재시도 간격 설정
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(1000L);  // 1초 대기 후 재시도
        
        return stepBuilderFactory.get("retryStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(retryMemberReader())
                .processor(retryMemberProcessor())
                .writer(retryMemberWriter())
                
                // Retry 설정 활성화
                .faultTolerant()
                .retry(SocketTimeoutException.class)  // Retry 대상 예외
                .retryLimit(3)                         // 최대 재시도 횟수
                .backOffPolicy(backOffPolicy)          // 재시도 간격
                
                // 재시도 후에도 실패하면 Skip
                .skip(SocketTimeoutException.class)
                .skipLimit(2)
                .build();
    }
    
    @Bean
    public ItemReader<Member> retryMemberReader() {
        return new ListItemReader<>(Arrays.asList(
                Member.builder().id(1L).name("User1").build(),
                Member.builder().id(2L).name("NetworkIssue").build(),
                Member.builder().id(3L).name("User3").build()
        ));
    }
    
    @Bean
    public ItemProcessor<Member, Member> retryMemberProcessor() {
        return member -> {
            if ("NetworkIssue".equals(member.getName())) {
                int attempt = attemptCount.incrementAndGet();
                log.info(">>>>> 외부 API 호출 시도: {} ({}회차)", 
                        member.getName(), attempt);
                
                if (attempt < 3) {  // 3번째 시도에서 성공
                    throw new SocketTimeoutException("네트워크 타임아웃");
                }
                log.info(">>>>> 외부 API 호출 성공!");
            }
            return member;
        };
    }
    
    @Bean
    public ItemWriter<Member> retryMemberWriter() {
        return members -> {
            for (Member member : members) {
                log.info(">>>>> Writing: {}", member.getName());
            }
        };
    }
}
```

**Commit Message**: `feat: [Step 20] Retry 기능 구현 - 일시적 오류 재시도`

### 4. 마크다운 문서화 요구사항

`docs/README_Step20.md` 작성:
- Retry vs Skip 사용 기준
- BackOffPolicy 종류 (Fixed, Exponential)
- Retry + Skip 조합 전략

---

## [Step 21-22] Listener 구현

### Step 21: JobExecutionListener, StepExecutionListener

**커밋**: `feat: [Step 21] Job/Step Listener 구현 - 실행 전후 처리`

```java
package com.batchflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

@Slf4j
public class JobLoggerListener implements JobExecutionListener {
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("========================================");
        log.info("[JOB 시작] Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("[JOB 시작] Start Time: {}", jobExecution.getStartTime());
        log.info("========================================");
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        long duration = System.currentTimeMillis() - 
                jobExecution.getStartTime().getTime();
        
        log.info("========================================");
        log.info("[JOB 종료] Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("[JOB 종료] Status: {}", jobExecution.getStatus());
        log.info("[JOB 종료] Duration: {}ms", duration);
        log.info("========================================");
        
        // 실패 시 알림 발송
        if (jobExecution.getStatus().isUnsuccessful()) {
            log.error("[JOB 실패] 관리자 알림 발송 필요!");
            // TODO: 슬랙/이메일 알림 발송
        }
    }
}
```

### Step 22: ChunkListener, ItemReadListener, ItemProcessListener, ItemWriteListener

**커밋**: `feat: [Step 22] Chunk/Item Listener 구현 - 상세 처리 로깅`

---

## [Step 23-26] 안정성 강화

### Step 23: 트랜잭션 관리와 롤백 처리

**커밋**: `feat: [Step 23] 트랜잭션 관리 - 롤백 시나리오 처리`

### Step 24: 중복 실행 방지

**커밋**: `feat: [Step 24] JobParameters 기반 중복 실행 방지`

### Step 25: 재시작(Restart) 처리

**커밋**: `feat: [Step 25] 실패한 Job 재시작 - ExecutionContext 활용`

### Step 26: 안정성 테스트 및 문서화

**커밋**: `test: [Step 26] 오류 처리 통합 테스트 및 문서화`

---

# Phase 4: 성능 최적화 (Week 7-8)

---

## [Step 27] Multi-threaded Step

### 1. 시나리오 및 요구사항

- **상황(Context)**: 단일 스레드로 100만 건 처리 시 5시간이 소요됩니다.

- **문제(Problem)**: 배치 윈도우(야간 4시간) 내에 처리가 불가능합니다.

- **미션(Mission)**: Multi-threaded Step을 적용하여 처리 속도를 개선하세요.

### 2. 학습 목표 (Key Concepts)

- `TaskExecutor` 설정
- Thread-safe한 ItemReader 필요성 (`JdbcPagingItemReader`, `SynchronizedItemStreamReader`)
- 스레드 풀 크기 결정 기준

### 3. 구현 가이드 (Technical Guide)

**MultiThreadedStepJobConfig.java**
```java
package com.batchflow.job.parallel;

import com.batchflow.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MultiThreadedStepJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    
    private static final int CHUNK_SIZE = 1000;
    private static final int POOL_SIZE = 10;
    
    @Bean
    public Job multiThreadedJob() {
        return jobBuilderFactory.get("multiThreadedJob")
                .start(multiThreadedStep())
                .build();
    }
    
    @Bean
    public Step multiThreadedStep() {
        return stepBuilderFactory.get("multiThreadedStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(multiThreadedReader())
                .processor(multiThreadedProcessor())
                .writer(multiThreadedWriter())
                .taskExecutor(taskExecutor())  // 멀티스레드 실행
                .throttleLimit(POOL_SIZE)      // 동시 실행 스레드 수 제한
                .build();
    }
    
    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(POOL_SIZE);
        executor.setThreadNamePrefix("batch-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
    
    /**
     * JdbcPagingItemReader는 Thread-safe함
     * ⚠️ JpaPagingItemReader, JdbcCursorItemReader는 Thread-safe하지 않음!
     */
    @Bean
    public JdbcPagingItemReader<Member> multiThreadedReader() {
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);
        
        return new JdbcPagingItemReaderBuilder<Member>()
                .name("multiThreadedReader")
                .dataSource(dataSource)
                .selectClause("SELECT id, name, email, status")
                .fromClause("FROM member")
                .whereClause("WHERE status = 'ACTIVE'")
                .sortKeys(sortKeys)
                .pageSize(CHUNK_SIZE)
                .rowMapper((rs, rowNum) -> Member.builder()
                        .id(rs.getLong("id"))
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .build())
                // saveState=false: 멀티스레드에서 상태 저장 비활성화
                .saveState(false)
                .build();
    }
    
    @Bean
    public ItemProcessor<Member, Member> multiThreadedProcessor() {
        return member -> {
            log.info(">>>>> [{}] Processing: {}", 
                    Thread.currentThread().getName(), member.getName());
            Thread.sleep(10);  // 시뮬레이션
            return member;
        };
    }
    
    @Bean
    public ItemWriter<Member> multiThreadedWriter() {
        return members -> {
            log.info(">>>>> [{}] Writing {} items", 
                    Thread.currentThread().getName(), members.size());
        };
    }
}
```

**Multi-threaded Step 주의사항**
```
┌─────────────────────────────────────────────────────────────────┐
│             Multi-threaded Step 주의사항                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ⚠️ Thread-safe한 Reader만 사용 가능:                          │
│     ✅ JdbcPagingItemReader                                    │
│     ✅ SynchronizedItemStreamReader (래핑)                     │
│     ❌ JdbcCursorItemReader                                    │
│     ❌ JpaPagingItemReader                                     │
│                                                                 │
│  ⚠️ 실행 순서가 보장되지 않음:                                 │
│     • 처리 순서가 중요한 경우 사용 불가                        │
│     • 의존성 있는 데이터 처리 시 주의                          │
│                                                                 │
│  ⚠️ 재시작(Restart) 불가:                                      │
│     • saveState=false 설정 필수                                │
│     • ExecutionContext 저장 비활성화                           │
│                                                                 │
│  ✅ 적합한 경우:                                               │
│     • 각 아이템이 독립적일 때                                  │
│     • 순서가 중요하지 않을 때                                  │
│     • 처리 속도가 중요할 때                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Commit Message**: `feat: [Step 27] Multi-threaded Step 구현 - 병렬 처리`

### 4. 마크다운 문서화 요구사항

`docs/README_Step27.md` 작성:
- Multi-threaded Step 동작 원리
- Thread-safe ItemReader 목록
- 스레드 풀 크기 튜닝 가이드

---

## [Step 28] Parallel Steps - 동시 실행

### 1. 시나리오 및 요구사항

- **상황(Context)**: 서로 의존성이 없는 여러 Step을 순차 실행하고 있습니다.

- **문제(Problem)**: 독립적인 Step들을 순차 실행하면 불필요하게 시간이 오래 걸립니다.

- **미션(Mission)**: `Flow`를 사용하여 독립적인 Step들을 동시에 실행하세요.

### 2. 학습 목표 (Key Concepts)

- `Flow.split()` 사용법
- 병렬 Flow 설계
- 합류 지점(merge point) 설정

### 3. 구현 가이드 (Technical Guide)

**ParallelStepsJobConfig.java**
```java
package com.batchflow.job.parallel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ParallelStepsJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    
    @Bean
    public Job parallelStepsJob() {
        return jobBuilderFactory.get("parallelStepsJob")
                .start(splitFlow())           // 병렬 Flow 시작
                .next(finalStep())            // 병렬 완료 후 최종 Step
                .end()
                .build();
    }
    
    @Bean
    public Flow splitFlow() {
        return new FlowBuilder<SimpleFlow>("splitFlow")
                .split(parallelTaskExecutor())  // 병렬 실행
                .add(flow1(), flow2(), flow3()) // 동시 실행할 Flow들
                .build();
    }
    
    @Bean
    public TaskExecutor parallelTaskExecutor() {
        return new SimpleAsyncTaskExecutor("parallel-");
    }
    
    @Bean
    public Flow flow1() {
        return new FlowBuilder<SimpleFlow>("flow1")
                .start(step1())
                .build();
    }
    
    @Bean
    public Flow flow2() {
        return new FlowBuilder<SimpleFlow>("flow2")
                .start(step2())
                .build();
    }
    
    @Bean
    public Flow flow3() {
        return new FlowBuilder<SimpleFlow>("flow3")
                .start(step3())
                .build();
    }
    
    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [{}] Step1 실행 (3초)", 
                            Thread.currentThread().getName());
                    Thread.sleep(3000);
                    log.info(">>>>> [{}] Step1 완료", 
                            Thread.currentThread().getName());
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step step2() {
        return stepBuilderFactory.get("step2")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [{}] Step2 실행 (2초)", 
                            Thread.currentThread().getName());
                    Thread.sleep(2000);
                    log.info(">>>>> [{}] Step2 완료", 
                            Thread.currentThread().getName());
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step step3() {
        return stepBuilderFactory.get("step3")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> [{}] Step3 실행 (1초)", 
                            Thread.currentThread().getName());
                    Thread.sleep(1000);
                    log.info(">>>>> [{}] Step3 완료", 
                            Thread.currentThread().getName());
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
    
    @Bean
    public Step finalStep() {
        return stepBuilderFactory.get("finalStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info(">>>>> 모든 병렬 Step 완료 후 최종 처리");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

**실행 결과 예시**
```
[parallel-1] Step1 실행 (3초)
[parallel-2] Step2 실행 (2초)
[parallel-3] Step3 실행 (1초)
[parallel-3] Step3 완료      // 1초 후
[parallel-2] Step2 완료      // 2초 후
[parallel-1] Step1 완료      // 3초 후
[main] 모든 병렬 Step 완료 후 최종 처리

// 순차 실행 시: 6초
// 병렬 실행 시: 3초 (가장 오래 걸리는 Step 기준)
```

**Commit Message**: `feat: [Step 28] Parallel Steps 구현 - 독립 Step 동시 실행`

### 4. 마크다운 문서화 요구사항

`docs/README_Step28.md` 작성:
- Parallel Steps vs Multi-threaded Step 차이
- 병렬 실행 적합한 시나리오
- 병렬 Step 간 데이터 공유 방법

---

## [Step 29] Partitioning - 데이터 분할 병렬 처리

### 1. 시나리오 및 요구사항

- **상황(Context)**: 1000만 건의 회원 데이터를 처리해야 합니다. Multi-threaded Step만으로는 부족합니다.

- **문제(Problem)**: 하나의 Reader가 병목이 되어 성능 향상에 한계가 있습니다.

- **미션(Mission)**: Partitioning을 적용하여 데이터를 분할하고 각 파티션을 병렬로 처리하세요.

### 2. 학습 목표 (Key Concepts)

- `Partitioner` 인터페이스 구현
- Master-Slave 구조 이해
- `gridSize` 설정과 튜닝

### 3. 구현 가이드 (Technical Guide)

```
┌─────────────────────────────────────────────────────────────────┐
│                    Partitioning 아키텍처                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                    ┌─────────────┐                             │
│                    │  Master     │                             │
│                    │  Partitioner│                             │
│                    └──────┬──────┘                             │
│                           │                                     │
│         ┌─────────────────┼─────────────────┐                  │
│         ▼                 ▼                 ▼                  │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐          │
│  │  Slave 1    │   │  Slave 2    │   │  Slave 3    │          │
│  │  (1~100만)  │   │  (101~200만)│   │  (201~300만)│          │
│  └─────────────┘   └─────────────┘   └─────────────┘          │
│         │                 │                 │                  │
│         ▼                 ▼                 ▼                  │
│  ┌───────────┐     ┌───────────┐     ┌───────────┐            │
│  │  Reader   │     │  Reader   │     │  Reader   │            │
│  │  Processor│     │  Processor│     │  Processor│            │
│  │  Writer   │     │  Writer   │     │  Writer   │            │
│  └───────────┘     └───────────┘     └───────────┘            │
│                                                                 │
│  * 각 Slave는 독립적인 Reader/Processor/Writer 보유            │
│  * 데이터 범위가 겹치지 않으므로 Thread-safe 이슈 없음          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**MemberIdRangePartitioner.java**
```java
package com.batchflow.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class MemberIdRangePartitioner implements Partitioner {
    
    private final DataSource dataSource;
    
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        // 전체 데이터 범위 조회
        long minId = getMinId();
        long maxId = getMaxId();
        long targetSize = (maxId - minId) / gridSize + 1;
        
        log.info(">>>>> 파티셔닝: minId={}, maxId={}, gridSize={}, targetSize={}", 
                minId, maxId, gridSize, targetSize);
        
        long start = minId;
        long end = start + targetSize - 1;
        
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minId", start);
            context.putLong("maxId", Math.min(end, maxId));
            
            partitions.put("partition" + i, context);
            log.info(">>>>> partition{}: {} ~ {}", i, start, Math.min(end, maxId));
            
            start = end + 1;
            end = start + targetSize - 1;
        }
        
        return partitions;
    }
    
    private long getMinId() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT MIN(id) FROM member");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("minId 조회 실패", e);
        }
        return 1L;
    }
    
    private long getMaxId() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT MAX(id) FROM member");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.error("maxId 조회 실패", e);
        }
        return 1L;
    }
}
```

**PartitioningJobConfig.java**
```java
package com.batchflow.job.parallel;

import com.batchflow.domain.entity.Member;
import com.batchflow.partitioner.MemberIdRangePartitioner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PartitioningJobConfig {
    
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    
    private static final int CHUNK_SIZE = 1000;
    private static final int GRID_SIZE = 10;  // 파티션 수
    private static final int POOL_SIZE = 10;  // 스레드 수
    
    @Bean
    public Job partitioningJob() {
        return jobBuilderFactory.get("partitioningJob")
                .start(masterStep())
                .build();
    }
    
    /**
     * Master Step: Slave Step들을 관리
     */
    @Bean
    public Step masterStep() {
        return stepBuilderFactory.get("masterStep")
                .partitioner("slaveStep", partitioner())  // 파티셔너 지정
                .step(slaveStep())                        // Slave Step 지정
                .gridSize(GRID_SIZE)                      // 파티션 수
                .taskExecutor(partitionTaskExecutor())    // 병렬 실행
                .build();
    }
    
    @Bean
    public Partitioner partitioner() {
        return new MemberIdRangePartitioner(dataSource);
    }
    
    @Bean
    public TaskExecutor partitionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(POOL_SIZE);
        executor.setMaxPoolSize(POOL_SIZE);
        executor.setThreadNamePrefix("partition-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
    
    /**
     * Slave Step: 각 파티션 처리
     */
    @Bean
    public Step slaveStep() {
        return stepBuilderFactory.get("slaveStep")
                .<Member, Member>chunk(CHUNK_SIZE)
                .reader(partitionedReader(null, null))
                .processor(partitionedProcessor())
                .writer(partitionedWriter())
                .build();
    }
    
    @Bean
    @StepScope
    public JdbcPagingItemReader<Member> partitionedReader(
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId) {
        
        log.info(">>>>> Reader 생성: minId={}, maxId={}", minId, maxId);
        
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);
        
        Map<String, Object> params = new HashMap<>();
        params.put("minId", minId);
        params.put("maxId", maxId);
        
        return new JdbcPagingItemReaderBuilder<Member>()
                .name("partitionedReader")
                .dataSource(dataSource)
                .selectClause("SELECT id, name, email, status")
                .fromClause("FROM member")
                .whereClause("WHERE id >= :minId AND id <= :maxId")
                .sortKeys(sortKeys)
                .parameterValues(params)
                .pageSize(CHUNK_SIZE)
                .rowMapper((rs, rowNum) -> Member.builder()
                        .id(rs.getLong("id"))
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .build())
                .build();
    }
    
    @Bean
    @StepScope
    public ItemProcessor<Member, Member> partitionedProcessor() {
        return member -> {
            log.debug(">>>>> [{}] Processing: {}", 
                    Thread.currentThread().getName(), member.getId());
            return member;
        };
    }
    
    @Bean
    @StepScope
    public ItemWriter<Member> partitionedWriter() {
        return members -> {
            log.info(">>>>> [{}] Writing {} items", 
                    Thread.currentThread().getName(), members.size());
        };
    }
}
```

**Commit Message**: `feat: [Step 29] Partitioning 구현 - 데이터 분할 병렬 처리`

### 4. 마크다운 문서화 요구사항

`docs/README_Step29.md` 작성:
- Partitioning vs Multi-threaded Step 비교
- gridSize 결정 기준
- 파티션 키 선택 전략

---

## [Step 30-35] 성능 최적화 심화

### Step 30: AsyncItemProcessor/AsyncItemWriter

**커밋**: `feat: [Step 30] AsyncItemProcessor 구현 - 비동기 처리`

### Step 31: 성능 측정 및 병목 분석

**커밋**: `feat: [Step 31] 배치 성능 측정 - StepExecutionListener 활용`

### Step 32: DB 튜닝 - fetchSize, batchSize

**커밋**: `feat: [Step 32] DB 튜닝 - fetchSize/batchSize 최적화`

### Step 33: 메모리 최적화

**커밋**: `feat: [Step 33] 메모리 최적화 - 대용량 처리 OOM 방지`

### Step 34: 성능 비교 테스트

**커밋**: `test: [Step 34] 병렬 처리 성능 비교 테스트`

### Step 35: 성능 최적화 문서화

**커밋**: `docs: [Step 35] 성능 최적화 가이드 문서화`

---

# Phase 5: 운영과 모니터링 (Week 9-10)

---

## [Step 36] JobOperator - Job 제어

### 1. 시나리오 및 요구사항

- **상황(Context)**: 운영 중인 배치 Job을 외부에서 제어해야 합니다. 실행, 중지, 재시작 등의 기능이 필요합니다.

- **문제(Problem)**: `JobLauncher`만으로는 실행 중인 Job 제어가 어렵습니다.

- **미션(Mission)**: `JobOperator`를 활용하여 Job을 프로그래밍 방식으로 제어하세요.

### 2. 학습 목표 (Key Concepts)

- `JobOperator` vs `JobLauncher` 차이
- `JobExplorer`를 통한 Job 상태 조회
- `JobRegistry`를 통한 Job 목록 관리

### 3. 구현 가이드 (Technical Guide)

**BatchOperatorConfig.java**
```java
package com.batchflow.config;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchOperatorConfig {
    
    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(
            JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor postProcessor = 
                new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }
    
    @Bean
    public JobOperator jobOperator(
            JobLauncher jobLauncher,
            JobRepository jobRepository,
            JobExplorer jobExplorer,
            JobRegistry jobRegistry) {
        
        SimpleJobOperator jobOperator = new SimpleJobOperator();
        jobOperator.setJobLauncher(jobLauncher);
        jobOperator.setJobRepository(jobRepository);
        jobOperator.setJobExplorer(jobExplorer);
        jobOperator.setJobRegistry(jobRegistry);
        
        return jobOperator;
    }
}
```

**BatchJobController.java**
```java
package com.batchflow.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchJobController {
    
    private final JobOperator jobOperator;
    private final JobExplorer jobExplorer;
    
    /**
     * 등록된 Job 목록 조회
     */
    @GetMapping("/jobs")
    public Set<String> getJobNames() {
        return jobOperator.getJobNames();
    }
    
    /**
     * Job 실행
     */
    @PostMapping("/jobs/{jobName}/start")
    public Long startJob(@PathVariable String jobName,
                        @RequestParam(required = false) String requestDate) 
            throws Exception {
        
        String params = "requestDate=" + 
                (requestDate != null ? requestDate : System.currentTimeMillis());
        
        Long executionId = jobOperator.start(jobName, params);
        log.info(">>>>> Job 실행 시작: {} (executionId={})", jobName, executionId);
        
        return executionId;
    }
    
    /**
     * Job 중지
     */
    @PostMapping("/jobs/{executionId}/stop")
    public boolean stopJob(@PathVariable Long executionId) throws Exception {
        boolean stopped = jobOperator.stop(executionId);
        log.info(">>>>> Job 중지 요청: executionId={}, stopped={}", 
                executionId, stopped);
        return stopped;
    }
    
    /**
     * Job 재시작
     */
    @PostMapping("/jobs/{executionId}/restart")
    public Long restartJob(@PathVariable Long executionId) throws Exception {
        Long newExecutionId = jobOperator.restart(executionId);
        log.info(">>>>> Job 재시작: {} -> {}", executionId, newExecutionId);
        return newExecutionId;
    }
    
    /**
     * Job 실행 상태 조회
     */
    @GetMapping("/jobs/{jobName}/executions")
    public List<JobExecution> getJobExecutions(@PathVariable String jobName) {
        List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 10);
        
        return instances.stream()
                .flatMap(instance -> 
                        jobExplorer.getJobExecutions(instance).stream())
                .toList();
    }
    
    /**
     * 특정 실행 상세 조회
     */
    @GetMapping("/executions/{executionId}")
    public JobExecution getJobExecution(@PathVariable Long executionId) {
        return jobExplorer.getJobExecution(executionId);
    }
}
```

**Commit Message**: `feat: [Step 36] JobOperator를 활용한 Job 제어 API 구현`

### 4. 마크다운 문서화 요구사항

`docs/README_Step36.md` 작성:
- JobOperator, JobExplorer, JobRegistry 역할
- REST API를 통한 Job 제어 시나리오
- 운영 환경에서의 활용 사례

---

## [Step 37-42] 운영 모니터링

### Step 37: 배치 실행 이력 관리

**커밋**: `feat: [Step 37] 배치 실행 이력 조회 API 구현`

### Step 38: 알림 시스템 연동

**커밋**: `feat: [Step 38] 배치 실패 시 Slack/Email 알림 연동`

### Step 39: 스케줄러 연동 (Quartz/Spring Scheduler)

**커밋**: `feat: [Step 39] Spring Scheduler를 활용한 배치 스케줄링`

### Step 40: 모니터링 대시보드 연동

**커밋**: `feat: [Step 40] Spring Boot Admin/Actuator 연동`

### Step 41: 운영 환경 설정

**커밋**: `config: [Step 41] 운영 환경 설정 분리 (local/dev/prod)`

### Step 42: 운영 가이드 문서화

**커밋**: `docs: [Step 42] 배치 운영 가이드 문서화`

---

# Phase 6: 실전 프로젝트 (Week 11-12)

---

## [Step 43-46] 실전 예제 1: 일일 정산 Job

### 시나리오

매일 자정에 전일 거래 내역을 집계하여 정산 데이터를 생성합니다.

**커밋 시퀀스**:
- `feat: [Step 43] 일일 정산 Job - 거래 내역 조회 Reader`
- `feat: [Step 44] 일일 정산 Job - 정산 금액 계산 Processor`
- `feat: [Step 45] 일일 정산 Job - 정산 결과 저장 Writer`
- `test: [Step 46] 일일 정산 Job 통합 테스트`

---

## [Step 47-50] 실전 예제 2: 대량 알림 발송 Job

### 시나리오

회원 10만 명에게 개인화된 마케팅 이메일을 발송합니다.

**커밋 시퀀스**:
- `feat: [Step 47] 대량 알림 발송 Job - 대상자 조회`
- `feat: [Step 48] 대량 알림 발송 Job - 메시지 생성`
- `feat: [Step 49] 대량 알림 발송 Job - 외부 API 연동 (Partitioning)`
- `test: [Step 50] 대량 알림 발송 Job 통합 테스트`

---

# 부록

## A. 커밋 메시지 컨벤션

| 타입 | 용도 |
|------|------|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| test | 테스트 추가/수정 |
| refactor | 코드 리팩토링 |
| docs | 문서 작성/수정 |
| config | 설정 파일 변경 |
| style | 코드 포맷팅 |

**형식**
```
[type]: [Step N] [간단한 설명]

[상세 설명]

학습 포인트:
- [포인트 1]
- [포인트 2]
```

## B. 테스트 작성 가이드

### @SpringBatchTest 사용법

```java
@SpringBatchTest
@SpringBootTest(classes = {TargetJobConfig.class, TestBatchConfig.class})
class TargetJobConfigTest {
    
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    
    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    
    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
    }
    
    @Test
    void job_실행_성공() throws Exception {
        // given
        JobParameters params = new JobParametersBuilder()
                .addString("requestDate", "2025-01-01")
                .toJobParameters();
        
        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
        
        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
    
    @Test
    void step_단독_실행_테스트() {
        // given
        
        // when
        JobExecution jobExecution = 
                jobLauncherTestUtils.launchStep("targetStep");
        
        // then
        assertThat(jobExecution.getStepExecutions())
                .extracting("stepName")
                .containsExactly("targetStep");
    }
}
```

## C. 참고 자료

| 주제 | 링크 |
|------|------|
| Spring Batch 공식 문서 | https://docs.spring.io/spring-batch/docs/4.3.x/reference/html/ |
| Spring Boot 2.7 문서 | https://docs.spring.io/spring-boot/docs/2.7.x/reference/html/ |
| Baeldung Spring Batch | https://www.baeldung.com/spring-batch-guide |

---

## 마무리

이 커리큘럼을 완주하면 다음을 달성할 수 있습니다:

- ✅ Spring Batch 4.x의 핵심 개념 완전 이해
- ✅ Chunk 기반 대용량 데이터 처리 능력
- ✅ 오류 처리(Skip/Retry) 전략 수립 능력
- ✅ 병렬 처리를 통한 성능 최적화 경험
- ✅ 운영 환경에서의 Job 모니터링 및 제어 능력
- ✅ 테스트 주도 개발을 통한 안정적인 배치 시스템 구축

**"좋은 배치 시스템은 실패해도 복구할 수 있고, 느려도 개선할 수 있습니다. 하지만 잘못된 설계는 돌이킬 수 없습니다."**
