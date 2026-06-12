# [심화 Step 16] 비동기 처리 — Processor가 느릴 때

> **소요 시간**: 약 1.5시간 (심화 — 필수 트랙 + Step 14~15 완주 후)
> **이번 Step의 도구**: `AsyncItemProcessor`/`AsyncItemWriter`(spring-batch-integration!), fetchSize/JDBC batch 튜닝, 성능 비교 테스트 설계
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/async, test/java/com/batchflow/advanced/step16}/`
> **50-Step 매핑**: Step 30(Async), 31(성능 측정), 32(DB 튜닝), 34(성능 비교 테스트) 압축

---

## 1. Before We Start — 병목은 어디인가

휴면 회원 15명에게 알림을 발송하는 배치가 있습니다. 건당 외부 알림 API 호출이
20ms — 별것 아닌 것 같지만, 단일 스레드에선 **15건 × 20ms = 최소 300ms**가
구조적 바닥입니다. 회원이 10만 명이라면? **33분**입니다. 읽기도 쓰기도 빠른데요.

Step 14의 멀티스레드 Step으로 풀면 되지 않나? 됩니다 — 단, **대가**가 있었죠:
커서 리더 금지(페이징 강제), saveState 무효, 처리 순서 포기. 병목이 Processor
하나뿐인데 Step 전체를 병렬화하는 건 과잉 처방입니다.

> 병목 진단이 먼저다: 읽기가 느리면 fetchSize/파티셔닝, 쓰기가 느리면 batch INSERT,
> **가공(외부 호출)이 느리면 — 오늘의 주인공, AsyncItemProcessor.**

## 2. What We're Building

```
syncNotificationJob   : reader → [20ms Processor] → writer        (대조군, 바닥 300ms)
asyncNotificationJob  : reader → AsyncItemProcessor(8 threads) → AsyncItemWriter
                          가공만 병렬! 읽기/쓰기는 단일 스레드 그대로
```

```
src/main/java/com/batchflow/
├── processor/NotificationComposeProcessor.java  ← 20ms 지연 가공기 (양쪽 공용!)
└── job/async/{Sync,Async}NotificationJobConfig.java

src/test/java/com/batchflow/advanced/step16/
├── example/AsyncNotificationJobTest.java     ← 정확성 먼저 (카운트/내용/JOIN 교차)
├── example/SyncVsAsyncPerformanceTest.java   ← 성능 비교의 3원칙
├── exercise/AsyncNotificationExerciseTest.java ← 2회 실행, 장부와 DB 동시 검증
└── answer/AsyncNotificationAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 AsyncItemProcessor / AsyncItemWriter — 가공만 병렬로

```java
.<Member, Future<Notification>>chunk(5)   // 출력 타입이 Future다!
.reader(커서 리더 그대로)                   // 읽기는 단일 스레드 — thread-safe 불필요!
.processor(asyncItemProcessor)             // 건마다 Future 즉시 반환, 가공은 풀에서
.writer(asyncItemWriter)                   // 쓰기 직전 Future.get()으로 풀어 위임
```

동작 원리: AsyncItemProcessor는 위임 Processor를 스레드 풀에 던지고 **Future를
즉시 반환**합니다. chunk가 차면 AsyncItemWriter가 각 Future를 **풀어서(get)**
위임 writer에 전달 — 그래서 트랜잭션·카운트 의미가 동기와 같게 유지됩니다.

**Step 14와의 선택 기준** (이 표가 이 Step의 본전):

| | 멀티스레드 Step (14) | AsyncItemProcessor (16) |
|---|---|---|
| 병렬 범위 | chunk 전체 (읽기~쓰기) | **가공만** |
| reader 제약 | thread-safe 필수 (페이징) | **커서 그대로 OK** |
| 어울리는 병목 | 전 구간이 고르게 느림 | **Processor만 느림 (외부 호출 등)** |

함정 하나: 이 두 클래스는 spring-batch-core가 아니라 **spring-batch-integration**
모듈에 있습니다 — 의존성을 모르면 import부터 막힙니다.

### 3-2. 튜닝 2종 — 이미 코드에 깔려 있던 것 (50-Step 32)

```java
.fetchSize(100)              // 튜닝①: DB 왕복당 100행 — 행 단위 왕복 방지
JdbcBatchItemWriter           // 튜닝②: chunk 단위 JDBC batch INSERT
```

fetchSize는 "커서가 한 번에 몇 행을 가져오나", JdbcBatchItemWriter는 "INSERT를
몇 건씩 묶어 보내나"(= chunk 크기) — 둘 다 **왕복 횟수**를 줄이는 무기입니다.
그런데도 이 배치가 느렸던 이유: 병목이 I/O가 아니라 **Processor의 대기**였기
때문. 튜닝은 병목을 맞춰야 효과가 있습니다 — 측정 없는 튜닝은 도박입니다.

### 3-3. 성능 비교 테스트의 3원칙 (50-Step 31·34)

성능 테스트는 flaky의 온상입니다 — CI 머신마다 속도가 다르니까요. 그래서:

