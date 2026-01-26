# Spring Batch Skills
## BatchFlow 프로젝트 기술 가이드 (Step 1-50)

---

## 📍 Step 매핑 빠른 참조

| Phase | Step | 주제 | 바로가기 |
|-------|------|------|----------|
| 1 | 1-8 | 기초: 프로젝트/Job/Step/Flow | [Phase 1](#phase-1-기초-다지기-step-1-8) |
| 2 | 9-18 | 핵심: Chunk/Reader/Processor/Writer | [Phase 2](#phase-2-핵심-패턴-step-9-18) |
| 3 | 19-26 | 안정성: Skip/Retry/Listener | [Phase 3](#phase-3-오류-제어와-안정성-step-19-26) |
| 4 | 27-35 | 성능: Multi-thread/Partitioning | [Phase 4](#phase-4-성능-최적화-step-27-35) |
| 5 | 36-42 | 운영: JobOperator/스케줄링/모니터링 | [Phase 5](#phase-5-운영과-모니터링-step-36-42) |
| 6 | 43-50 | 실전: 정산Job/알림Job | [Phase 6](#phase-6-실전-프로젝트-step-43-50) |

---

## 🔧 필수 기술 제약 (모든 Step 공통)

```
Java:           1.8 (var/record/text block 금지)
Spring Boot:    2.7.17
Spring Batch:   4.3.x (Factory 패턴 사용)
Package:        javax.* (jakarta.* 금지)
```

```java
// ✅ Spring Batch 4.x 스타일 (필수)
@RequiredArgsConstructor
public class MyJobConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
}

// ❌ Spring Batch 5.x 스타일 (금지)
public Job myJob(JobRepository jobRepository) {
    return new JobBuilder("myJob", jobRepository)...
}
```

---

# Phase 1: 기초 다지기 (Step 1-8)

## Step 1: 프로젝트 초기화

```groovy
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '2.7.17'
    id 'io.spring.dependency-management' version '1.0.15.RELEASE'
}

sourceCompatibility = '1.8'

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-batch'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.h2database:h2'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.batch:spring-batch-test'
}
```

```yaml
# application.yml
spring:
  batch:
    jdbc:
      initialize-schema: always
    job:
      enabled: false  # 자동 실행 비활성화
  datasource:
    url: jdbc:h2:mem:batchdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
```

```java
@Configuration
@EnableBatchProcessing  // 필수!
public class BatchConfig {
}
```

**체크포인트**: H2 콘솔에서 `BATCH_JOB_INSTANCE`, `BATCH_JOB_EXECUTION` 등 6개 테이블 확인

---

## Step 2-3: Job/Step/Tasklet

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
                    return RepeatStatus.FINISHED;  // 1회 실행
                })
                .build();
    }
}
```

---

## Step 4-5: JobParameters

```java
@Bean
@JobScope  // JobParameters 사용 시 필수!
public Step paramStep(@Value("#{jobParameters['requestDate']}") String requestDate) {
    return stepBuilderFactory.get("paramStep")
            .tasklet((contribution, chunkContext) -> {
                log.info(">>>>> 요청일자: {}", requestDate);
                return RepeatStatus.FINISHED;
            })
            .build();
}
```

| Scope | 용도 | 적용 대상 |
|-------|------|----------|
| `@JobScope` | JobParameters 접근 | Step |
| `@StepScope` | JobParameters + ExecutionContext | Reader/Processor/Writer |

---

## Step 6-7: Flow 조건 분기

```java
@Bean
public Job flowJob() {
    return jobBuilderFactory.get("flowJob")
            .start(checkStep())
                .on("COMPLETED").to(successStep())
            .from(checkStep())
                .on("FAILED").to(failStep())
            .from(checkStep())
                .on("*").to(defaultStep())
            .end()
            .build();
}

// JobExecutionDecider - 복잡한 분기 로직
@Bean
public JobExecutionDecider decider() {
    return (jobExecution, stepExecution) -> {
        String result = /* 비즈니스 로직 */;
        return new FlowExecutionStatus(result);
    };
}
```

---

## Step 8: ExecutionContext (데이터 공유)

```java
// Job ExecutionContext - 모든 Step에서 공유
ExecutionContext jobContext = chunkContext.getStepContext()
        .getStepExecution()
        .getJobExecution()
        .getExecutionContext();
jobContext.putString("sharedKey", "value");

// Step ExecutionContext - 해당 Step에서만
ExecutionContext stepContext = chunkContext.getStepContext()
        .getStepExecution()
        .getExecutionContext();
```

---

# Phase 2: 핵심 패턴 (Step 9-18)

## Step 9-11: Chunk 모델

```java
private static final int CHUNK_SIZE = 1000;

@Bean
public Step chunkStep() {
    return stepBuilderFactory.get("chunkStep")
            .<InputType, OutputType>chunk(CHUNK_SIZE)
            .reader(itemReader())
            .processor(itemProcessor())
            .writer(itemWriter())
            .build();
}
```

**처리 흐름**: Reader(1건씩) → Processor(1건씩) → Writer(Chunk 단위 커밋)

---

## Step 12-14: ItemReader

### JdbcCursorItemReader (단일 스레드)

```java
@Bean
public JdbcCursorItemReader<Member> cursorReader(DataSource dataSource) {
    return new JdbcCursorItemReaderBuilder<Member>()
            .name("cursorReader")
            .dataSource(dataSource)
            .sql("SELECT * FROM member WHERE status = 'ACTIVE' ORDER BY id")
            .rowMapper(new BeanPropertyRowMapper<>(Member.class))
            .fetchSize(CHUNK_SIZE)
            .build();
}
```

### JdbcPagingItemReader (멀티스레드 가능)

```java
@Bean
@StepScope
public JdbcPagingItemReader<Member> pagingReader(
        DataSource dataSource,
        @Value("#{jobParameters['targetDate']}") String targetDate) {

    Map<String, Object> params = new HashMap<>();
    params.put("targetDate", targetDate);

    return new JdbcPagingItemReaderBuilder<Member>()
            .name("pagingReader")
            .dataSource(dataSource)
            .pageSize(CHUNK_SIZE)
            .selectClause("SELECT *")
            .fromClause("FROM member")
            .whereClause("WHERE created_at < :targetDate")
            .sortKeys(Collections.singletonMap("id", Order.ASCENDING))  // 필수!
            .parameterValues(params)
            .rowMapper(new BeanPropertyRowMapper<>(Member.class))
            .build();
}
```

### JpaPagingItemReader

```java
@Bean
@StepScope
public JpaPagingItemReader<Member> jpaReader(EntityManagerFactory emf) {
    return new JpaPagingItemReaderBuilder<Member>()
            .name("jpaReader")
            .entityManagerFactory(emf)
            .pageSize(CHUNK_SIZE)
            .queryString("SELECT m FROM Member m WHERE m.status = :status ORDER BY m.id")
            .parameterValues(Collections.singletonMap("status", "ACTIVE"))
            .build();
}
```

⚠️ **Paging Reader는 반드시 ORDER BY 필수!** (없으면 데이터 누락/중복)

---

## Step 15-16: ItemProcessor

```java
// 단일 Processor
@Bean
public ItemProcessor<Member, Member> dormantProcessor() {
    return member -> {
        if (member.getEmail() == null) {
            return null;  // null = 필터링 (Writer로 안 감)
        }
        member.updateToDormant();
        return member;
    };
}

// CompositeItemProcessor (체이닝)
@Bean
public CompositeItemProcessor<Member, Member> compositeProcessor() {
    CompositeItemProcessor<Member, Member> processor = new CompositeItemProcessor<>();
    processor.setDelegates(Arrays.asList(
            validationProcessor(),
            transformProcessor(),
            enrichmentProcessor()
    ));
    return processor;
}
```

---

## Step 17-18: ItemWriter

```java
// JpaItemWriter
@Bean
public JpaItemWriter<Member> jpaWriter(EntityManagerFactory emf) {
    JpaItemWriter<Member> writer = new JpaItemWriter<>();
    writer.setEntityManagerFactory(emf);
    return writer;
}

// JdbcBatchItemWriter
@Bean
public JdbcBatchItemWriter<Member> jdbcWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Member>()
            .dataSource(dataSource)
            .sql("UPDATE member SET status = :status WHERE id = :id")
            .beanMapped()
            .build();
}

// CompositeItemWriter (다중 Writer)
@Bean
public CompositeItemWriter<Member> compositeWriter() {
    CompositeItemWriter<Member> writer = new CompositeItemWriter<>();
    writer.setDelegates(Arrays.asList(
            memberWriter(),
            historyWriter()
    ));
    return writer;
}
```

---

# Phase 3: 오류 제어와 안정성 (Step 19-26)

## Step 19-21: Skip 처리

```java
@Bean
public Step skipStep() {
    return stepBuilderFactory.get("skipStep")
            .<Member, Member>chunk(CHUNK_SIZE)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .faultTolerant()
            .skipLimit(10)  // 최대 10건까지 Skip
            .skip(ValidationException.class)
            .skip(DataIntegrityViolationException.class)
            .noSkip(IllegalStateException.class)  // 이건 Skip 안 함
            .build();
}
```

---

## Step 22-23: Retry 처리

```java
@Bean
public Step retryStep() {
    return stepBuilderFactory.get("retryStep")
            .<Member, Member>chunk(CHUNK_SIZE)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .faultTolerant()
            .retryLimit(3)  // 최대 3회 재시도
            .retry(DeadlockLoserDataAccessException.class)
            .retry(OptimisticLockingFailureException.class)
            .noRetry(ValidationException.class)
            .build();
}
```

---

## Step 24-26: Listener

```java
// JobExecutionListener
@Slf4j
@Component
public class JobLogListener implements JobExecutionListener {
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>>>> [{}] Job 시작", jobExecution.getJobInstance().getJobName());
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        long duration = Duration.between(
                jobExecution.getStartTime(), 
                jobExecution.getEndTime()
        ).toMillis();
        log.info(">>>>> [{}] Job 종료. 상태: {}, 소요시간: {}ms",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus(),
                duration);
    }
}

