# Spring Batch Testing Skills
## @SpringBatchTest를 활용한 배치 테스트 패턴

---

## 🎯 이 스킬은 언제 사용하나요?

- 모든 Step에서 테스트 코드 작성 시
- Job/Step 레벨 테스트 시
- 통합 테스트 작성 시
- 테스트 데이터 준비 및 검증 시

---

## 📚 핵심 개념

### 1. @SpringBatchTest가 제공하는 것

```java
@SpringBatchTest  // 이 어노테이션이 자동으로 등록해주는 Bean들
class MyTest {
    
    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;      // Job 실행 유틸
    
    @Autowired
    JobRepositoryTestUtils jobRepositoryTestUtils;  // 메타데이터 정리 유틸
    
    // 내부적으로 사용
    // - StepScopeTestExecutionListener
    // - JobScopeTestExecutionListener
}
```

### 2. 테스트 구조

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Batch 테스트 구조                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  @SpringBatchTest                                               │
│  @SpringBootTest(classes = {TargetJobConfig.class,              │
│                             TestBatchConfig.class})              │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    테스트 클래스                          │   │
│  │                                                          │   │
│  │  @BeforeEach                                             │   │
│  │  └── jobRepositoryTestUtils.removeJobExecutions()       │   │
│  │      (이전 실행 기록 제거)                                │   │
│  │                                                          │   │
│  │  @Test                                                   │   │
│  │  └── Job 테스트                                          │   │
│  │      ├── JobParameters 생성                              │   │
│  │      ├── jobLauncherTestUtils.launchJob(params)          │   │
│  │      └── JobExecution 검증                               │   │
│  │                                                          │   │
│  │  @Test                                                   │   │
│  │  └── Step 테스트                                         │   │
│  │      ├── jobLauncherTestUtils.launchStep("stepName")     │   │
│  │      └── StepExecution 검증                              │   │
│  │                                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 코드 패턴

### 패턴 1: 기본 테스트 설정

```java
// TestBatchConfig.java - 테스트 전용 배치 설정
@Configuration
@EnableBatchProcessing
public class TestBatchConfig {
    // 테스트 시 필요한 추가 Bean 정의
}
```

```java
// 테스트 클래스 기본 구조
@SpringBatchTest
@SpringBootTest(classes = {
    DormantMemberJobConfig.class,  // 테스트 대상 Job Config
    TestBatchConfig.class           // 테스트 설정
})
@ActiveProfiles("test")  // application-test.yml 사용 (선택)
class DormantMemberJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private MemberRepository memberRepository;  // 데이터 검증용

    @BeforeEach
    void setUp() {
        // 필수: 이전 Job 실행 기록 제거
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        // 선택: 테스트 데이터 정리
        memberRepository.deleteAll();
    }
}
```

### 패턴 2: Job 레벨 테스트

```java
@Test
@DisplayName("휴면회원 전환 Job - 1년 이상 미접속 회원 전환 성공")
void dormantMemberJob_1년이상미접속회원존재_휴면전환성공() throws Exception {
    // given - 테스트 데이터 준비
    LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1).minusDays(1);
    Member targetMember = memberRepository.save(
        Member.builder()
            .name("테스트회원")
            .email("test@test.com")
            .status(MemberStatus.ACTIVE)
            .lastLoginAt(oneYearAgo)
            .build()
    );

    JobParameters params = new JobParametersBuilder()
            .addString("targetDate", LocalDate.now().toString())
            .addLong("timestamp", System.currentTimeMillis())  // 고유성 보장
            .toJobParameters();

    // when - Job 실행
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

    // then - 결과 검증
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

    // 데이터 검증
    Member updatedMember = memberRepository.findById(targetMember.getId())
            .orElseThrow();
    assertThat(updatedMember.getStatus()).isEqualTo(MemberStatus.DORMANT);
}

@Test
@DisplayName("휴면회원 전환 Job - 대상 회원 없으면 정상 종료")
void dormantMemberJob_대상회원없음_정상완료() throws Exception {
    // given - 대상 없는 상황 (최근 접속 회원만)
    memberRepository.save(
        Member.builder()
            .name("최근접속회원")
            .email("recent@test.com")
            .status(MemberStatus.ACTIVE)
            .lastLoginAt(LocalDateTime.now().minusDays(10))
            .build()
    );

    JobParameters params = new JobParametersBuilder()
            .addString("targetDate", LocalDate.now().toString())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

    // then - Job은 성공하지만 처리 건수 0
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    
    StepExecution stepExecution = jobExecution.getStepExecutions()
            .iterator().next();
    assertThat(stepExecution.getReadCount()).isZero();
    assertThat(stepExecution.getWriteCount()).isZero();
}
```

### 패턴 3: Step 레벨 테스트