1. **변인 통제**: 두 Job이 같은 데이터·같은 Processor·같은 writer를 씁니다.
   다른 건 실행 방식 하나 — 그래야 차이가 "비동기 덕분"이라고 말할 수 있습니다.
2. **바닥 증명**: `sync >= 300ms` 단언 — sleep이 만드는 구조적 바닥이라 머신과
   무관하게 참입니다. 측정 자체가 제대로 됐다는 전제 검증.
3. **상대 비교**: `async < sync` — "비동기는 100ms 미만" 같은 절대값 단언은
   느린 CI에서 깨집니다. 관계만 단언하면 머신 독립적입니다.

### 3-4. 정확성 먼저, 성능은 그 다음

example A가 성능보다 먼저 봉인하는 것: **read 15 / write 15, DB 15건,
수신자 전원 DORMANT(JOIN 교차), 메시지 형식.** 빨라졌는데 두 번 발송되거나
엉뚱한 사람에게 가면 그건 최적화가 아니라 사고입니다.
Step 14에서 "병렬이어도 카운트는 정확히 50"을 먼저 쟀던 것과 같은 순서 —
**성능 개선의 첫 번째 검증은 언제나 정확성.**

### 3-5. JobLauncherTestUtils가 안 될 때

비교 테스트는 Job 빈이 2개라 `@SpringBatchTest`의 JobLauncherTestUtils가
주입에 실패합니다(어느 Job인지 모호). 이럴 땐 **JobLauncher를 직접** 쓰고,
메타데이터 격리는 `removeJobExecutions()` 대신 **유니크 파라미터**로 —
Step 3에서 배운 JobInstance 규칙의 실전 응용입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.advanced.step16.*"
```

1. `NotificationComposeProcessor` — 20ms의 의미 (외부 API 흉내 + 결정성)
2. Sync/Async JobConfig를 나란히 — 차이는 Async 래퍼 2개와 `Future<>` 타입뿐
3. 성능 비교 테스트 로그에서 sync/async 실측값 확인
4. **일부러 깨뜨려보기**: Async 쪽 chunk를 `<Member, Notification>`으로 바꾸면?
   (컴파일 에러 — Future 타입이 설계를 강제한다)

## 5. Testing — exercise 풀기

`advanced/step16/exercise`의 TODO 1~3을 채우세요. 포인트는 **장부(StepExecution)와
DB(COUNT)를 둘 다** 재는 것 — 비동기에서 사고가 나면 그 간극이 첫 단서입니다.

## 6. Lessons Learned

### 사례: 33분짜리 알림 배치의 진짜 범인

- **증상**: 10만 건 알림 배치가 33분 — DBA는 "DB는 한가하다"고 답함
- **원인**: 병목은 건당 20ms의 외부 API 직렬 호출 — fetchSize를 아무리 키워도 무효
- **해결**: AsyncItemProcessor(스레드 풀 16) → 3분대로 단축
- **교훈**: 튜닝 전에 병목 측정. I/O 튜닝은 I/O 병목에만 듣는다.

### 사례: 빨라진 대신 두 번 발송된 알림

- **증상**: 비동기 전환 후 일부 회원에게 알림 중복 발송
- **원인**: 커스텀 비동기 코드가 실패 Future를 재제출하며 성공분까지 다시 실행
- **해결**: 검증된 AsyncItemProcessor/Writer로 교체 + 장부 vs DB 카운트 교차 테스트
- **교훈**: 동시성 코드는 직접 짜지 말고 검증된 부품을 — 그리고 정확성 테스트가 먼저다.

### 시니어의 시선

> "비동기로 바꿨더니 빨라졌어요"라는 PR엔 두 가지를 묻습니다. ① 병목이 Processor라는
> 측정 근거가 있나요? (아니면 멀티스레드 Step이나 파티셔닝이 맞았을 수도)
> ② 정확성 테스트가 성능 테스트보다 먼저 있나요? 순서가 바뀐 최적화는 받지 않습니다.

## 7. Key Takeaways

- 병목 진단이 먼저: 읽기→fetchSize/파티셔닝, 쓰기→batch INSERT, **가공→Async 2종**
- AsyncItemProcessor = 가공만 병렬 — reader는 단일 스레드 유지 (커서 OK, Step 14와의 차별점)
- Async 2종은 spring-batch-integration 모듈 (코어에 없다!)
- 성능 비교 3원칙: 변인 통제 / 구조적 바닥 증명 / 절대값 아닌 상대 비교
- 성능 개선의 첫 검증은 정확성 — 장부와 DB를 함께 잰다
- Job 빈 2개면 JobLauncherTestUtils 대신 JobLauncher + 유니크 파라미터

## 8. Next Steps — 다음 Step의 문제

배치가 빨라졌고 정확합니다. 그런데 운영을 시작하면 다른 질문이 옵니다:

> "어젯밤 정산 배치 실패했다는데, **지금 재기동해 주세요.** 아, 그리고 이번 달
> 실행 이력 좀 뽑아주시고요."

테스트 코드로 Job을 돌리던 우리에게, **운영자가 Job을 제어하는 창구**가 필요해집니다.
JobOperator와 실행 이력 — **심화 Step 17**에서 다룹니다.