// StepExecutionListener
@Slf4j
@Component
public class StepLogListener implements StepExecutionListener {
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info(">>>>> [{}] Step 시작", stepExecution.getStepName());
    }
    
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info(">>>>> [{}] Step 종료. Read: {}, Write: {}, Skip: {}",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount());
        return stepExecution.getExitStatus();
    }
}

// SkipListener
@Slf4j
@Component
public class SkipLogListener implements SkipListener<Member, Member> {
    
    @Override
    public void onSkipInRead(Throwable t) {
        log.warn(">>>>> Read Skip: {}", t.getMessage());
    }
    
    @Override
    public void onSkipInProcess(Member item, Throwable t) {
        log.warn(">>>>> Process Skip. ID: {}, Error: {}", item.getId(), t.getMessage());
    }
    
    @Override
    public void onSkipInWrite(Member item, Throwable t) {
        log.warn(">>>>> Write Skip. ID: {}, Error: {}", item.getId(), t.getMessage());
    }
}

// Step에 Listener 적용
@Bean
public Step stepWithListeners() {
    return stepBuilderFactory.get("stepWithListeners")
            .<Member, Member>chunk(CHUNK_SIZE)
            .reader(reader())
            .processor(processor())
            .writer(writer())
            .listener(stepLogListener)
            .listener(skipLogListener)
            .build();
}
```

---

# Phase 4: 성능 최적화 (Step 27-35)

## Step 27-29: Multi-Thread Step

```java
@Bean
public Step multiThreadStep() {
    return stepBuilderFactory.get("multiThreadStep")
            .<Member, Member>chunk(CHUNK_SIZE)
            .reader(pagingReader())  // Thread-safe Reader 필수!
            .processor(processor())
            .writer(writer())
            .taskExecutor(taskExecutor())
            .throttleLimit(4)  // 동시 실행 스레드 수
            .build();
}

