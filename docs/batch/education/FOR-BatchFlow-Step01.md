# FOR-BatchFlow-Step01: Spring Batch 프로젝트 초기화

> Spring Batch의 핵심 인프라를 구성하고, 배치 메타데이터 테이블을 생성합니다.

---

## 🎬 Before We Start

"안녕하세요, 신입 개발자님! 오늘부터 Spring Batch를 배워볼 건데요, 첫날부터 어려운 걸 하진 않을 거예요."

Spring Batch는 마치 **대형 요리 공장**을 운영하는 것과 같아요. 요리 공장을 차리려면 먼저 무엇이 필요할까요?

- 주방 설비 (냉장고, 오븐, 조리대)
- 재료 창고
- 작업 일지 (누가, 언제, 무엇을 만들었는지)

Spring Batch도 마찬가지입니다. 배치 작업을 실행하기 전에:
- **인프라 설정** (Spring Batch 컴포넌트)
- **데이터베이스** (작업 결과 저장)
- **메타데이터 테이블** (실행 기록 관리)

이게 준비되어야 본격적인 배치 작업을 할 수 있습니다.

---

## 🏗️ What We're Building

### Step 1에서 구현할 것

```
┌─────────────────────────────────────────────────────────────┐
│                 Spring Batch Infrastructure                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  [1] application.yml                                         │
│      ├─ Spring Batch 설정 (메타데이터 테이블 자동 생성)      │
│      ├─ H2 데이터베이스 설정 (메모리 DB)                     │
│      └─ H2 콘솔 활성화 (웹에서 DB 확인)                      │
│                                                              │
│  [2] BatchConfig.java                                        │
│      └─ @EnableBatchProcessing (핵심 Bean 자동 등록)        │
│                                                              │
│  [3] BatchFlowApplication.java                               │
│      └─ 메인 애플리케이션 진입점                             │
│                                                              │
│  [결과] Spring Batch 메타데이터 테이블 6개 생성              │
│      ├─ BATCH_JOB_INSTANCE                                   │
│      ├─ BATCH_JOB_EXECUTION                                  │
│      ├─ BATCH_JOB_EXECUTION_PARAMS                           │
│      ├─ BATCH_STEP_EXECUTION                                 │
│      ├─ BATCH_STEP_EXECUTION_CONTEXT                         │
│      └─ BATCH_JOB_EXECUTION_CONTEXT                          │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧠 Core Concepts

### 1. @EnableBatchProcessing이 하는 일

```java
@Configuration
@EnableBatchProcessing  // 이 어노테이션 하나가 마법을 부립니다!
public class BatchConfig {
}
```

**이 어노테이션이 자동으로 등록하는 Bean들:**

| Bean | 역할 | 비유 |
|------|------|------|
| **JobRepository** | Job 실행 정보 저장소 | 공장 작업일지 |
| **JobLauncher** | Job 실행 런처 | 공장 관리자 (작업 시작 버튼) |
| **JobBuilderFactory** | Job 생성 빌더 | 레시피 템플릿 |
| **StepBuilderFactory** | Step 생성 빌더 | 작업 단계 템플릿 |

**비유로 이해하기:**
- JobRepository = 공장의 **작업 일지**. "오늘 누가 무슨 요리를 만들었고, 성공했는지 실패했는지" 기록
- JobLauncher = **공장 관리자**. "자, 이제 김치찌개 만들기 시작!" 하고 작업 시작 버튼을 누르는 사람
- JobBuilderFactory = **레시피 템플릿**. 요리를 만들 때 사용하는 빈 레시피 양식
- StepBuilderFactory = **작업 단계 템플릿**. "1. 재료 손질, 2. 볶기, 3. 끓이기" 같은 단계별 작업 양식

### 2. Spring Batch 메타데이터 테이블

Spring Batch는 **실행 이력을 반드시 기록**합니다. 왜냐하면:
- 같은 작업을 두 번 실행하면 안 되는 경우가 많음 (예: 일일 정산)
- 실패한 작업을 이어서 실행할 수 있어야 함
- 누가, 언제, 무엇을, 얼마나 처리했는지 추적 필요

**6개의 메타데이터 테이블:**

```
┌────────────────────────────────────────────────────────────┐
│                    메타데이터 테이블 구조                     │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  [BATCH_JOB_INSTANCE]                                       │
│    Job 고유 식별 정보 (Job 이름 + Parameters)               │
│    ├─ JOB_INSTANCE_ID                                       │
│    ├─ JOB_NAME (예: "dormantMemberJob")                     │
│    └─ JOB_KEY (Parameters 해시값)                           │
│                                                             │
│  [BATCH_JOB_EXECUTION]                                      │
│    Job 실행 시도 기록 (성공/실패/진행중)                     │
│    ├─ JOB_EXECUTION_ID                                      │
│    ├─ STATUS (COMPLETED, FAILED, STARTED 등)                │
│    ├─ START_TIME, END_TIME                                  │
│    └─ EXIT_CODE, EXIT_MESSAGE                               │
│                                                             │
│  [BATCH_JOB_EXECUTION_PARAMS]                               │
│    Job 실행 시 전달된 파라미터                               │
│    ├─ PARAMETER_NAME (예: "requestDate")                    │
│    ├─ PARAMETER_VALUE (예: "2025-01-26")                    │
│    └─ PARAMETER_TYPE (STRING, LONG, DATE 등)                │
│                                                             │
│  [BATCH_STEP_EXECUTION]                                     │
│    Step 실행 기록 (읽기/쓰기 건수 포함)                      │
│    ├─ STEP_EXECUTION_ID                                     │
│    ├─ READ_COUNT, WRITE_COUNT (처리 건수)                   │
│    ├─ COMMIT_COUNT, ROLLBACK_COUNT                          │
│    └─ STATUS, EXIT_CODE                                     │
│                                                             │
│  [BATCH_STEP_EXECUTION_CONTEXT]                             │
│    Step 간 데이터 공유 (JSON 형태)                          │
│    └─ SERIALIZED_CONTEXT                                    │
│                                                             │
│  [BATCH_JOB_EXECUTION_CONTEXT]                              │
│    Job 전체에서 공유하는 데이터                              │
│    └─ SERIALIZED_CONTEXT                                    │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

