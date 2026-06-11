# [Batch Step 1] 배치 인프라 — 판 깔기

> **소요 시간**: 약 1시간
> **이번 Step의 도구**: `@EnableBatchProcessing`, BATCH_* 메타데이터 테이블 6종, H2(MODE=MSSQLServer), `INFORMATION_SCHEMA`로 스키마 검증
> **코드 위치**: `spring-batch-onboarding/src/test/java/com/batchflow/step01/`

---

## 1. Before We Start — 기록 없는 배치는 신뢰할 수 없다

새벽 3시에 도는 정산 배치를 상상해봅시다. 아침에 출근해서 받는 질문들:

- "어제 배치 돌았어요? **언제, 어떤 파라미터로?**"
- "실패했다는데 **몇 번째 Step에서, 몇 건 처리하고** 죽었어요?"
- "다시 돌리면 **이미 처리된 5만 건은** 어떻게 돼요?"

이 질문에 답하지 못하는 배치는 운영할 수 없습니다. Spring Batch의 첫 번째 가치는
화려한 기능이 아니라 **모든 실행을 장부에 남긴다**는 것입니다 — 항공기의 블랙박스처럼,
실행의 모든 순간이 기록되어야 사고 분석도 재시작도 가능합니다.

`@EnableBatchProcessing` 한 줄이 그 장부 시스템 전체를 깔아줍니다.

## 2. What We're Building

이번 Step은 코드를 "만들기"보다 **깔린 판을 확인하고 검증**합니다.

```
@EnableBatchProcessing
        │
        ├── 핵심 Bean 4종 ──────────── JobRepository  (장부 저장소)
        │                              JobLauncher    (실행 진입점)
        │                              JobBuilderFactory / StepBuilderFactory (조립 공장, 4.x)
        │
        └── 메타데이터 테이블 6종 ───── BATCH_JOB_INSTANCE / JOB_EXECUTION / JOB_EXECUTION_PARAMS
            (initialize-schema)        BATCH_STEP_EXECUTION / JOB·STEP_EXECUTION_CONTEXT
```

```
src/test/java/com/batchflow/step01/
├── example/BatchInfrastructureTest.java   ← 빈 4종 + 테이블 6종 + 통계 컬럼 검증
├── exercise/MetadataSchemaExerciseTest.java
└── answer/MetadataSchemaAnswerTest.java
```

### 이번에 바뀐 인프라 (중요!)

| 항목 | 이전 | 현재 | 이유 |
|------|------|------|------|
| DB | H2 **파일**(~/batchdb) + MODE=**MySQL** | H2 **인메모리** + MODE=**MSSQLServer** | 실무 DB(MS-SQL) 방언 정합 + 테스트 격리(매 실행 깨끗한 상태) |
| ORM | JPA(ddl-auto) | **JDBC만** (JPA 제거) | 배치 필수 트랙은 Jdbc Reader/Writer 중심 — 학습 표면 축소 |

## 3. Core Concepts

### 3-1. 메타데이터 테이블 6종 — 장부의 구조

| 테이블 | 기록하는 것 | 비유 |
|--------|------------|------|
| BATCH_JOB_INSTANCE | "어떤 Job + 어떤 파라미터"라는 논리적 단위 | 항공편 (KE123, 6/11편) |
| BATCH_JOB_EXECUTION | 그 인스턴스의 실행 시도 (1:N) | 그 항공편의 운항 시도 (결항 후 재운항 포함) |
| BATCH_JOB_EXECUTION_PARAMS | 실행 파라미터 | 운항 조건 |
| BATCH_STEP_EXECUTION | Step별 실행 통계 (READ/WRITE/SKIP/COMMIT...) | 구간별 비행 기록 |
| BATCH_JOB/STEP_EXECUTION_CONTEXT | 실행 중 상태 보관함 | 블랙박스 스냅샷 |

**READ_COUNT, WRITE_COUNT, FILTER_COUNT, SKIP 계열, ROLLBACK_COUNT** —
이 컬럼들은 이후 모든 Step에서 "몇 건 읽고 걸러내고 썼는지"를 검증하는 무기가 됩니다
(example의 두 번째 테스트가 이 컬럼들의 존재를 봉인해 둡니다).

### 3-2. JobInstance vs JobExecution — 미리 심는 복선

`BATCH_JOB_INSTANCE`에는 `JOB_KEY`라는 컬럼이 있습니다(exercise에서 직접 확인).
**파라미터의 해시값**인데, 이것이 Step 3에서 "같은 파라미터로는 성공한 Job을
다시 못 돌린다"는 사건의 범인입니다. 지금은 컬럼의 존재만 기억해두세요.