@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setThreadNamePrefix("batch-thread-");
    return executor;
}
```

⚠️ **CursorReader는 Thread-safe 아님!** → PagingReader 사용

---

## Step 30-32: Parallel Steps

```java
@Bean
public Job parallelJob() {
    Flow flow1 = new FlowBuilder<SimpleFlow>("flow1")
            .start(step1())
            .build();
            
    Flow flow2 = new FlowBuilder<SimpleFlow>("flow2")
            .start(step2())
            .build();

    return jobBuilderFactory.get("parallelJob")
            .start(flow1)
            .split(taskExecutor())  // 병렬 실행!
            .add(flow2)
            .end()
            .build();
}
```

---

## Step 33-35: Partitioning

```java
// Partitioner - 데이터 분할 전략
@Component
public class MemberIdRangePartitioner implements Partitioner {
    
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> partitions = new HashMap<>();
        
        long min = 1L;
        long max = 100000L;
        long range = (max - min) / gridSize + 1;
        
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minId", min + (range * i));
            context.putLong("maxId", Math.min(min + (range * (i + 1)) - 1, max));
            partitions.put("partition" + i, context);
        }
        
        return partitions;
    }
}

// Master Step
@Bean
public Step masterStep() {
    return stepBuilderFactory.get("masterStep")
            .partitioner("workerStep", partitioner)
            .step(workerStep())
            .gridSize(4)  // 파티션 수
            .taskExecutor(taskExecutor())
            .build();
}

