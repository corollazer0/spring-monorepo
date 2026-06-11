# [심화 Step 15] Partitioning — 나눠서 정복

> **소요 시간**: 약 1.5시간 (심화 — 필수 트랙 + Step 14 완주 후)
> **이번 Step의 도구**: `Partitioner`, `.partitioner()/.gridSize()/.step()`, `#{stepExecutionContext[...]}` 주입, 파티셔너의 순수 단위 테스트
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/{partitioner, job/partition}, test/java/com/batchflow/advanced/step15}/`

---

## 1. Before We Start — 스레드만으로 부족할 때

Step 14의 멀티스레드에는 한계가 있습니다 — 공유 리더 하나에 스레드가 매달리는 구조라
리더가 병목이 되고, 한 JVM의 자원이 천장입니다. 천만 건이라면?

발상의 전환: **일감(데이터) 자체를 쪼개라.**

```
[Manager]  Partitioner: "1~17은 1번, 18~34는 2번, 35~50은 3번이 맡아라"
              ↓ 각자의 ExecutionContext에 minId/maxId 배포
[Worker×3] 자기 범위만 읽고-처리한다 (각자 자기만의 리더!)
```

이삿짐에 비유하면: 멀티스레드는 "한 트럭의 짐을 여럿이 나르기",
파티셔닝은 "**방별로 담당을 정해** 각자 자기 방만 책임지기"입니다.

## 2. What We're Building

```
partitionedMemberScanJob
  partitionManagerStep (gridSize=3, SimpleAsyncTaskExecutor)
    ├─ partitionWorkerStep:partition0  → member_id 1~17  (17명)
    ├─ partitionWorkerStep:partition1  → member_id 18~34 (17명)
    └─ partitionWorkerStep:partition2  → member_id 35~50 (16명)
```

```
src/main/java/com/batchflow/
├── partitioner/MemberIdRangePartitioner.java
└── job/partition/PartitionedMemberScanJobConfig.java

src/test/java/com/batchflow/advanced/step15/
├── example/PartitionedMemberScanJobTest.java  ← 분할/완전성/분배 3단 검증
├── exercise/PartitionerExerciseTest.java      ← 분할 산수의 순수 단위 (Mockito!)
└── answer/PartitionerAnswerTest.java
```

## 3. Core Concepts

### 3-1. Manager-Worker 구조

```java
stepBuilderFactory.get("partitionManagerStep")
        .partitioner("partitionWorkerStep", memberIdRangePartitioner()) // 나누고
        .step(partitionWorkerStep())                                     // 배포하고
        .gridSize(3)                                                     // 조각 수
        .taskExecutor(new SimpleAsyncTaskExecutor("partition-"))         // 병렬로
        .build();
```

워커의 장부 이름 규약: `워커스텝명:파티션명` (partitionWorkerStep:partition0...) —
파티션별 처리량이 BATCH_STEP_EXECUTION에 **각각** 남아 운영 분석이 쉽다.

### 3-2. 배포 통로는 또 ExecutionContext — Step 5의 마지막 회수

파티셔너가 만든 Map<파티션명, ExecutionContext>의 minId/maxId를
워커 리더가 주입받습니다:

```java
@Value("#{stepExecutionContext['minId']}") Long minId   // jobParameters가 아니다!
```

JobParameters(전 워커 공통)와 stepExecutionContext(워커별 고유)의 구분 —
파라미터는 "무엇을", 파티션 EC는 "어느 조각을".

### 3-3. Step 14와의 결정적 차이 — 공유가 없다

| | Multi-threaded (14) | Partitioning (15) |
|---|---|---|
| 리더 | **공유 1개** → thread-safe 필수(페이징) | 워커마다 **자기 것** → 커서도 안전! |
| 분할 기준 | 없음 (선착순 chunk) | 명시적 (ID 범위 등) |
| 확장 | 한 JVM 내 | **원격 워커(다중 서버)로 확장 가능** |
| 장부 | Step 1개 기록 | 파티션별 기록 (분석 우위) |

### 3-4. 파티셔닝 테스트의 세 기둥 + 분할 산수의 단위 테스트

통합(example): ① 분할(워커 3개) ② **완전성**(합계 = 50 — 겹치면 초과, 빠지면 미달!)
③ 분배(모든 워커 > 0).

그리고 분할 "산수"(나누어떨어지지 않는 범위, 경계 연속성)는 — **DB 없이!**
JdbcTemplate을 Mockito로 바꿔치고 MIN/MAX를 마음대로 주입해 ms 단위로 검증합니다
(exercise). TestCraft Step 2의 Mockito가 배치의 끝에서 다시 등장하는 순간.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.advanced.step15.*"
```