### 3. initialize-schema: always의 의미

```yaml
spring:
  batch:
    jdbc:
      initialize-schema: always  # 이 설정의 의미는?
```

| 값 | 의미 | 사용 시기 |
|---|------|----------|
| **always** | 앱 시작 시 항상 테이블 생성 | 개발 환경, H2 메모리 DB |
| **never** | 테이블 생성 안 함 (수동 관리) | 운영 환경 |
| **embedded** | 내장 DB(H2, HSQL)일 때만 생성 | 기본값 |

**왜 always로 설정했나요?**
- H2는 **메모리 DB**라서 애플리케이션 종료 시 데이터가 사라집니다.
- 매번 다시 시작할 때마다 테이블을 새로 만들어야 해요.
- 운영 환경(MySQL, PostgreSQL)에서는 **never**로 설정하고 DBA가 직접 관리합니다.

### 4. job.enabled: false의 비밀

```yaml
spring:
  batch:
    job:
      enabled: false  # 자동 실행 비활성화
```

**기본값(true)이면 무슨 일이?**
- 애플리케이션 시작 시 **모든 Job이 자동 실행**됩니다.
- 여러 개의 Job이 있으면 전부 실행돼서 혼란 발생!

**false로 설정하면?**
- 수동으로 원하는 Job만 실행 가능
- 테스트 작성이 쉬워짐
- 실수로 운영 Job이 실행되는 것 방지

**실무 팁:**
- 개발 환경: `false` (테스트용)
- 운영 환경: `false` + 스케줄러(Cron) 또는 REST API로 실행

---

## 💻 Step-by-Step Implementation

### Step 1: application.yml 작성

