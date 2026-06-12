# BatchFlow - Spring Batch 온보딩 모듈

> ⚠️ 이 파일은 `spring-batch-onboarding` 모듈 전용 규칙입니다.
> 공통 규칙은 **루트 `CLAUDE.md`** 를 참조하세요.

---

## 🎯 모듈 개요

Spring Batch 미경험자가 **약 2주(하루 1~2시간, 자기주도)** 안에 실무 배치의 기본기
(Job 설계 → Chunk → 오류 제어 → 재시작)를 갖추게 하는 학습 모듈입니다.

- **학습 기준 커리큘럼**: `docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md` (필수 Step 1~13 + 심화 14~18)
- 기존 50-Step 문서(`00-BatchFlow-Curriculum.md`)는 **심화/전체 참조 자료** (필수 트랙과의 매핑표는 01 문서)
- 학습 철학: 문제 주도형 — 각 Step은 앞 단계의 한계에서 출발 (TestCraft와 동일)

---

## 🔒 기술 스택 (변경 불가)

```
Java:           1.8
Spring Boot:    2.7.17
Spring Batch:   4.3.x (Boot 2.7 내장 — 5.x 스타일 금지!)
H2:             Boot BOM 관리, 인메모리 + MODE=MSSQLServer (실무 DB가 MS-SQL)
JUnit:          5.x + spring-batch-test (@SpringBatchTest)
Lombok:         루트 build.gradle이 제공
Integration:    spring-batch-integration (심화 Step 16 — AsyncItemProcessor/Writer의 출처, Boot BOM 관리)
JPA:            ❌ 사용 금지 (JDBC 기반 — JdbcCursor/PagingItemReader, JdbcBatchItemWriter)
```

DB 정책: `jdbc:h2:mem:batchdb;MODE=MSSQLServer;...` — SQL은 MS-SQL 방언
(`mybatis-mssql` 스킬의 방언 규칙 준수: OFFSET/FETCH, IDENTITY, GETDATE, NVARCHAR).

---

## 🏗️ 디렉토리 구조

```
src/main/java/com/batchflow/
├── BatchFlowApplication.java
├── config/BatchConfig.java          # @EnableBatchProcessing
├── job/                             # Job 정의 (도메인별 하위 패키지)
│   ├── hello/                       # Step 2
│   ├── dormant/                     # Step 7~12 (휴면회원 전환)
│   ├── settlement/                  # Step 13 캡스톤 (일일 정산)
│   ├── async/                       # 심화 Step 16 (비동기 알림 — spring-batch-integration)
│   ├── ops/                         # 심화 Step 17 (JobOperator/JobRegistry)
│   └── massnotify/                  # 심화 Step 18 제2 캡스톤 (대량 알림 — 파티셔닝+skip+멱등)
├── domain/                          # 순수 자바 도메인 (JPA 엔티티 아님!)
├── listener/                        # Job/Step/Skip 리스너
└── (reader/processor/writer는 JobConfig 내 @Bean 우선, 복잡해지면 분리)

src/main/resources/
├── application.yml                  # H2 MSSQLServer, batch.job.enabled=false
├── schema.sql / data.sql            # 도메인 테이블 + 소규모 시드 (Step 6~)

src/test/java/com/batchflow/
├── config/TestBatchConfig.java      # @EnableBatchProcessing + @EnableAutoConfiguration
├── step01/ ~ step13/                # 필수 트랙
│   ├── example/   # 완성 모범 — 항상 통과
│   ├── exercise/  # @Disabled TODO 골격 — 컴파일 항상 성공
│   └── answer/    # 모범답안 — 항상 통과
└── advanced/step14/ ~ step18/       # 심화 (14 멀티스레드/병렬, 15 파티셔닝, 16 비동기, 17 JobOperator, 18 캡스톤2)
```

example/exercise/answer 3종 규약은 TestCraft와 동일
(클래스명 `{대상}Test` / `{대상}ExerciseTest` / `{대상}AnswerTest`, exercise는 클래스 레벨 @Disabled).

---

