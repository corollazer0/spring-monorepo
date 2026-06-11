# [Batch Step 8] JdbcPagingItemReader — 끊어 읽기와 리더 단독 테스트

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `JdbcPagingItemReaderBuilder`, `sortKeys`, `PagingQueryProvider` 자동 감지, 🆕 `StepScopeTestUtils` + `MetaDataInstanceFactory`(리더 단독 테스트)
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/dormant, test/java/com/batchflow/step08}/`

---

## 1. Before We Start — 커서의 약점

Step 7의 커서는 빠르고 단순하지만 두 가지 비용이 있습니다.

1. **긴 점유**: 처리 내내 커넥션/커서를 잡는다 — 느린 처리와 만나면 몇 시간짜리 커넥션
2. **thread-unsafe**: 하나의 커서를 여러 스레드가 당기면 깨진다 — 멀티스레드(Step 14) 불가

대안이 **페이징**: "1페이지 주세요" → 끊고 → "2페이지 주세요". 매 페이지가 독립 쿼리라
커넥션을 오래 잡지 않고, thread-safe합니다. 대가는? **페이지 사이의 순서 보장**이
전적으로 sortKeys에 달리게 됩니다.

| | 커서 (Step 7) | 페이징 (Step 8) |
|---|---|---|
| 쿼리 | 1번 열고 흘리기 | 페이지마다 새 쿼리 |
| 커넥션 점유 | 처리 내내 | 페이지 조회 순간만 |
| thread-safe | ❌ | ✅ (Step 14의 전제) |
| 순서 보장 | 커서가 보장 | **sortKeys가 생명선** |

## 2. What We're Building

```
dormantCandidatePagingScanJob — Step 7과 같은 조회를 "페이징"으로 (page=4, 10명→3페이지)

src/test/java/com/batchflow/step08/
├── example/DormantCandidatePagingScanJobTest.java ← 카운트 + 🆕리더 단독 테스트(내용물!)
├── exercise/PagingReaderExerciseTest.java         ← 빈 결과 / 전원 ACTIVE 검증
└── answer/PagingReaderAnswerTest.java
```

## 3. Core Concepts

### 3-1. 페이징 리더의 조립 — SQL을 조각으로 준다

```java
new JdbcPagingItemReaderBuilder<Member>()
        .name("dormantCandidatePagingReader")
        .dataSource(dataSource)
        .pageSize(4)
        .selectClause("SELECT member_id, name, ...")
        .fromClause("FROM member")
        .whereClause("WHERE status = 'ACTIVE' AND last_login_at < :cutoffDate")
        .parameterValues(params)                                   // :이름 바인딩
        .sortKeys(Collections.singletonMap("member_id", ASCENDING)) // 생명선!
        .rowMapper(new MemberRowMapper())
        .build();
```

왜 SQL을 통째로 안 주고 조각(select/from/where)으로 줄까요?
**페이징 구문(OFFSET/FETCH, TOP, LIMIT...)은 DB마다 달라서**, 리더가 DB에 맞는
페이징 SQL을 **조립해야** 하기 때문입니다. 그 조립공이 PagingQueryProvider이고,
빌더가 DataSource에서 DB 제품을 감지해 자동 선택합니다.

### 3-2. ⚠️ 우리 환경의 함정 — "H2로 감지된다"

우리 H2는 MODE=MSSQLServer지만 **제품명은 H2** → `H2PagingQueryProvider`가 선택됩니다.
실서버(진짜 MS-SQL)에서는 `SqlServerPagingQueryProvider`가 선택되어 **다른 페이징 SQL**이
나갑니다. 즉:

> H2 테스트 통과 = 조회 조건/매핑/카운트 검증 ✅
> 실서버의 페이징 "구문" 검증 ❌ — 첫 운영 배포 전 개발계 MS-SQL에서 1회 확인 필수!

(TestCraft Step 3의 "H2 호환 모드 ≠ 진짜 MS-SQL" 교훈의 배치 버전입니다)

### 3-3. sortKeys — 유일키가 아니면 중복/누락이 생긴다

페이지 2를 요청하는 사이에 정렬 동률 행들의 순서가 바뀌면? 같은 행을 두 번 읽거나
빠뜨립니다. **sortKeys에는 유일성이 보장되는 컬럼(PK)을 반드시 포함** —
TestCraft Step 3의 페이징 교훈과 동일한 원리입니다.

### 3-4. 🆕 리더 단독 테스트 — 카운트 너머 "내용물"을 본다

지금까지의 검증은 카운트(몇 건)였습니다. "**어떤** 행이 **어떤 순서**로"는
리더를 Job 없이 직접 여닫아야 보입니다. 문제: @StepScope Bean은 Step 실행 중에만
존재 — 그래서 spring-batch-test가 가짜 실행 환경을 제공합니다:

```java
StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(파라미터);