1. Partitioner — 분할 산수와 마지막 조각의 max 클램핑
2. Config — manager/worker 배선과 stepExecutionContext 주입
3. example — 로그에서 partition-1~3 스레드가 각자 범위를 처리하는 모습 확인
4. **일부러 깨뜨려보기**: Partitioner의 `end` 계산에서 `-1`을 빼먹으면?
   (경계 겹침 → 합계 50 초과 → 완전성 검증이 잡는다!) 원복.

## 5. Testing — exercise 풀기

`advanced/step15/exercise/PartitionerExerciseTest.java`의 TODO 1~5를 채우세요.
경계 연속성(이전 max + 1 = 다음 min)의 연쇄 검증이 백미입니다 —
이 등식이 깨지는 순간이 곧 운영의 "중복 처리" 또는 "누락" 사고입니다.

## 6. Lessons Learned

### 사례 1: ID에 구멍이 많은 테이블의 균등 분할 실패

- **증상**: 범위는 균등한데 실제 행 수가 파티션마다 10배 차이 (한 워커만 밤새 일함)
- **원인**: 삭제가 잦아 ID가 듬성듬성 — "ID 범위 균등" ≠ "데이터 균등"
- **해결**: NTILE/ROW_NUMBER 기반 분할, 또는 건수 기반 경계 산출
- **교훈**: 파티셔너는 데이터 분포를 알아야 한다. 분할 전 분포를 측정하라.

### 사례 2: 파티션 경계의 off-by-one

- **증상**: 월말 정산에서 한 건이 두 번 처리됨 (BETWEEN 경계 겹침)
- **교훈**: exercise의 연속성 등식을 파티셔너 테스트의 표준으로 — 산수는 단위로 봉인.

### 시니어의 시선

> 병렬화 3단 사다리를 기억하세요: **튜닝(인덱스/chunk) → Multi-thread → Partitioning.**
> 위 칸일수록 복잡도 이자가 비쌉니다. 그리고 Partitioning을 쓰게 됐다면
> 그 다음 질문은 "이 배치, 애초에 설계(쿼리/모델)가 맞나?"입니다 —
> 가장 빠른 배치는 일을 덜 하는 배치입니다.

## 7. Key Takeaways

- Partitioning = 데이터를 쪼개 워커별 배포 (Manager-Worker)
- 배포 통로는 stepExecutionContext — JobParameters(공통)와 구분하라
- 공유 리더가 없으므로 커서도 안전 — Step 14와의 결정적 차이
- 테스트 3기둥: 분할 수 / **완전성(합계)** / 분배 + 분할 산수는 Mockito 단위로
- ID 범위 균등 ≠ 데이터 균등 — 분포를 알고 나눠라

## 8. Next Steps — BatchFlow 수료 🎓

필수 트랙(1~13)과 심화(14~15)를 모두 완주했습니다. 이제 여러분은:
- Job/Chunk/오류제어/재시작을 갖춘 배치를 설계하고
- 그 모든 것을 테스트로 증명하며
- 시간이 모자라면 병렬화의 사다리를 오를 수 있습니다.

더 깊은 주제(비동기 처리, 성능 측정, JobOperator/스케줄링/모니터링, 대량 알림 프로젝트)는
[50-Step 전체 커리큘럼](../curriculum/00-BatchFlow-Curriculum.md)의 Phase 4~6에 있습니다.
TestCraft에서 배운 테스트 기본기와 여기서 배운 배치 설계 —
이 둘이 실무에서 만나는 곳이 여러분의 다음 프로젝트입니다.
