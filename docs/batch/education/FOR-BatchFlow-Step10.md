# [Batch Step 10] 실전: 휴면회원 전환 Job — 조립과 진짜 커밋

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `JdbcBatchItemWriter`(`beanMapped`), 부품 조립(reader+composite+writer), 자연 멱등성, 🚨 테스트 원상복구 패턴
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/dormant, test/java/com/batchflow/step10}/`

---

## 1. Before We Start — 부품은 다 있다, 조립만 남았다

지난 세 Step에서 만든 것들:

- **리더**(7~8): ACTIVE + 기준일 이전 로그인 후보를 흘려 읽는다
- **프로세서**(9): 검증(ActiveOnly) → 변환(DormantConvert) — 단위 검증 완료
- **Writer 설계도**(9): JdbcBatchItemWriter

이제 진짜 업무 — **회원 10명을 실제로 DORMANT로 바꾸는** Job을 조립합니다.
그리고 처음으로 정면으로 마주합니다: **배치는 진짜로 커밋한다**는 사실이
테스트에 주는 숙제를.

## 2. What We're Building

```
dormantMemberJob (cutoffDate, dormantAt 파라미터)
  reader(커서) → [ActiveOnly 검증 → DormantConvert 변환] → JdbcBatchItemWriter(UPDATE)
  결과: 회원 21~30 → status=DORMANT, dormant_at=2026-06-11T03:00
```

```
src/test/java/com/batchflow/step10/
├── example/DormantMemberJobTest.java            ← 전환 + DB 상태 + 자연 멱등성
├── exercise/DormantJobSideEffectExerciseTest.java ← "건드리지 않은 것" 검증
└── answer/DormantJobSideEffectAnswerTest.java
```

## 3. Core Concepts

### 3-1. JdbcBatchItemWriter — chunk를 batch UPDATE 한 방에

```java
new JdbcBatchItemWriterBuilder<Member>()
        .dataSource(dataSource)
        .sql("UPDATE member SET status = :status, dormant_at = :dormantAt WHERE member_id = :memberId")
        .beanMapped()   // :status ← getStatus(), :dormantAt ← getDormantAt() ...
        .build();
