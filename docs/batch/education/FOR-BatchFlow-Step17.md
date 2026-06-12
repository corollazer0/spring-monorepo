# [심화 Step 17] JobOperator와 실행 이력 — 운영자의 콘솔

> **소요 시간**: 약 1.5시간 (심화 — 필수 트랙 + Step 16 완주 후)
> **이번 Step의 도구**: `JobOperator`, `JobRegistry`(+`JobRegistryBeanPostProcessor`), `restart`, `JobExplorer` 이력 조회
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/ops, test/java/com/batchflow/advanced/step17}/`
> **50-Step 매핑**: Step 36(JobOperator), 37(실행 이력 관리) 압축 + 39(스케줄러 연동) 관점 정리

---

## 1. Before We Start — "재기동해 주세요"

운영 첫 주, 이런 메시지를 받습니다:

> "어젯밤 정산 배치 실패했어요. 원인(스토리지 장애)은 복구됐으니 **지금 재기동**
> 부탁드려요. 아, 그리고 **이번 달 실행 이력**도 좀 뽑아주세요."

지금까지 우리가 Job을 돌린 방법을 떠올려보세요 — `jobLauncherTestUtils.launchJob()`.
테스트 코드였죠. 운영자에게 "IDE 열고 테스트 돌리세요"라고 할 순 없습니다.
운영자는 **코드가 아니라 이름과 문자열**로 Job을 다뤄야 합니다:

```java
jobOperator.start("dailySettlementJob", "date=2026-06-12")
jobOperator.restart(실패한_실행ID)
```

이 인터페이스가 JobOperator — 운영 도구(관리 콘솔, 스케줄러, CLI)가 배치를
만지는 표준 창구입니다.

## 2. What We're Building

```
opsDemoJob  : BROKEN 스위치(환경 장애 교보재)에 따라 성공/실패하는 데모 Job
JobRegistry : 이름 → Job 전화번호부 (JobRegistryBeanPostProcessor가 자동 등록)
JobOperator : start(이름, "key=value") / restart(실행ID) / getSummary / getExecutions
JobExplorer : BATCH_* 장부의 읽기 전용 조회 API (이력의 창구)
```

```
src/main/java/com/batchflow/job/ops/OpsDemoJobConfig.java
src/test/java/com/batchflow/advanced/step17/
├── example/JobOperatorTest.java        ← 등록 확인 / 문자열 실행 / 재기동 시나리오
├── exercise/JobHistoryExerciseTest.java ← 2일치 이력: Instance vs Execution 세기
└── answer/JobHistoryAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 JobRegistry — 컨텍스트의 빈 ≠ 레지스트리의 등록 (1번 함정!)

JobOperator는 Job을 **이름으로** 찾습니다. 어디서? JobRegistry에서.
그런데 Job 빈을 만들었다고 레지스트리에 자동 등록되지는 않습니다:

```java
@Bean
public static JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
    JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
    postProcessor.setJobRegistry(jobRegistry);
    return postProcessor;   // 컨텍스트의 모든 Job 빈을 레지스트리에 자동 등록
}
```

이걸 빠뜨리면? Job 빈은 멀쩡한데 `operator.start(...)`가 **NoSuchJobException** —
"빈은 있는데 전화번호부에 없다"는 황당한 상황입니다. 그래서 example의 첫 테스트가
`getJobNames()` 등록 확인입니다 — 인프라 전제부터 봉인.

### 3-2. 🆕 문자열 파라미터 — 운영의 어휘

```java
Long executionId = jobOperator.start("opsDemoJob", "date=2026-06-12,trigger=manual");
```

`"key=value,key2=value2"` 문자열이 JobParameters로 변환됩니다(컨버터가 파싱).
운영 도구·스케줄러·CLI가 전부 이 형식으로 말합니다 — **타입 세계(코드)와
문자열 세계(운영)의 경계**가 여기입니다. Step 3에서 배운 JobInstance 규칙은
그대로: 같은 문자열로 성공한 작업은 다시 start 못 합니다(JobInstanceAlreadyExists).

### 3-3. 재기동 시나리오 — 이 Step의 백미

새벽 배치 실패의 표준 운영 절차를 코드로 재현합니다:

```
① 실패 확인     start(...) → FAILED (환경 장애: BROKEN=true)
② 원인 복구     BROKEN=false (운영자가 스토리지를 살렸다 — 코드 수정 없음!)
③ 재기동       restart(실패한_실행ID) → COMPLETED
④ 장부 확인     같은 JobInstance에 실행 2개 (실패 1 + 성공 1)
```

④가 핵심 통찰입니다 — restart는 **같은 작업(Instance)에 새로운 시도(Execution)를
추가**합니다. Step 3의 "Instance = 작업, Execution = 시도" 구분이 운영 화면의
이력 구조 그 자체였던 거죠. Step 12의 재시작(체크포인트)이 "어디서부터 다시"였다면,
Step 17은 "**누가, 무엇으로** 다시"입니다.

### 3-4. JobExplorer — 장부의 읽기 전용 창구

"이번 달 몇 번 돌았어요? 각각 결과는요?"의 답:

```java
jobExplorer.getJobInstances("opsDemoJob", 0, 10)   // 작업 목록 (페이징)
jobExplorer.getJobExecutions(instance)              // 작업별 시도들
jobOperator.getSummary(executionId)                 // 사람이 읽는 요약 문자열
```