### 3-3. 스키마도 테스트로 검증한다

```java
List<String> tables = jdbcTemplate.queryForList(
        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE 'BATCH_%'", ...);
assertThat(tables).contains("BATCH_JOB_INSTANCE", ...);
```

"메타테이블이 자동 생성된다"는 설정(`initialize-schema: embedded`)의 약속을
**테스트가 봉인**합니다. 누군가 설정을 지우면 이 테스트가 먼저 깨집니다 —
TestCraft에서 배운 "설정 한 줄의 회귀를 테스트로 막는다"의 배치 버전입니다.

### 3-4. 왜 `spring.batch.job.enabled: false`인가

기본값(true)이면 **애플리케이션이 뜰 때 등록된 모든 Job이 자동 실행**됩니다.
테스트 컨텍스트가 뜰 때마다 Job이 제멋대로 돌아가는 대참사 — 그래서 이 모듈은
끄고, 실행은 항상 테스트(JobLauncherTestUtils)나 명시적 호출로만 합니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step01.*"
```

1. `application.yml`을 열어 datasource(MSSQLServer 모드)와 batch 설정 2종을 확인
2. `BatchInfrastructureTest` — 빈 4종 → 테이블 6종 → 통계 컬럼 순으로 읽기
3. **일부러 깨뜨려보기**: yml에서 `initialize-schema: embedded`를 `never`로 바꾸고 실행 —
   어떤 테스트가 어떻게 깨지나요? (확인 후 원복)

## 5. Testing — exercise 풀기

`step01/exercise/MetadataSchemaExerciseTest.java`의 TODO 1~3을 채우세요.
JOB_KEY 컬럼을 직접 확인하는 것이 포인트 — Step 3의 복선입니다.

## 6. Lessons Learned

### 사례 1: 파일 DB(~/batchdb)의 함정 (이번 전환의 이유)

- **증상**: 테스트가 "내 PC에서만" 실패한다 — 동료 PC에서는 통과
- **원인**: H2 파일 DB는 이전 실행의 메타데이터가 홈 디렉토리에 **누적**된다.
  지난주에 돌린 Job 기록이 오늘 테스트의 JobInstance 충돌을 일으킨다
- **해결**: 인메모리(`mem:batchdb`) 전환 — 매 실행이 깨끗한 상태에서 시작
- **교훈**: 테스트의 반복 가능성(F.I.R.S.T의 R)은 저장소 선택에서 시작된다.

### 사례 2: MODE=MySQL인데 실무는 MS-SQL

- **증상**: H2(MySQL 모드)에서 통과한 SQL이 실서버(MS-SQL)에서 문법 오류 (LIMIT 등)
- **해결**: MODE=MSSQLServer로 통일 + MS-SQL 방언 규칙(`mybatis-mssql` 스킬) 적용
- **교훈**: 호환 모드는 "실무와 같은 방언"일 때만 학습 전이가 생긴다.

### 시니어의 시선

> 배치 면접에서 제가 꼭 묻는 질문: "BATCH_JOB_INSTANCE와 BATCH_JOB_EXECUTION의
> 차이가 뭔가요?" — 이 차이를 아는 사람은 재시작과 중복 방지를 설계할 수 있고,
> 모르는 사람은 메타테이블을 "지워도 되는 로그"로 취급하다 사고를 냅니다.
> 운영 중인 메타테이블을 함부로 TRUNCATE하지 마세요. 그건 장부 소각입니다.

## 7. Key Takeaways

- @EnableBatchProcessing = 핵심 Bean 4종 + 메타데이터 장부 6종
- 배치의 신뢰성(재시작/중복방지/추적)은 전부 메타데이터 위에서 동작한다
- `job.enabled: false` — Job 실행은 항상 명시적으로
- 스키마/설정의 약속도 테스트로 봉인한다 (INFORMATION_SCHEMA)
- JOB_KEY(파라미터 해시)를 기억하라 — Step 3의 복선

## 8. Next Steps — 다음 Step의 문제

판은 깔렸습니다. 그런데 정작 **Job이 하나도 없습니다.**
가장 작은 배치 — Job 하나, Step 하나, "Hello"를 찍는 Tasklet 하나 — 를 만들고
테스트로 실행해봅니다. 그리고 그 한 번의 실행이 방금 본 장부에
**어떤 기록을 남기는지** 직접 확인합니다. → **Step 2**