**위치:** `spring-batch-onboarding/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: batch-flow

  # Spring Batch 설정
  batch:
    jdbc:
      initialize-schema: always  # BATCH_* 메타데이터 테이블 자동 생성
    job:
      enabled: false  # 자동 실행 비활성화

  # H2 데이터베이스 설정
  datasource:
    url: jdbc:h2:mem:batchdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:

  # JPA 설정
  jpa:
    hibernate:
      ddl-auto: create  # 애플리케이션 시작 시 테이블 자동 생성
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  # H2 콘솔 설정
  h2:
    console:
      enabled: true  # H2 웹 콘솔 활성화
      path: /h2-console

# 로깅 설정
logging:
  level:
    com.batchflow: DEBUG
    org.springframework.batch: INFO
```

**주요 설정 설명:**
- `jdbc:h2:mem:batchdb`: 메모리 기반 H2 데이터베이스
- `MODE=MySQL`: MySQL 호환 모드 (실무와 유사한 환경)
- `DB_CLOSE_DELAY=-1`: JVM 종료 전까지 DB 유지
- `show-sql: true`: 실행되는 SQL 로그 출력 (학습용)

### Step 2: BatchConfig.java 작성

**위치:** `spring-batch-onboarding/src/main/java/com/batchflow/config/BatchConfig.java`

```java
package com.batchflow.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 기본 설정
 *
 * @EnableBatchProcessing
 * - Spring Batch의 핵심 인프라 Bean들을 자동으로 등록합니다.
 * - JobRepository: Job 실행 정보를 저장하는 저장소
 * - JobLauncher: Job을 실행하는 런처
 * - JobBuilderFactory: Job을 생성하는 빌더 팩토리
 * - StepBuilderFactory: Step을 생성하는 빌더 팩토리
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    // 기본 설정은 @EnableBatchProcessing이 자동으로 처리
    // 필요에 따라 커스텀 설정 추가 가능 (TaskExecutor, DataSource 등)
}
```

**왜 이렇게 간단한가요?**
- `@EnableBatchProcessing`이 거의 모든 걸 자동으로 해줍니다.
- 나중에 성능 튜닝이 필요하면 여기에 TaskExecutor, DataSource 등을 추가합니다.

### Step 3: BatchFlowApplication.java 패키지 정리

**위치:** `spring-batch-onboarding/src/main/java/com/batchflow/BatchFlowApplication.java`

```java
package com.batchflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BatchFlow 온보딩 프로젝트 메인 애플리케이션
 *
 * Spring Batch를 학습하기 위한 온보딩 프로젝트입니다.
 * 50개의 Step을 통해 Batch 처리의 기초부터 실전까지 학습합니다.
 */
@SpringBootApplication
public class BatchFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchFlowApplication.class, args);
    }
}
```

**패키지 구조:**
```
com.batchflow/
├── BatchFlowApplication.java    (메인)
└── config/
    └── BatchConfig.java          (설정)
```

---

## 🧪 Testing

### 테스트 방법 1: 애플리케이션 실행

**실행 명령:**
```bash
# 프로젝트 루트에서
cd spring-batch-onboarding
../gradlew bootRun

# 또는 IDE에서 BatchFlowApplication.java 실행
```

**성공 로그 예시:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::       (v2.7.17)

2025-01-26 23:45:12.123  INFO 12345 --- [main] c.b.BatchFlowApplication : Starting BatchFlowApplication
2025-01-26 23:45:13.456  INFO 12345 --- [main] o.s.b.c.config.JobRegistryBeanPostProcessor : No job beans found
2025-01-26 23:45:14.789  INFO 12345 --- [main] c.b.BatchFlowApplication : Started BatchFlowApplication in 3.5 seconds
```

**주목할 로그:**
- `No job beans found`: 아직 Job을 만들지 않아서 정상입니다!
- `Started BatchFlowApplication`: 정상 기동 완료

### 테스트 방법 2: H2 콘솔에서 테이블 확인

**1. 웹 브라우저에서 H2 콘솔 접속:**
```
http://localhost:8080/h2-console
```

**2. 접속 정보 입력:**
- JDBC URL: `jdbc:h2:mem:batchdb`
- User Name: `sa`
- Password: (비워둠)

**3. Connect 버튼 클릭**

**4. 메타데이터 테이블 확인 SQL:**
```sql
-- 1. 테이블 목록 조회
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'PUBLIC'
  AND TABLE_NAME LIKE 'BATCH%'
