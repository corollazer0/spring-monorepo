# [Batch Step 6] Chunk 모델 첫 경험 — 대량 처리의 심장

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `<I,O>chunk(SIZE)`, `ItemReader/Processor/Writer`, `ListItemReader` + `@StepScope`, READ/FILTER/WRITE/COMMIT_COUNT, 도메인 스키마(schema.sql/data.sql)
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/chunk, test/java/com/batchflow/step06}/`

---

## 1. Before We Start — Tasklet 하나로 10만 건?

> "회원 10만 명의 상태를 갱신해줘"

Tasklet으로 짜면 두 가지 방식 모두 재앙입니다.

1. **전부 조회 후 루프**: 10만 건이 메모리에 → OOM의 길
2. **하나의 트랜잭션**: 5만 번째에서 죽으면 → **전체 롤백**, 4시간이 증발

뷔페에 비유하면, 음식 전체를 한 번에 옮기려다 쟁반째 엎는 것입니다.
**접시 단위로 나르세요** — 한 접시(chunk)를 옮길 때마다 확정(commit)하면,
도중에 넘어져도 이미 나른 접시들은 안전합니다.

```
[Chunk 모델]  reader(1건씩 읽기) → processor(1건씩 가공/필터) → writer(접시째 쓰기)
              └────────────── chunk 1개 = 트랜잭션 1개 = 커밋 1번 ──────────────┘
```

## 2. What We're Building

첫 경험은 **DB 없이 숫자 1~10**으로 — chunk의 기계 장치에만 집중합니다.
(그리고 이번 Step에서 이후 모든 Step이 쓸 **도메인 스키마**도 깔아둡니다)

```
firstChunkJob: 1~10 읽기 → 홀수 필터(null) + 짝수×10 → 3개씩 묶어 쓰기

src/main/resources/schema.sql / data.sql    ← 도메인: member 50 / bank_transaction 15 / settlement
src/main/java/com/batchflow/
├── domain/Member.java                       ← 순수 POJO (JPA 아님)
└── job/chunk/FirstChunkJobConfig.java

src/test/java/com/batchflow/step06/
├── example/FirstChunkJobTest.java     ← 카운트 4종 검증 + @StepScope의 가치
├── example/DomainSeedTest.java        ← 시드 수치 봉인 (휴면대상 10명, 정산 9건...)
├── exercise/ChunkCountExerciseTest.java
└── answer/ChunkCountAnswerTest.java
```

## 3. Core Concepts

### 3-1. 카운트 4종 — Chunk의 모든 것이 숫자에 있다

입력 1~10, chunk(3), 홀수 필터일 때 **읽기 전에 예측해보세요**:

| 카운트 | 값 | 의미 |
|--------|----|------|
| READ_COUNT | 10 | reader가 돌려준 건수 |
| FILTER_COUNT | 5 | processor가 **null로 버린** 건수 (예외 아님!) |
| WRITE_COUNT | 5 | writer에 도달한 건수 |
| COMMIT_COUNT | 4 | 3+3+3+1 — **마지막 1건도 한 chunk** |

Step 1에서 "존재만 확인"했던 그 컬럼들이 드디어 채워집니다.
**Chunk 테스트의 표준 = 카운트 검증** — 이후 모든 Step의 무기입니다.

### 3-2. 필터링 — null 반환은 "조용히 버리기"

```java
return number % 2 != 0 ? null : number * 10;   // null = 이 건은 버린다
```

- FILTER_COUNT에 집계되고, writer에 가지 않는다. **예외/실패가 아니다**
- "처리 대상이 아님"(필터)과 "처리하다 죽음"(Skip, Step 11)은 완전히 다른 개념 —
  이 구분이 흐려지면 운영 카운트 분석이 산으로 간다

### 3-3. ⚠️ @StepScope — 상태를 가진 reader의 생명주기

`ListItemReader`는 "어디까지 읽었는지" 인덱스를 **상태로** 가집니다.
싱글톤 Bean이면 첫 실행이 다 소진 → 두 번째 실행은 READ_COUNT 0!

```java
@Bean
@StepScope   // Step 실행마다 새로 생성 — 상태 있는 reader의 필수 장치
public ListItemReader<Integer> numberReader() { ... }
```

example의 두 번째 테스트가 이것을 봉인합니다. **상태를 가진 reader/writer에는
@StepScope** — Step 7부터의 DB reader들도 같은 규칙을 따릅니다.

### 3-4. 도메인 스키마 — 이후 7개 Step의 무대

| 테이블 | 시드 | 쓰임 |
|--------|------|------|
| member | 50명 (ACTIVE 30 = 최근 20 + **휴면대상 10** / DORMANT 15 / WITHDRAWN 5) | Step 7~12 휴면 전환 |
| bank_transaction | 15건 (**06-10자 9건** = 정산 시나리오) | Step 13 정산 캡스톤 |
| settlement | 0건 | 캡스톤이 채운다 |

`DomainSeedTest`가 핵심 수치(휴면대상 10, 정산 9건/5명)를 봉인합니다 —
이 수치가 이후 Step들의 기대값이므로, 시드를 고치면 이 테스트가 먼저 알려줍니다.

테이블명이 `bank_transaction`인 이유: MS-SQL에서 TRANSACTION은 예약어
(`BEGIN TRANSACTION`) — 예약어를 피해 짓는 것도 실무 설계입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step06.*"
```

