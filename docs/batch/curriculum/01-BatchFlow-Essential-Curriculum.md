# BatchFlow 필수 트랙 커리큘럼 — Spring Batch 기본기

> **대상**: Spring Batch를 처음 접하는 SpringBoot 개발자 (TestCraft 수료 권장 — 테스트 작성 기본기 전제)
> **목표**: 약 2주(하루 1~2시간) 안에 Job 설계 → Chunk 처리 → 오류 제어 → 재시작까지,
> **실무 배치를 스스로 만들고 테스트할 수 있는 기본기**를 만든다.
> **스택**: Java 1.8 · Spring Boot 2.7.17 · **Spring Batch 4.3.x** (5.x 금지!) · H2(MS-SQL 호환 모드) · JUnit5
>
> 📚 이 문서가 **학습 기준**입니다. 기존 [50-Step 전체 커리큘럼(00 문서)](./00-BatchFlow-Curriculum.md)은
> 필수 트랙 완주 후의 **심화/전체 참조 자료**로 보존되어 있습니다.

---

## 1. 학습 철학: 문제를 먼저 겪고, 해결책을 배운다 (TestCraft와 동일)

```
Hello Job             "같은 Job을 또 돌리니 안 돌아간다?!"
   ↓ 한계
JobParameters         "성공/실패에 따라 다음 단계를 바꾸고 싶은데?"
   ↓ 한계
Flow & Decider        "앞 Step이 계산한 값을 뒤 Step이 어떻게 받지?"
   ↓ 한계
ExecutionContext      "10만 건을 Tasklet 하나로? 중간에 죽으면 전부 날아간다"
   ↓ 한계
Chunk 모델            "전체를 메모리에 다 올릴 수는 없는데"
   ↓ 한계
Cursor/Paging Reader  "읽기만? 변환하고 걸러내고 저장해야지"
   ↓ 한계
Processor & Writer    "조각을 모아 진짜 업무(휴면 전환)를 완성하자"
   ↓ 통합
휴면회원 전환 Job       "1건 오류로 99,999건이 롤백된다?!"
   ↓ 한계
Skip/Retry/Listener   "새벽 3시에 죽은 배치, 처음부터 다시?"
   ↓ 한계
재시작과 멱등성         "이제 스스로 설계할 수 있는가"
   ↓ 졸업
캡스톤: 일일 정산 Job
```

## 2. 사용 방법

### 2-1. 시작하기

```bash
# 모듈 테스트가 도는지 먼저 확인 (skipped는 여러분이 풀 exercise — 정상!)
.\gradlew :spring-batch-onboarding:test
```

> Batch는 웹앱이 아니므로 화면 대신 **테스트가 실행의 기본 수단**입니다.
> Job 실행 결과는 테스트 로그와 BATCH_* 메타데이터 테이블로 확인합니다.

### 2-2. Step 진행 루틴 (Step당 1~1.5시간) — TestCraft와 동일

1. `docs/batch/education/FOR-BatchFlow-StepNN.md` 문서 읽기
2. `stepNN/example/` 완성 테스트 실행 + 주석 읽기
3. `stepNN/exercise/` `@Disabled` 제거 후 TODO 풀기
4. `stepNN/answer/` 와 비교

## 3. 커리큘럼 전체 지도

### 필수 트랙 (Step 1~13, 약 2주)

| Step | 제목 | 배우는 이유 (앞 단계의 한계) | 핵심 도구 | 50-Step 매핑 | 소요 |
|------|------|---------------------------|----------|:---:|------|
| 1 | 배치 인프라 — 판 깔기 | 실행 기록 없는 배치는 신뢰할 수 없다 | @EnableBatchProcessing, BATCH_* 6종, H2(MSSQLServer) | 1, 8 | 1h |
| 2 | Hello Job — Job/Step/Tasklet | 가장 작은 배치의 해부 | JobBuilderFactory, Tasklet, RepeatStatus, @SpringBatchTest | 2 | 1h |
| 3 | JobParameters & JobInstance | 같은 Job인데 두 번째 실행이 거부된다?! | JobParameters, JobInstance vs JobExecution, @JobScope | 3 | 1.5h |
| 4 | Flow 제어 — 분기와 Decider | 성공/실패에 따라 다음 단계를 바꾸려면 | ExitStatus, .on()/.to()/.end(), JobExecutionDecider | 4~6 | 1.5h |
| 5 | ExecutionContext — Step 간 데이터 공유 | 앞 Step의 계산 결과를 뒤 Step이 받으려면 | Step/Job ExecutionContext, PromotionListener | 7 | 1h |
| 6 | Chunk 모델 첫 경험 | Tasklet 하나로 10만 건? 중간 실패는? | reader/processor/writer, chunk 트랜잭션 경계, 도메인 스키마 도입 | 9 | 1.5h |
| 7 | JdbcCursorItemReader | 전체를 메모리에 올릴 수 없다 | 커서 스트리밍, RowMapper, fetchSize | 10 | 1h |
| 8 | JdbcPagingItemReader | 커서를 오래 못 여는 환경이라면 | PagingQueryProvider, sortKeys, thread-safe | 11 | 1.5h |
| 9 | Processor & Writer | 읽었으면 변환·필터링·저장 | 필터링(null), CompositeItemProcessor, JdbcBatchItemWriter | 13, 14 | 1.5h |
| 10 | 실전: 휴면회원 전환 Job | 조각을 모아 진짜 업무로 | 통합 + StepExecution 카운트 검증(read/filter/write) | 15~18 | 1.5h |
| 11 | 오류 제어 — Skip/Retry/Listener | 1건 오류 = 전체 실패여선 안 된다 | faultTolerant, skip/retry+BackOff, SkipListener, ExecutionListener | 19~22 | 1.5h |
| 12 | 재시작과 멱등성 | 새벽에 죽은 배치, 어디서부터 다시? | 실패 지점 재시작, saveState, allowStartIfComplete | 24, 25 | 1.5h |
| 13 | **캡스톤: 일일 정산 Job** | 정답지 없이 스스로 설계할 수 있는가 | 요구사항 → Job 설계 → 전체 구현+테스트 (answer 제공) | 43~46 | 2h |

