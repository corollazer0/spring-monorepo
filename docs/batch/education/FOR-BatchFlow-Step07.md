# [Batch Step 7] JdbcCursorItemReader — 한 건씩 흘려 읽기

> **소요 시간**: 약 1시간
> **이번 Step의 도구**: `JdbcCursorItemReaderBuilder`, `RowMapper`, `preparedStatementSetter`, `fetchSize`, `@StepScope`
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/dormant, test/java/com/batchflow/step07}/`

---

## 1. Before We Start — queryForList의 함정

> "휴면 후보를 DB에서 읽어와. 단, 10만 명이어도 메모리가 터지면 안 돼"

웹 개발의 습관대로 짜면:

```java
List<Member> all = jdbcTemplate.query("SELECT ... ", rowMapper);  // 전부 메모리로!
```

10만 건이면 수백 MB가 한 번에 힙으로 들어옵니다. 배치의 읽기는 달라야 합니다 —
**양동이로 다 받아오지 말고, 수도꼭지를 틀어두고 컵만 대는 것.**

`JdbcCursorItemReader`는 SELECT를 한 번 열어두고(DB 커서), chunk가 요구하는
만큼만 흘려보냅니다. 메모리에는 `fetchSize`만큼만 머뭅니다.

## 2. What We're Building

```
dormantCandidateScanJob
  reader : SELECT ... WHERE status='ACTIVE' AND last_login_at < :cutoffDate (커서)
  writer : 후보 이름 로깅 (전환은 Step 10에서!)

시드 기준값: cutoffDate=2025-06-11 → 후보 정확히 10명 (회원21~30)
```

```
src/main/java/com/batchflow/job/dormant/DormantCandidateScanJobConfig.java
src/test/java/com/batchflow/step07/
├── example/DormantCandidateScanJobTest.java  ← 10명/3chunk + 빈 결과 정상성
├── exercise/CursorReaderExerciseTest.java    ← SQL 교차 검증 + 경계값
└── answer/CursorReaderAnswerTest.java
```

## 3. Core Concepts

### 3-1. 커서 리더의 조립

```java
new JdbcCursorItemReaderBuilder<Member>()
        .name("dormantCandidateCursorReader")  // 필수! — ExecutionContext 키 (재시작용)
        .dataSource(dataSource)
        .sql("SELECT ... WHERE status = 'ACTIVE' AND last_login_at < ? ORDER BY member_id")
        .preparedStatementSetter(ps -> ps.setString(1, cutoffDate))  // ? 바인딩
        .rowMapper(memberRowMapper())          // 한 행 → 한 객체
        .fetchSize(100)                        // DB가 한 번에 건네주는 묶음
        .build();
```

- `name`은 장식이 아닙니다 — 리더가 "어디까지 읽었는지"를 ExecutionContext(Step 5!)에
  기록할 때 쓰는 키입니다. 재시작(Step 12)의 전제
- `ORDER BY` 필수 — 결정적 순서가 없으면 검증도 재시작도 흔들립니다
- 기준일은 JobParameters(Step 3)로 — 코드 수정 없이 매일 다른 날짜로 실행

### 3-2. fetchSize vs CHUNK_SIZE — 헷갈리는 두 숫자

| | fetchSize (100) | CHUNK_SIZE (4) |
|---|---|---|
| 주체 | JDBC 드라이버 ↔ DB | Spring Batch |
| 의미 | 네트워크로 한 번에 가져오는 행 수 | 트랜잭션/쓰기 묶음 크기 |
| 영향 | 메모리 사용량, 왕복 횟수 | 커밋 횟수, 장애 폭 |

서로 독립적인 다이얼입니다. fetchSize 100 + chunk 4면, 드라이버는 100행을
버퍼링해두고 리더는 거기서 1건씩 꺼내 chunk에 4건씩 채웁니다.

### 3-3. SQL로 거르기 vs Processor로 거르기

DORMANT 15명, WITHDRAWN 5명은 **WHERE가** 걸러냈습니다 — READ_COUNT 자체가 10.
Step 6의 processor 필터(FILTER_COUNT)와 대비하세요:

| | WHERE (읽기 전) | Processor null (읽은 후) |
|---|---|---|
| 카운트 | READ_COUNT에 아예 안 잡힘 | READ엔 잡히고 FILTER_COUNT로 |
| 비용 | DB가 거름 (싸다) | 일단 읽고 버림 (비싸다) |
| 기준 | SQL로 표현 가능한 조건 | 자바 로직이 필요한 조건 |

**거를 수 있으면 최대한 SQL에서** — 단, 복잡한 비즈니스 판단은 Processor의 몫(Step 9).

### 3-4. 빈 결과는 실패가 아니다

후보가 0명인 날도 Job은 COMPLETED여야 합니다 (example의 두 번째 테스트).
"할 일 없음"과 "실패"를 구분하지 못하는 배치는 운영자를 새벽에 깨웁니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step07.*"
```