ORDER BY TABLE_NAME;

-- 예상 결과: 6개 테이블
-- BATCH_JOB_EXECUTION
-- BATCH_JOB_EXECUTION_CONTEXT
-- BATCH_JOB_EXECUTION_PARAMS
-- BATCH_JOB_INSTANCE
-- BATCH_STEP_EXECUTION
-- BATCH_STEP_EXECUTION_CONTEXT
```

**5. 각 테이블 구조 확인:**
```sql
-- BATCH_JOB_INSTANCE 구조
SELECT * FROM BATCH_JOB_INSTANCE LIMIT 0;

-- BATCH_JOB_EXECUTION 구조
SELECT * FROM BATCH_JOB_EXECUTION LIMIT 0;

-- BATCH_STEP_EXECUTION 구조
SELECT * FROM BATCH_STEP_EXECUTION LIMIT 0;
```

**현재는 데이터가 없어야 정상입니다!**
- 아직 Job을 실행하지 않았으니까요.
- Step 2에서 첫 Job을 만들면 데이터가 생깁니다.

### 테스트 방법 3: Bean 등록 확인 (선택)

**Bean 확인용 테스트 코드:**

**위치:** `spring-batch-onboarding/src/test/java/com/batchflow/config/BatchConfigTest.java`

```java
package com.batchflow.config;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BatchConfigTest {

    @Autowired(required = false)
    private JobRepository jobRepository;

    @Autowired(required = false)
    private JobLauncher jobLauncher;

    @Autowired(required = false)
    private JobBuilderFactory jobBuilderFactory;

    @Autowired(required = false)
    private StepBuilderFactory stepBuilderFactory;

    @Test
    void BatchConfig_EnableBatchProcessing_핵심Bean등록확인() {
        // then - @EnableBatchProcessing이 자동으로 등록한 Bean들 확인
        assertThat(jobRepository).isNotNull();
        assertThat(jobLauncher).isNotNull();
        assertThat(jobBuilderFactory).isNotNull();
        assertThat(stepBuilderFactory).isNotNull();
    }
}
```

**테스트 실행:**
```bash
../gradlew test --tests BatchConfigTest
```

**성공 시 출력:**
```
BatchConfigTest > BatchConfig_EnableBatchProcessing_핵심Bean등록확인() PASSED
```

---

## 🐛 Lessons Learned

### 버그 1: H2 콘솔 접속 안 됨

**증상:**
```
H2 콘솔에서 Connect 시 "Database not found" 에러
```

**원인:**
- JDBC URL을 잘못 입력 (`jdbc:h2:mem:testdb` 대신 `jdbc:h2:mem:batchdb`)
- 또는 애플리케이션이 실행 중이지 않음

**해결:**
1. JDBC URL을 정확히 확인: `jdbc:h2:mem:batchdb`
2. 애플리케이션이 실행 중인지 확인
3. `DB_CLOSE_DELAY=-1` 설정 확인 (없으면 연결이 즉시 끊김)

**교훈:**
- H2 메모리 DB는 **애플리케이션과 생명주기를 같이합니다**.
- 앱이 꺼지면 DB도 사라집니다.

### 버그 2: 메타데이터 테이블이 안 생김

**증상:**
```sql
SELECT * FROM BATCH_JOB_INSTANCE;
-- Table "BATCH_JOB_INSTANCE" not found
```

**원인:**
- `spring.batch.jdbc.initialize-schema`가 `never`로 설정됨
- 또는 DataSource 설정이 잘못됨

**해결:**
```yaml
spring:
  batch:
    jdbc:
      initialize-schema: always  # 이 설정 확인!