```

chunk 4건이 모이면 JDBC batch로 **한 번에** 나갑니다 — 건건이 UPDATE 대비
네트워크 왕복이 1/4. 대량 배치 성능의 기본기입니다.

### 3-2. 파라미터 → 부품 생성 — Step 3와 9의 합류점

```java
@Bean
@StepScope
public CompositeItemProcessor<Member, Member> dormantMemberProcessor(
        @Value("#{jobParameters['dormantAt']}") String dormantAt) {
    composite.setDelegates(Arrays.asList(
            new ActiveOnlyValidationProcessor(),
            new DormantConvertProcessor(LocalDateTime.parse(dormantAt))));  // 주입!
```

전환 시각은 now()가 아니라 **파라미터**(Step 3)로 들어와 **생성자 주입**(Step 9)됩니다.
참고: @Bean으로 반환하면 afterPropertiesSet()은 Spring이 자동 호출 —
Step 9의 수동 조립과 비교해보세요.

### 3-3. 🚨 배치는 진짜 커밋한다 — 테스트 원상복구 패턴

TestCraft의 @MybatisTest는 자동 롤백이 있었습니다. **배치는 없습니다** —
chunk마다 진짜 커밋이고, 그게 배치의 존재 이유(부분 보존)입니다.
그래서 테스트가 끝나도 회원 21~30은 DORMANT로 남습니다. 처리법:

```java
@BeforeEach
void setUp() {
    jobRepositoryTestUtils.removeJobExecutions();        // 장부 청소 (늘 하던 것)
    jdbcTemplate.update("UPDATE member SET status='ACTIVE', dormant_at=NULL " +
            "WHERE member_id BETWEEN 21 AND 30");        // 🆕 데이터 원상복구!
}
```

TestCraft Step 8(RANDOM_PORT의 @Sql 정리)과 같은 철학 — **커밋되는 테스트는
정리가 내 책임**입니다. 이것을 빼먹으면 "혼자는 통과, 같이 돌리면 실패"의 늪.

### 3-4. 검증 3단 — 카운트, 상태, 그리고 "안 변한 것"

| 단계 | 검증 | 잡는 사고 |
|------|------|----------|
| 카운트 | read 10 / filter 0 / write 10 | 조립 누락, 의도치 않은 필터 |
| DB 상태 | DORMANT 25명, dormant_at 시각 일치 | SQL 오타, 매핑 누락 ("썼다" ≠ "맞게 썼다") |
| **비대상 불변** (exercise) | 1~20 무변화, WITHDRAWN 그대로 | **WHERE 빠진 UPDATE — 최악의 사고** |

대량 UPDATE 배치에서 가장 무서운 건 "대상이 아닌 행을 건드리는 것"입니다.
전환"된" 것만큼 전환"되지 않은" 것의 검증이 중요합니다.

### 3-5. 자연 멱등성 — WHERE가 상태 전이를 따라간다

```
1차 실행: ACTIVE인 21~30 읽힘 → DORMANT로 전환
2차 실행: 21~30은 이제 DORMANT → WHERE status='ACTIVE'에 안 걸림 → 읽을 게 0건!
```

재실행해도 안전한 이유가 코드 어딘가의 if문이 아니라 **조회 조건의 구조**에
있습니다 — "상태 전이를 따라가는 WHERE"는 멱등 배치 설계의 제1원칙입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step10.*"
```

1. Config — 부품 3종이 어떻게 모이는지, 파라미터가 어디로 흘러가는지
2. example — 전환 검증 → **자연 멱등성** (read 0의 의미!)
3. **일부러 깨뜨려보기**: writer SQL에서 `WHERE member_id = :memberId`를 지우면?
   (실행 전에 결과를 예측해보라 — 그리고 exercise의 비대상 검증이 그걸 잡는지 확인. 원복!)

## 5. Testing — exercise 풀기

`step10/exercise/DormantJobSideEffectExerciseTest.java`의 TODO 1~5를 채우세요.
TODO 1(원상복구)을 빼먹으면 어떤 일이 생기는지 일부러 한 번 빼고 전체를 돌려보세요 —
그 경험이 이 Step의 진짜 수업입니다.

## 6. Lessons Learned

### 사례 1: "혼자는 통과, 같이 돌리면 실패" — 배치판

- **증상**: 전환 테스트 단독은 통과, 전체 스위트에선 카운트 불일치
- **원인**: 앞 테스트의 커밋된 전환 결과가 남아 다음 테스트의 출발선 오염
- **해결**: @BeforeEach 원상복구 (장부 청소 + 데이터 복구 세트)
- **교훈**: 자동 롤백이 없는 세계에서 격리는 셀프서비스다.

### 사례 2: WHERE 없는 UPDATE — 전 회원 휴면 사건

- **증상**: (가상이지만 실재하는 악몽) 50명 전원이 DORMANT로
- **원인**: writer SQL 수정 중 WHERE 절 실수 삭제 — 컴파일은 통과한다!
- **방어**: exercise의 "비대상 불변" 검증이 이 사고를 빌드에서 잡는다
- **교훈**: UPDATE/DELETE 배치에는 반드시 "안 변한 것" 테스트를 짝지어라.

### 시니어의 시선

> 멱등성을 후처리(중복 체크 테이블, if문)로 덧대는 설계와, 조회 조건 자체가
> 멱등성을 만드는 설계(상태 전이 WHERE)는 운영 난이도가 하늘과 땅입니다.
> 새 배치를 설계할 때 두 번째 질문은 이것입니다(첫째는 JobInstance 정의, Step 3) —
> **"이 Job을 두 번 돌리면 무슨 일이 생기는가?"** 답이 "아무 일도"가 되도록 설계하세요.

## 7. Key Takeaways

- JdbcBatchItemWriter + beanMapped = chunk를 batch DML 한 방에
- 배치는 진짜 커밋한다 → 테스트 원상복구(@BeforeEach)는 내 책임
- 검증 3단: 카운트 / DB 상태(재조회) / **비대상 불변**
- 자연 멱등성: WHERE가 상태 전이를 따라가게 설계하라
- 부품(단위 검증 완료)의 조립 테스트는 "연결"에 집중한다

## 8. Next Steps — 다음 Step의 문제

완벽해 보이는 이 Job에 비정상 데이터 하나를 흘려봅시다 — 이메일이 깨진 회원,
변환 중 예외를 던지는 한 건. 지금 설계에서는...

**chunk 전체가 롤백되고 Job이 FAILED로 죽습니다.** 1건 때문에 9,999건이 멈추는 것.
실무의 답: "그 한 건은 **건너뛰고(Skip)** 기록해라", "일시적 오류면 **다시 시도(Retry)**해라",
"그리고 무슨 일이 있었는지 **알려라(Listener)**". → **Step 11: 오류 제어**