1. `FirstChunkJobConfig` — chunk(3)의 제네릭 <읽는 타입, 쓰는 타입>과 3박자 조립
2. example A 실행 — **로그에서 Writer 묶음 크기([2건], [2건], [1건]...)를 눈으로 확인**
3. example B — 시드 수치가 어떻게 이후 Step의 "기준값"이 되는지
4. **일부러 깨뜨려보기**: `@StepScope`를 지우고 example A의 두 번째 테스트 실행 —
   READ_COUNT가 0이 되는 순간을 목격하라 (원복!)

## 5. Testing — exercise 풀기

`step06/exercise/ChunkCountExerciseTest.java`의 TODO 1~4를 채우세요.
같은 카운트를 **메모리 객체**(StepExecution)와 **장부**(SQL) 양쪽에서 확인하는 것이
과제입니다 — 운영에서는 객체가 없습니다. 장부를 읽는 능력이 진짜 실력입니다.

## 6. Lessons Learned

### 사례 1: chunk size를 늘렸더니 빨라졌는데... 장애 폭도 커졌다

- **상황**: chunk 100 → 10000으로 변경, 커밋 횟수 감소로 처리 속도 향상
- **부작용**: 한 건의 오류로 롤백되는 범위도 10000건. 메모리 사용량도 비례 증가
- **교훈**: chunk size = 성능과 장애 폭의 트레이드오프. 그래서 상수로 빼두고
  (CHUNK_SIZE) 환경별 튜닝 대상으로 관리한다 — 하드코딩 금지 규칙의 이유.

### 사례 2: 필터와 Skip을 혼동한 카운트 분석

- **증상**: "왜 READ 10에 WRITE 5죠? 5건이 실패했나요?!" — 운영 중 오보
- **원인**: FILTER_COUNT(의도적 제외)를 실패로 오해
- **교훈**: READ = FILTER + WRITE + SKIP. 등식으로 외워라. 필터는 설계, Skip은 사고다.

### 시니어의 시선

> Chunk 모델을 배우면 모든 것을 chunk로 만들고 싶어집니다. 기준은 간단합니다 —
> **"건 단위 반복이 있는가?"** 파일 하나 지우기, 플래그 하나 갱신 같은 단발 작업은
> Tasklet이 정답입니다. 모델 선택의 기준을 갖는 것, 그것이 이 Step의 진짜 졸업장입니다.

## 7. Key Takeaways

- Chunk = 접시 단위 운반: chunk 1개 = 트랜잭션 1개, 마지막 자투리도 한 chunk
- 카운트 등식: READ = FILTER + WRITE (+ SKIP) — 테스트와 운영 분석의 공통 언어
- processor의 null = 조용한 필터 (예외 아님, Skip과 구분!)
- 상태 있는 reader에는 @StepScope — 없으면 두 번째 실행이 굶는다
- 시드 수치(휴면대상 10명, 정산 9건)는 테스트로 봉인 — 이후 Step들의 기준값

## 8. Next Steps — 다음 Step의 문제

기계 장치는 익혔습니다. 이제 **진짜 데이터**를 읽을 차례 — 그런데 문제가 있습니다.

> "휴면 후보 회원을 DB에서 읽어와. 단, **10만 명이어도** 메모리가 터지면 안 돼"

`jdbcTemplate.queryForList(...)`는 전부 메모리에 올립니다. 필요한 것은
**한 건씩 흘려보내는 수도꼭지** — DB 커서를 쥐고 chunk가 달라는 만큼만 흘리는
`JdbcCursorItemReader`입니다. **Step 7**에서 휴면 후보 조회로 직접 만들어봅니다.