```

**교훈:**
- 개발 환경에서는 `always`로 설정해서 자동 생성
- 운영 환경에서는 `never`로 설정하고 DBA가 수동으로 테이블 생성

### 버그 3: 애플리케이션 시작 시 Job이 자동 실행됨

**증상:**
```
애플리케이션 시작하자마자 모든 Job이 실행되어 혼란
```

**원인:**
- `spring.batch.job.enabled: true` (기본값)

**해결:**
```yaml
spring:
  batch:
    job:
      enabled: false  # 자동 실행 비활성화
```

**교훈:**
- 개발/테스트 환경에서는 **반드시 false**로 설정
- 운영 환경도 false로 설정하고, 스케줄러나 API로 실행

### 시니어 개발자의 사고방식

**"왜 H2 메모리 DB를 사용하나요? MySQL을 쓰면 안 되나요?"**

좋은 질문입니다! 각각의 장단점이 있어요:

| 환경 | DB | 장점 | 단점 |
|------|-----|------|------|
| **학습/개발** | H2 메모리 | 설치 불필요, 빠름, 초기화 쉬움 | 재시작 시 데이터 소실 |
| **통합 테스트** | H2 파일 | 데이터 유지, 빠름 | 방언(Dialect) 차이 |
| **운영** | MySQL/PostgreSQL | 안정성, 확장성, 실제 환경 | 설치/관리 필요 |

**실무 팁:**
- Step 1-20: H2 메모리 (빠른 학습)
- Step 21-50: MySQL 추가 (실전 준비)
- 운영: 절대 H2 사용 금지!

---

## 🎯 Key Takeaways

### 1. @EnableBatchProcessing은 Spring Batch의 "마법 주문"
- 이 어노테이션 하나로 핵심 인프라 Bean이 모두 등록됩니다.
- JobRepository, JobLauncher, JobBuilderFactory, StepBuilderFactory

### 2. Spring Batch는 실행 이력을 반드시 기록합니다
- 6개의 메타데이터 테이블에 모든 실행 정보 저장
- 같은 Job+Parameters 조합은 한 번만 성공 가능 (재실행 방지)

### 3. initialize-schema: always는 개발 환경 전용
- H2 메모리 DB에서는 always 사용
- 운영 환경(MySQL, PostgreSQL)에서는 never + 수동 관리

### 4. job.enabled: false로 설정하는 습관
- 자동 실행 방지로 예상치 못한 Job 실행 차단
- 테스트 작성과 디버깅이 훨씬 쉬워짐

### 5. H2 콘솔은 강력한 학습 도구
- 웹에서 바로 SQL 실행 가능
- 메타데이터 테이블을 직접 조회하며 Spring Batch 내부 동작 이해

---

## ✅ 기능 확인 체크리스트

완료했으면 체크하세요!

- [ ] 애플리케이션이 정상 실행됨 (`Started BatchFlowApplication` 로그 확인)
- [ ] H2 콘솔 접속 성공 (`http://localhost:8080/h2-console`)
- [ ] 6개의 BATCH_* 테이블 생성 확인
- [ ] BatchConfigTest 통과 (핵심 Bean 등록 확인)
- [ ] 로그에 "No job beans found" 메시지 (정상, 아직 Job 없음)

---

## 🔗 Next Steps

**Step 2 예고: Hello World Job 만들기**

이제 기본 인프라가 준비되었으니, 드디어 첫 번째 Job을 만들어볼 거예요!

Step 2에서는:
- Job과 Step의 개념 이해
- Tasklet을 사용한 간단한 작업 실행
- JobLauncher로 수동 실행
- 메타데이터 테이블에 실행 기록이 남는 것 확인

"Hello, Spring Batch!"를 출력하는 간단한 Job이지만, Spring Batch의 핵심 개념을 모두 담고 있습니다.

다음 Step에서 만나요! 🚀