### 심화 (선택)

| Step | 제목 | 동기 | 핵심 | 50-Step 매핑 | 소요 |
|------|------|------|------|:---:|------|
| 14 | Multi-threaded Step & Parallel Flow | 4시간짜리 배치를 1시간으로 | TaskExecutor, thread-safe reader 조건, Flow.split | 27, 28 | 1.5h |
| 15 | Partitioning | 스레드만으론 부족할 때 — 데이터 분할 | Partitioner, gridSize, Master-Worker | 29 | 1.5h |
| 16 | 비동기 처리 — Processor가 느릴 때 | 병목이 가공(외부 호출)뿐인데 Step 전체 병렬화는 과잉 | AsyncItemProcessor/Writer(integration 모듈!), fetchSize/batch INSERT 튜닝, 성능 비교 3원칙 | 30~32, 34 | 1.5h |
| 17 | JobOperator와 실행 이력 — 운영자의 콘솔 | 테스트 코드 없이 Job을 제어·재기동하려면 | JobOperator, JobRegistry(BeanPostProcessor), restart, JobExplorer 이력 | 36, 37 (+39) | 1.5h |
| 18 | **제2 캡스톤: 대량 알림 발송** | 심화 무기(분할/skip/멱등)의 조합 판단을 스스로 | 파티셔닝+skip+자연 멱등(NOT EXISTS) 종합 — 요구사항+체크리스트+answer | 47~50 | 2.5h |

> 남은 참조 주제(알림/대시보드 연동, Quartz 클러스터)는
> [00 문서의 Phase 5](./00-BatchFlow-Curriculum.md)를 참조하세요.

## 4. 도메인: 미니 금융 배치

Step 6부터 하나의 도메인 위에서 진행합니다 (기존 50-Step 설계를 소규모로 압축).

| 테이블 | 용도 | 시드 |
|--------|------|------|
| member | 휴면 전환 대상 (status, last_login_at) | 50명 (ACTIVE 30 = 최근 20 + 휴면대상 10 / DORMANT 15 / WITHDRAWN 5) |
| bank_transaction | 일일 정산의 원천 (tx_type, amount) — TRANSACTION은 MS-SQL 예약어라 개명 | 15건 (06-10자 9건 = 정산 고정 시나리오) |
| settlement | 정산 결과 (캡스톤에서 생성) | 0건 |

> DB는 H2 인메모리 **MODE=MSSQLServer** — 실무(MS-SQL) SQL 방언으로 연습합니다.
> 대량(10만+) 데이터 설계는 [기존 스키마 문서](../sql/Database-Schema-And-Data.md) 참조 (심화 성능 실습용).

## 5. 배치 테스트의 기본 패턴 (전 Step 공통)

```java
@SpringBatchTest
@SpringBootTest(classes = {XxxJobConfig.class, TestBatchConfig.class})
class XxxJobConfigTest {
    @Autowired JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() { jobRepositoryTestUtils.removeJobExecutions(); } // 메타데이터 격리 — 필수!
}
```

- 필요한 JobConfig만 명시 로드 → 빠른 슬라이스형 실행
- `removeJobExecutions()` = TestCraft의 "롤백"에 해당하는 배치식 격리 장치

## 6. 완주 체크리스트

- [ ] Step 1~12의 모든 exercise를 통과시켰다
- [ ] Step 13 캡스톤에서 정산 Job을 스스로 설계·구현했다
- [ ] `.\gradlew :spring-batch-onboarding:test` 가 그린이다
- [ ] "이 업무를 배치로 만들면 Job/Step/Chunk를 어떻게 쪼갤지" 스스로 말할 수 있다
- [ ] 실패한 배치를 보고 "Skip할 것인가, Retry할 것인가, 재시작 설계는?"을 판단할 수 있다

## 7. 관련 문서

| 문서 | 경로 |
|------|------|
| Step별 교육 가이드 | `docs/batch/education/FOR-BatchFlow-StepNN.md` |
| 전체 50-Step 커리큘럼 (심화 참조) | `docs/batch/curriculum/00-BatchFlow-Curriculum.md` |
| 배치 테스트 패턴 | `docs/batch/skills/spring-batch-testing.md` |
| 모듈 규칙 | `spring-batch-onboarding/CLAUDE.md` |
| 계획/태스크 | `docs/batch/plan/plan.md`, `task.md` |