// Worker Step (각 파티션에서 실행)
@Bean
public Step workerStep() {
    return stepBuilderFactory.get("workerStep")
            .<Member, Member>chunk(CHUNK_SIZE)
            .reader(partitionReader(null, null))  // @StepScope로 동적 주입
            .processor(processor())
            .writer(writer())
            .build();
}

// Partition Reader
@Bean
@StepScope
public JpaPagingItemReader<Member> partitionReader(
        @Value("#{stepExecutionContext['minId']}") Long minId,
        @Value("#{stepExecutionContext['maxId']}") Long maxId) {
    
    Map<String, Object> params = new HashMap<>();
    params.put("minId", minId);
    params.put("maxId", maxId);
    
    return new JpaPagingItemReaderBuilder<Member>()
            .name("partitionReader")
            .entityManagerFactory(emf)
            .queryString("SELECT m FROM Member m WHERE m.id BETWEEN :minId AND :maxId ORDER BY m.id")
            .parameterValues(params)
            .pageSize(CHUNK_SIZE)
            .build();
}
```

---

# Phase 5: 운영과 모니터링 (Step 36-42)

## Step 36-37: JobOperator

```java
@Configuration
@EnableBatchProcessing
public class BatchOperatorConfig {
    
    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }
    
    @Bean
    public JobOperator jobOperator(JobLauncher jobLauncher, JobRepository jobRepository,
                                   JobExplorer jobExplorer, JobRegistry jobRegistry) {
        SimpleJobOperator operator = new SimpleJobOperator();
        operator.setJobLauncher(jobLauncher);
        operator.setJobRepository(jobRepository);
        operator.setJobExplorer(jobExplorer);
        operator.setJobRegistry(jobRegistry);
        return operator;
    }
}