```java
@Test
@DisplayName("휴면회원 Step - 단독 실행 테스트")
void dormantMemberStep_단독실행_성공() throws Exception {
    // given
    Member member = memberRepository.save(
        Member.builder()
            .name("테스트")
            .status(MemberStatus.ACTIVE)
            .lastLoginAt(LocalDateTime.now().minusYears(2))
            .build()
    );

    JobParameters params = new JobParametersBuilder()
            .addString("targetDate", LocalDate.now().toString())
            .toJobParameters();

    // when - 특정 Step만 실행
    JobExecution jobExecution = jobLauncherTestUtils.launchStep(
            "dormantMemberStep",  // Step 이름
            params
    );

    // then
    StepExecution stepExecution = jobExecution.getStepExecutions()
            .iterator().next();
    
    assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(stepExecution.getReadCount()).isEqualTo(1);
    assertThat(stepExecution.getWriteCount()).isEqualTo(1);
}
```

### 패턴 4: StepExecution 상세 검증

```java
@Test
void stepExecution_상세정보_검증() throws Exception {
    // given & when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

    // then
    StepExecution stepExecution = jobExecution.getStepExecutions()
            .stream()
            .filter(se -> se.getStepName().equals("targetStep"))
            .findFirst()
            .orElseThrow();

    // 처리 건수 검증
    assertThat(stepExecution.getReadCount()).isEqualTo(100);
    assertThat(stepExecution.getWriteCount()).isEqualTo(95);
    assertThat(stepExecution.getFilterCount()).isEqualTo(5);  // Processor에서 null 반환
    assertThat(stepExecution.getSkipCount()).isEqualTo(0);
    assertThat(stepExecution.getCommitCount()).isEqualTo(1);   // CHUNK_SIZE=100일 때
    assertThat(stepExecution.getRollbackCount()).isEqualTo(0);

    // 실행 시간 검증 (선택)
    assertThat(stepExecution.getStartTime()).isNotNull();
    assertThat(stepExecution.getEndTime()).isNotNull();
    
    // ExecutionContext 검증
    ExecutionContext context = stepExecution.getExecutionContext();
    assertThat(context.containsKey("processedIds")).isTrue();
}
```

### 패턴 5: JobParameters 테스트

```java
@Test
@DisplayName("필수 파라미터 누락 시 실패")
void job_필수파라미터누락_실패() {
    // given - targetDate 파라미터 누락
    JobParameters params = new JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

    // when & then
    assertThatThrownBy(() -> jobLauncherTestUtils.launchJob(params))
            .isInstanceOf(JobParametersInvalidException.class);
}

@Test
@DisplayName("동일 파라미터로 재실행 시 기존 실행 재개")
void job_동일파라미터재실행_재개() throws Exception {
    // given - 첫 번째 실행 (실패로 가정)
    JobParameters params = new JobParametersBuilder()
            .addString("targetDate", "2025-01-15")
            .toJobParameters();

    // 첫 번째 실행이 FAILED 상태라고 가정
    // 두 번째 실행은 재시작으로 처리됨
    JobExecution firstExecution = jobLauncherTestUtils.launchJob(params);
    // ... 실패 시뮬레이션

    // when - 동일 파라미터로 재실행
    JobExecution secondExecution = jobLauncherTestUtils.launchJob(params);

    // then - 동일 JobInstance
    assertThat(secondExecution.getJobInstance().getId())
            .isEqualTo(firstExecution.getJobInstance().getId());
}
```

### 패턴 6: 예외 상황 테스트

```java
@Test
@DisplayName("처리 중 예외 발생 시 Step 실패")
void step_예외발생_실패() throws Exception {
    // given - 예외를 유발하는 데이터
    memberRepository.save(
        Member.builder()
            .name(null)  // NotNull 제약 위반
            .status(MemberStatus.ACTIVE)
            .lastLoginAt(LocalDateTime.now().minusYears(2))
            .build()
    );

    JobParameters params = new JobParametersBuilder()
            .addString("targetDate", LocalDate.now().toString())
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

    // then
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
    
    // 실패 원인 확인
    jobExecution.getAllFailureExceptions()
            .forEach(e -> log.info("Failure: {}", e.getMessage()));
}
```

### 패턴 7: Skip/Retry 동작 테스트

```java
@Test
@DisplayName("Skip 설정 시 오류 건은 건너뛰고 계속 진행")
void step_skip설정_오류건건너뜀() throws Exception {
    // given - 정상 10건 + 오류 유발 2건
    for (int i = 0; i < 10; i++) {
        memberRepository.save(createValidMember(i));
    }
    memberRepository.save(createInvalidMember()); // 오류 1
    memberRepository.save(createInvalidMember()); // 오류 2

    JobParameters params = createParams();

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

    // then
    StepExecution stepExecution = getStepExecution(jobExecution, "skipStep");
    
    assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(stepExecution.getReadCount()).isEqualTo(12);
    assertThat(stepExecution.getWriteCount()).isEqualTo(10);
    assertThat(stepExecution.getSkipCount()).isEqualTo(2);  // 2건 Skip
}
```