List<Long> ids = StepScopeTestUtils.doInStepScope(stepExecution, () -> {
    reader.open(stepExecution.getExecutionContext());
    try { /* read() 반복 수집 */ } finally { reader.close(); }
    return ids;
});

assertThat(ids).containsExactly(21L, ..., 30L);   // 내용물 + 순서까지!
```

이 기법은 커스텀 리더/프로세서를 만들 때의 표준 단위 테스트 도구입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step08.*"
```

1. Config — SQL 조각 조립과 sortKeys, H2 감지 함정 주석
2. example 1 — 카운트가 커서와 동일함 (방식이 달라도 결과는 같아야)
3. example 2 — **리더 단독 테스트**: open/read/close의 수동 생명주기
4. **일부러 깨뜨려보기**: `.sortKeys(...)`를 주석 처리하고 빌드 — 어떤 에러가?
   (sortKeys 없는 페이징은 빌더가 거부한다 — 생명선임을 프레임워크도 안다)

## 5. Testing — exercise 풀기

`step08/exercise/PagingReaderExerciseTest.java`의 TODO 1~5를 채우세요.
"전원 ACTIVE" 검증(TODO 4~5)이 핵심 — DORMANT가 한 건이라도 섞여 읽히면
Step 10에서 **이미 휴면인 회원을 또 전환**하는 사고가 됩니다.

## 6. Lessons Learned

### 사례 1: 페이징 배치가 일부 행을 빼먹는다 (실무 단골 미스터리)

- **증상**: 10만 건 중 수십 건이 처리 누락. 재실행하면 또 다른 행이 누락
- **원인**: sortKeys가 created_at(동률 다수) — 페이지 경계에서 순서가 흔들림
- **해결**: sortKeys에 PK 포함 (created_at, member_id)
- **교훈**: "가끔, 다른 행이" 누락되면 십중팔구 정렬 동률이다.

### 사례 2: 처리 중 새 데이터가 들어오면?

- **증상**: 배치 도는 동안 INSERT된 행이 읽히기도/안 읽히기도
- **원인**: 페이징은 매번 새 쿼리 — 스냅샷이 아니다
- **해결**: "기준 시점"을 WHERE에 박아라 (예: created_at <= :배치시작시각)
- **교훈**: 페이징 리더의 대상 집합은 **불변 조건**으로 고정해야 결정적이다.

### 사례 3: H2에서 통과한 페이징이 실서버에서 문법 오류

- **원인**: 3-2의 함정 — Provider가 다르다 (H2 vs SqlServer)
- **교훈**: 페이징 리더를 새로 쓰면 실 DB에서 생성 SQL을 한 번은 눈으로 확인.

### 시니어의 시선

> 커서냐 페이징이냐는 종교 논쟁이 아닙니다. 제 선택 기준 세 줄:
> **싱글스레드 + 빠른 처리 = 커서** (단순함이 이긴다) /
> **멀티스레드 예정 or 느린 처리 = 페이징** /
> **확신 없으면 페이징** (안전한 기본값). 단, 어느 쪽이든 ORDER BY는 PK 포함.

## 7. Key Takeaways

- 페이징 = 끊어 읽기: 커넥션 점유 짧고 thread-safe (Step 14의 전제)
- SQL을 조각으로 주는 이유 = DB별 페이징 구문을 Provider가 조립
- H2 모드 함정: H2로 감지된다 — 실서버 SQL은 별도 확인 (Provider가 다름!)
- sortKeys는 생명선 — PK 포함 안 하면 중복/누락
- 리더 단독 테스트(StepScopeTestUtils) — 카운트 너머 내용물·순서 검증

## 8. Next Steps — 다음 Step의 문제

읽기는 정복했습니다. 후보 10명을 손에 쥐었는데... 이제 뭘 하죠?

> "후보를 **DORMANT로 바꾸고**(변환), 전환 일시를 찍고, 혹시 모를 비정상 데이터는
> **걸러내고**(검증), 마지막으로 DB에 **반영**해줘(쓰기)"

변환·검증은 ItemProcessor의 일 — 그리고 **processor는 순수 자바라서 TestCraft에서
배운 단위 테스트로 검증**할 수 있습니다(Spring 없이!). 쓰기는 JdbcBatchItemWriter가
묶음 UPDATE로 처리합니다. **Step 9**에서 부품을 만들고, Step 10에서 조립합니다.