// REST Controller
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchController {
    
    private final JobOperator jobOperator;
    
    @PostMapping("/jobs/{jobName}/start")
    public Long startJob(@PathVariable String jobName) throws Exception {
        return jobOperator.start(jobName, "requestDate=" + LocalDate.now());
    }
    
    @PostMapping("/jobs/{executionId}/stop")
    public boolean stopJob(@PathVariable Long executionId) throws Exception {
        return jobOperator.stop(executionId);
    }
    
    @PostMapping("/jobs/{executionId}/restart")
    public Long restartJob(@PathVariable Long executionId) throws Exception {
        return jobOperator.restart(executionId);
    }
}
```

---

## Step 38-39: 알림 & 스케줄링

```java
// 실패 알림 Listener
@Slf4j
@Component
@RequiredArgsConstructor
public class JobFailureAlertListener implements JobExecutionListener {
    
    private final SlackNotifier slackNotifier;
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            slackNotifier.send(String.format(
                    "🚨 배치 실패!\nJob: %s\n시간: %s\n오류: %s",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getEndTime(),
                    jobExecution.getAllFailureExceptions()
            ));
        }
    }
}

// 스케줄러
@Configuration
@EnableScheduling
public class BatchScheduler {
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job dormantMemberJob;
    
    @Scheduled(cron = "0 0 2 * * ?")  // 매일 새벽 2시
    public void runDormantJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("requestDate", LocalDate.now().toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        
        jobLauncher.run(dormantMemberJob, params);
    }
}
```

---

## Step 40-42: 모니터링 & 환경 설정

```yaml
# application-prod.yml
spring:
  batch:
    jdbc:
      initialize-schema: never  # 운영에서는 직접 관리
    job:
      enabled: false

management:
  endpoints:
    web:
      exposure:
        include: health, metrics, batch
  endpoint:
    health:
      show-details: always
```

---

# Phase 6: 실전 프로젝트 (Step 43-50)

## Step 43-46: 일일 정산 Job

```java
@Bean
public Job dailySettlementJob() {
    return jobBuilderFactory.get("dailySettlementJob")
            .start(readTransactionStep())      // 거래 내역 조회
            .next(calculateSettlementStep())   // 정산 금액 계산
            .next(saveSettlementStep())        // 정산 결과 저장
            .listener(settlementJobListener)
            .build();
}
```

---

## Step 47-50: 대량 알림 Job

```java
@Bean
public Job bulkNotificationJob() {
    return jobBuilderFactory.get("bulkNotificationJob")
            .start(masterNotificationStep())  // Partitioning 적용
            .listener(notificationJobListener)
            .build();
}

@Bean
public Step masterNotificationStep() {
    return stepBuilderFactory.get("masterNotificationStep")
            .partitioner("workerNotificationStep", memberPartitioner)
            .step(workerNotificationStep())
            .gridSize(10)  // 10개 파티션
            .taskExecutor(taskExecutor())
            .build();
}
```

---

# 📋 테스트 패턴 (모든 Step 공통)

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
        jobRepositoryTestUtils.removeJobExecutions();  // 필수!
    }

    @Test
    void job_성공() throws Exception {
        // given
        JobParameters params = new JobParametersBuilder()
                .addString("requestDate", LocalDate.now().toString())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void step_단독_테스트() {
        // when
        JobExecution execution = jobLauncherTestUtils.launchStep("targetStep");

        // then
        StepExecution stepExecution = execution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(100);
        assertThat(stepExecution.getWriteCount()).isEqualTo(100);
    }
}
```

---

# ⚠️ 흔한 실수 모음

| 실수 | 문제 | 해결 |
|------|------|------|
| ORDER BY 누락 | Paging 시 데이터 누락/중복 | 항상 ORDER BY 추가 |
| @Scope 누락 | JobParameters null | @JobScope/@StepScope 추가 |
| pageSize ≠ chunkSize | 비효율적 처리 | 동일하게 설정 |
| CursorReader + 멀티스레드 | Thread-safe 아님 | PagingReader 사용 |
| var 키워드 사용 | Java 1.8 호환 불가 | 명시적 타입 선언 |
| jakarta.* 패키지 | Spring Boot 2.x 호환 불가 | javax.* 사용 |