### 패턴 8: Listener 동작 검증

```java
@Test
@DisplayName("JobExecutionListener가 정상 호출됨")
void listener_호출검증() throws Exception {
    // given
    JobParameters params = createParams();

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

    // then - ExecutionContext에 리스너가 저장한 값 확인
    ExecutionContext context = jobExecution.getExecutionContext();
    assertThat(context.getString("startTime")).isNotNull();
    assertThat(context.getString("endTime")).isNotNull();
}
```

### 패턴 9: @Sql을 활용한 테스트 데이터 준비

```java
@Test
@Sql(scripts = "/sql/test-members.sql", 
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", 
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
void job_sql로데이터준비() throws Exception {
    // given - @Sql로 데이터 준비됨
    JobParameters params = createParams();

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

    // then
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
}
```

```sql
-- test-members.sql
INSERT INTO member (id, name, email, status, last_login_at) VALUES
(1, '회원1', 'user1@test.com', 'ACTIVE', '2023-01-01 00:00:00'),
(2, '회원2', 'user2@test.com', 'ACTIVE', '2023-06-01 00:00:00'),
(3, '회원3', 'user3@test.com', 'ACTIVE', '2024-12-01 00:00:00');
```

### 패턴 10: Partitioning 테스트

```java
@Test
@DisplayName("Partitioning - 10개 파티션으로 병렬 처리")
void partitionJob_10파티션_병렬처리() throws Exception {
    // given - 10,000건 데이터
    for (int i = 0; i < 10000; i++) {
        memberRepository.save(createMember(i));
    }

    JobParameters params = createParams();

    // when
    JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);

    // then
    assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // 파티션 Step 확인
    Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
    
    // Master Step + Slave Step * 10
    long slaveStepCount = stepExecutions.stream()
            .filter(se -> se.getStepName().contains("partition"))
            .count();
    assertThat(slaveStepCount).isEqualTo(10);

    // 전체 처리 건수 합계
    int totalWriteCount = stepExecutions.stream()
            .mapToInt(StepExecution::getWriteCount)
            .sum();
    assertThat(totalWriteCount).isEqualTo(10000);
}
```

---

## ⚠️ 주의사항

### 1. Job 자동 주입 금지

```java
// ❌ 여러 Job이 있으면 NoUniqueBeanDefinitionException
@Autowired
private Job dormantMemberJob;

// ✅ JobLauncherTestUtils 사용
@Autowired
private JobLauncherTestUtils jobLauncherTestUtils;
```

### 2. 이전 실행 기록 제거 필수

```java
@BeforeEach
void setUp() {
    // 필수! 이전 테스트의 JobExecution이 남아있으면
    // "이미 완료된 Job" 또는 "실행 중인 Job" 에러 발생
    jobRepositoryTestUtils.removeJobExecutions();
}
```

### 3. 고유한 JobParameters 사용

```java
// ❌ 항상 동일한 파라미터 - 두 번째 실행 시 실패
JobParameters params = new JobParametersBuilder()
        .addString("date", "2025-01-15")
        .toJobParameters();

// ✅ 매번 다른 파라미터
JobParameters params = new JobParametersBuilder()
        .addString("date", "2025-01-15")
        .addLong("timestamp", System.currentTimeMillis())  // 고유값 추가
        .toJobParameters();
```

### 4. H2 메모리 DB 주의

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    # DB_CLOSE_DELAY=-1: JVM 종료 전까지 DB 유지
```

### 5. @Transactional과 배치 테스트

```java
// ❌ @Transactional을 붙이면 배치 실행과 트랜잭션이 꼬임
@Test
@Transactional  // 사용 금지!
void wrongTest() { }

// ✅ 테스트 데이터 정리는 @AfterEach에서
@AfterEach
void tearDown() {
    memberRepository.deleteAll();
}
```

---

## 📋 테스트 체크리스트

### 기본 설정

- [ ] `@SpringBatchTest` + `@SpringBootTest` 어노테이션
- [ ] TestBatchConfig.class 포함
- [ ] `@BeforeEach`에서 removeJobExecutions() 호출

### 테스트 케이스 커버리지

- [ ] 성공 케이스 (정상 데이터)
- [ ] 빈 데이터 케이스 (처리 대상 없음)
- [ ] 실패 케이스 (예외 발생)
- [ ] 경계값 케이스 (Chunk 경계 등)
- [ ] Skip/Retry 동작 (해당되는 경우)

### 검증 항목

- [ ] JobExecution.status (COMPLETED/FAILED)
- [ ] StepExecution.readCount, writeCount
- [ ] 실제 데이터 변경 결과
- [ ] ExecutionContext 내용 (필요시)

---

## 🔗 관련 스킬

- **기초 개념**: `@skills/spring-batch-core.md`
- **Chunk 처리**: `@skills/spring-batch-chunk.md`
- **에러 처리**: `@skills/spring-batch-error.md`