1. Config — builder의 각 다이얼(name/sql/setter/rowMapper/fetchSize) 역할 확인
2. example — 10명/3chunk, 그리고 빈 결과의 정상 종료
3. **일부러 깨뜨려보기**: SQL에서 `ORDER BY member_id`를 지워도 이 테스트들은 통과한다.
   그런데 왜 필수라고 했을까? (힌트: Step 8의 페이징, Step 12의 재시작 — 순서가 없으면
   "어디까지"가 무의미해진다)

## 5. Testing — exercise 풀기

`step07/exercise/CursorReaderExerciseTest.java`의 TODO 1~5를 채우세요.
백미는 **경계값**: 시드의 로그인 시각이 정확히 2024-01-15 00:00인데
cutoffDate를 2024-01-15로 주면 몇 명일까요? 예측을 적고 실행해보세요.

## 6. Lessons Learned

### 사례 1: "배치가 2시간째 안 끝나요" — fetchSize 기본값

- **증상**: 대량 조회 배치가 비정상적으로 느림
- **원인**: 일부 드라이버의 fetchSize 기본값이 매우 작아(예: 10) 네트워크 왕복 폭증
- **해결**: fetchSize를 명시 (수백~수천). 단, 너무 크면 메모리 압박 — 측정으로 튜닝
- **교훈**: 기본값은 당신의 데이터 규모를 모른다.

### 사례 2: 커서를 오래 열어두다 DB와 싸움

- **증상**: 장시간 배치 중 "connection reset" / 커서 타임아웃
- **원인**: 커서 방식은 **처리 내내 커넥션을 점유**한다 — 처리당 수 초씩 걸리는
  느린 chunk와 만나면 몇 시간짜리 커넥션이 된다
- **해결**: 그것이 다음 Step의 주제 — 끊었다 다시 붙는 **페이징 리더**
- **교훈**: 커서는 빠르고 단순하지만 "긴 점유"라는 비용이 있다.

### 시니어의 시선

> 리더 검증의 황금률: **리더 자신의 말을 믿지 마라.** READ_COUNT 10은 "내가 10건
> 읽었다"는 자기 주장일 뿐입니다. exercise처럼 같은 조건의 독립 SQL COUNT와
> 교차 검증해야 "WHERE가 의도와 일치한다"가 증명됩니다. 자기가 만든 자로
> 자기를 재는 테스트는 가짜입니다.

## 7. Key Takeaways

- 커서 리더 = 수도꼭지: SELECT 1회 + chunk가 달라는 만큼 흘리기 (메모리는 fetchSize만)
- `.name()`은 재시작용 ExecutionContext 키 / ORDER BY는 결정성의 전제
- fetchSize(드라이버 버퍼)와 CHUNK_SIZE(트랜잭션 묶음)는 독립된 다이얼
- 거를 수 있으면 SQL(WHERE)에서 — READ_COUNT에 아예 안 잡히게
- 빈 결과(0건)는 COMPLETED — "할 일 없음"과 "실패"를 구분하라

## 8. Next Steps — 다음 Step의 문제

커서의 약점이 드러났습니다 — **처리 내내 커넥션/커서를 점유**합니다.
처리가 느리거나, 멀티스레드(Step 14)로 읽고 싶다면? (커서는 thread-safe하지 않다!)

대안: "1페이지 주세요" → 끊고 → "2페이지 주세요" — 매번 새 쿼리로 끊어 읽는
**JdbcPagingItemReader**. 그리고 여기엔 멋진 함정이 하나 숨어 있습니다:
페이징 SQL은 DB마다 다른데(H2 vs MS-SQL), 그걸 누가 만들어줄까요? → **Step 8**