전부 Step 1에서 만난 BATCH_* 메타테이블을 읽는 API입니다 — 첫 Step의
"장부"가 마지막 Step에서 운영 이력 시스템이었음이 드러납니다.

### 3-5. 스케줄러와의 관계 (50-Step 39의 관점 정리)

"매일 새벽 2시 실행"은 누가 하나? **스케줄러(Quartz, Jenkins, 사내 배치 관리
시스템...)가 트리거를, JobOperator가 실행을** 맡습니다 — WebFlow Step 7에서
확립한 "트리거와 로직의 분리"가 배치에서도 같은 모양입니다:

```java
@Scheduled(cron = "0 0 2 * * *")
public void runNightly() {
    jobOperator.start("dailySettlementJob", "date=" + today);  // 날짜가 자연스런 멱등키!
}
```

date 파라미터 덕에 같은 날 중복 트리거는 JobInstance 규칙이 막아줍니다 —
파라미터 설계가 곧 중복 실행 방지 설계입니다.

### 3-6. 환경 장애의 연출 — BROKEN 스위치

"코드는 멀쩡한데 바깥(스토리지/네트워크)이 문제"인 실패를 정적 스위치로
연출했습니다. 교보재 규약 그대로: **운영 코드 반입 금지 + Before/AfterEach
정리 의무** (SEEN_THREADS, Sabotage와 같은 계보). 그리고 WebFlow Step 8의
파일 점거, Step 10의 transitionTo처럼 — **장애를 결정적으로 연출하는 것도
테스트 설계 기술**입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.advanced.step17.*"
```

1. `OpsDemoJobConfig` — JobRegistryBeanPostProcessor의 존재 이유 읽기
2. `JobOperatorTest` — 등록 확인 → 문자열 실행 → 재기동 절차 ①~④
3. **일부러 깨뜨려보기**: jobRegistryBeanPostProcessor @Bean을 주석 처리하면?
   (NoSuchJobException — "빈은 있는데 전화번호부에 없다"를 직접 목격)

## 5. Testing — exercise 풀기

`advanced/step17/exercise`의 TODO 1~3을 채우세요. "2일치 일일 배치"의 이력에서
**Instance(작업) 2개 / Execution(시도) 각 1개**를 구분해 세는 것이 채점 포인트 —
Step 3의 두 단어가 운영 어휘였음을 손으로 확인합니다.

## 6. Lessons Learned

### 사례: 재기동했더니 두 배로 입금된 정산

- **증상**: 실패한 정산 배치를 "처음부터 다시" 돌렸더니 일부 거래가 중복 정산됨
- **원인**: restart(이어서)가 아니라 새 파라미터로 start(처음부터) — 이미 커밋된
  chunk가 다시 처리됨
- **해결**: 운영 절차를 restart(실행ID)로 표준화 + Step 12의 체크포인트/멱등 설계 확인
- **교훈**: start와 restart는 다른 동사다. 운영 매뉴얼에 어느 쪽인지 명시하라.

### 사례: 콘솔에선 안 보이는 Job

- **증상**: 관리 콘솔의 Job 목록이 비어 있음 — 코드에는 Job이 8개나 있는데
- **원인**: JobRegistryBeanPostProcessor 미등록 — 레지스트리가 빈 전화번호부
- **해결**: BeanPostProcessor 등록 + `getJobNames()` 등록 확인 테스트 추가
- **교훈**: "빈이 있다"와 "등록되어 있다"는 다르다. 인프라 전제도 테스트로 봉인하라.

### 시니어의 시선

> 배치 운영 인수인계에서 첫 질문: "실패하면 어떻게 재기동하나요?" 이때
> "restart API로, 실행ID 기준으로요"라고 답하면 안심하고, "그냥 다시 돌려요"라고
> 답하면 중복 처리 사고 시나리오부터 점검합니다. 두 번째 질문은 "이력은 어디서
> 보나요?" — BATCH_* 테이블을 직접 SELECT하는 팀과 JobExplorer/콘솔을 쓰는 팀은
> 운영 성숙도가 다릅니다.

## 7. Key Takeaways

- JobOperator = 이름+문자열로 Job을 다루는 운영 창구 (start/restart/summary/이력)
- JobRegistry 등록은 자동이 아니다 — BeanPostProcessor 누락 = NoSuchJobException
- restart는 같은 Instance에 새 Execution을 쌓는다 (Step 3의 어휘 = 운영 이력 구조)
- 파라미터 설계(date=...)가 곧 중복 실행 방지 — JobInstance 규칙의 운영 응용
- 트리거(스케줄러)와 실행(JobOperator)의 분리 — WebFlow Step 7과 같은 철학
- start와 restart는 다른 동사 — 운영 매뉴얼에 명시하라

## 8. Next Steps — 모듈 너머

BatchFlow의 모든 Step(필수 13 + 심화 4)이 끝났습니다. 더 가면:

- **대량 알림 발송 프로젝트** (50-Step 47~50) — Step 16의 비동기를 10만 건 스케일로
- **모니터링 연동** (50-Step 38·40) — 실패 알림(Slack/메일), 대시보드(그라파나)
- **Spring Cloud Data Flow / Quartz 클러스터** — 멀티 서버 배치 오케스트레이션

그리고 가장 좋은 다음 단계 — **여러분 팀의 실제 배치**에 오늘의 질문을 던져보세요:
"실패하면 어떻게 재기동하나요? 이력은 어디서 보나요?"