## 📜 Spring Batch 4.x 필수 스타일

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class XxxJobConfig {

    private final JobBuilderFactory jobBuilderFactory;   // ✅ 4.x Factory 주입
    private final StepBuilderFactory stepBuilderFactory;

    private static final String JOB_NAME = "xxxJob";     // ✅ 이름은 상수
    private static final int CHUNK_SIZE = 10;            // ✅ chunk size는 상수 (하드코딩 금지)
}
```

### ⛔ 금지 (NEVER)

```java
// ❌ Spring Batch 5.x 스타일 — 인터넷 최신 예제 주의!
@Bean
public Job myJob(JobRepository jobRepository, ...) { }
new StepBuilder("step", jobRepository)...

// ❌ JPA 엔티티/리포지토리 (이 모듈은 JDBC 기반)
// ❌ 하드코딩 chunk size: .chunk(500)
// ❌ 테스트에서 Job 직접 @Autowired (JobLauncherTestUtils 사용)
```

### Bean/클래스 네이밍

| 유형 | 패턴 | 예시 |
|------|------|------|
| Job Config | `{도메인}{기능}JobConfig` | `DormantMemberJobConfig` |
| Job/Step Bean | camelCase + Job/Step | `dormantMemberJob()`, `dormantMemberStep()` |
| Reader/Processor/Writer Bean | `{도메인}{역할}` | `dormantMemberReader()` |
| Listener | `{레벨}{목적}Listener` | `JobDurationListener` |

---

## 🧪 테스트 규칙 (필수 패턴)

```java
@SpringBatchTest
@SpringBootTest(classes = {XxxJobConfig.class, TestBatchConfig.class})
class XxxJobConfigTest {

    @Autowired JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions(); // 메타데이터 격리 — 필수!
    }
}
```

- 필요한 JobConfig만 classes에 명시 (전체 컨텍스트 금지 — 속도)
- JobParameters가 필요한 Job은 `new JobParametersBuilder()...` + `launchJob(params)`
- 검증은 BatchStatus/ExitStatus + **StepExecution 카운트**(read/write/filter/skip)까지
- 메서드명 `{대상}_{시나리오}_{예상결과}` 한글 허용, AssertJ 전용, AAA 주석 (TestCraft 규약)

### application.yml 필수 설정

```yaml
spring:
  batch:
    job:
      enabled: false        # Job 자동 실행 금지 (필수!)
    jdbc:
      initialize-schema: embedded  # BATCH_* 메타테이블 자동 생성
```

---

## 📋 계획/태스크 운영 규칙 (MUST)

1. **작업 시작 전**: `docs/batch/plan/plan.md`에 계획 추가, `docs/batch/plan/task.md`에 태스크 `[ ]` 등록
2. **커밋 시**: task.md 체크 `[x]` + 커밋 해시 병기
3. 절차 상세: `step-commit` 스킬

## 📊 커밋 규칙

```
✨ feat: [Batch/Step N] 제목

상세 설명

학습 포인트:
- 포인트 1
```

- Step 단위 커밋 = 학습 단위 (프로덕션 + 테스트 3종 + 교육 문서 동시)
- 매 커밋 전 `.\gradlew :spring-batch-onboarding:test` 그린

## 📚 교육 문서 규칙

- 파일: `docs/batch/education/FOR-BatchFlow-StepNN.md` (필수 트랙 번호 기준)
- 템플릿: `education-doc` 스킬 (Before We Start → … → Next Steps), 문제 주도형 연결
- Step 추가/수정 시 01 커리큘럼 문서의 표와 매핑을 함께 갱신

## 📚 참조 문서

| 문서 | 경로 |
|------|------|
| **필수 트랙 커리큘럼 (학습 기준)** | `docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md` |
| 전체 50-Step (심화 참조) | `docs/batch/curriculum/00-BatchFlow-Curriculum.md` |
| 배치 테스트 패턴 | `docs/batch/skills/spring-batch-testing.md` |
| 배치 핵심 개념 | `docs/batch/skills/spring-batch-core.md` |
| 대량 데이터 스키마 (심화) | `docs/batch/sql/Database-Schema-And-Data.md` |
