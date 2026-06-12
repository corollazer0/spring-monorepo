# [심화 Step 19] 대량 데이터 성능 실습 — 측정하는 법을 측정한다

> **소요 시간**: 약 1.5시간 (심화 — Step 16 완주 후 권장)
> **이번 Step의 도구**: 🆕 JdbcTemplate.batchUpdate(대량 생성), chunkSize의 JobParameter화(@JobScope), commitCount 검증
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/bulk, test/java/com/batchflow/advanced/step19}/`
> **50-Step 매핑**: Step 31(성능 측정), 32(DB 튜닝 — fetchSize/batchSize), 33(메모리) 압축

---

## 1. Before We Start — "튜닝했더니 빨라졌어요"의 함정

Step 16에서 배운 문장을 떠올려보세요 — "측정 없는 튜닝은 도박이다."
그런데 측정 자체에도 기술이 필요합니다:

- 측정하려면 **대량 데이터**가 있어야 한다 — 50건짜리 시드로는 아무것도 안 보인다.
  그런데 2만 건을 건별 INSERT로 만들면 데이터 준비가 실험보다 오래 걸린다?!
- 측정한 시간을 테스트로 **단언**하면 — CI 머신이 느린 날 빌드가 빨갛다 (flaky)

이번 Step은 도구가 아니라 **방법**을 배웁니다: 대량 데이터를 빠르게 만들고,
시간은 관찰하되, 단언은 머신과 무관한 "구조"로 하는 법.

## 2. What We're Building

```
[실험 A] 같은 5,000건 생성: 건별 INSERT vs JdbcTemplate.batchUpdate — 왕복의 비용
[실험 B] 같은 20,000건 아카이브: chunk 100 vs 2000 — 커밋 횟수는 구조다
          bulkArchiveJob (chunkSize가 JobParameter! — @JobScope Late Binding)
```

전용 무대: `bulk_member`/`bulk_archive` (시드 0건 — 테스트가 생성/정리).
**공유 시드(50명)는 건드리지 않는다** — 카운트 기반 기존 테스트들의 불가침 영역.

## 3. Core Concepts

### 3-1. 🆕 대량 데이터는 "만드는 것"부터 기술 — batchUpdate

```java
jdbcTemplate.batchUpdate("INSERT INTO bulk_member ...", new BatchPreparedStatementSetter() {...});
```

건별 5,000번 `update()` 호출과의 차이는 **왕복(round-trip) 횟수**입니다.
인메모리 H2조차 문장 단위 오버헤드가 구조적으로 쌓입니다 — 네트워크 너머의
실서버라면 그 차이가 수십 배로 벌어집니다. Step 16의 JdbcBatchItemWriter가
빨랐던 이유의 원형이 바로 이것 — **묶어 보내기**.

### 3-2. 🆕 chunkSize를 JobParameter로 — 실험 가능한 배치

```java
@Bean
@JobScope
public Step bulkArchiveStep(@Value("#{jobParameters['chunkSize']}") Long chunkSize) {
    return stepBuilderFactory.get("bulkArchiveStep")
            .<BulkRecord, BulkRecord>chunk(chunkSize.intValue())...
```

chunk 크기는 보통 상수(CLAUDE.md 규칙: 하드코딩 금지, 상수로)지만, **실험할 땐
파라미터**입니다. Step 3의 Late Binding(@JobScope)이 "성능 실험 장치"로 재등장 —
같은 Job을 chunk 100으로도 2000으로도 돌릴 수 있어야 비교가 성립합니다
(변인 통제, Step 16의 1원칙).

### 3-3. 이 Step의 본전 — 시간은 관찰, 단언은 구조

성능 테스트가 flaky해지는 이유는 하나입니다: **머신의 기분을 단언해서**.
이 Step의 규율:

| 지표 | 성격 | 다루는 법 |
|------|------|----------|
| 처리 시간(ms) | 환경 의존 (CI 머신, 그날의 부하) | **로그로 관찰만** |
| 커밋 횟수 | 구조 — ceil(N / chunkSize) | **단언** (머신 무관!) |
| 처리 건수 | 구조 | 단언 |
| 방향성 비교 | 구조적 차이가 압도적일 때만 | 단언 (batchUpdate < 건별 — 왕복 5,000번의 비용은 구조다) |

Step 16에선 sleep이 만든 "구조적 바닥"이 있어 시간 비교를 단언했습니다.
여기엔 바닥이 없으므로 — chunk 100 vs 2000의 시간 차는 **단언하지 않습니다**.
인메모리 H2의 커밋은 너무 싸서 노이즈에 묻히니까요. 대신 그 커밋 수가
실서버(디스크 fsync)에서 곧 시간이 된다는 것을 해설로 남깁니다.

### 3-4. commitCount — 메타데이터 장부의 재등장

`StepExecution.getCommitCount()` — Step 1의 BATCH_* 장부가 기록해온 그 숫자입니다.
chunk가 트랜잭션 경계(Step 6)이므로:

```
커밋 수 ≈ ceil(20,000 / 100)  = 200   (chunk 100)
커밋 수 ≈ ceil(20,000 / 2000) = 10    (chunk 2000)
```

"≈"인 이유: 마지막에 "읽을 게 없음"을 확인하는 **빈 chunk가 한 번 더 커밋**될 수
있습니다(구현 디테일). 그래서 단언은 정확값 대신 **관계식**(`commits100 > 10 ×
commits2000`)이나 **isBetween(40, 41)** — 프레임워크 버전업에 테스트가 인질로
잡히지 않게. 구현 디테일에 강건한 단언, exercise의 채점 포인트입니다.

### 3-5. chunk 크기의 양면 — 크면 무조건 좋은가?

커밋이 적을수록 빠르다면 chunk를 100만으로? 아닙니다:

- **메모리**: chunk만큼의 item이 쓰기 전까지 메모리에 산다 (50-Step 33의 주제)
- **롤백 단위**: 1건 실패 시 chunk 전체 롤백 — 클수록 날아가는 양도 크다 (Step 11)
- **재시작 단위**: 체크포인트가 chunk 경계 — 클수록 재시작이 거칠다 (Step 12)

chunk 크기는 "속도 vs 안정성·메모리"의 트레이드오프 다이얼입니다.
실무 출발점은 수백~수천, 그 다음은 — 측정.

## 4. Step-by-Step

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.advanced.step19.*"
```

1. 실험 A 로그에서 건별/배치 실측치 확인 ("x N배"가 여러분 머신의 숫자)
2. 실험 B 로그에서 커밋 200 vs 10 + 시간 확인 — 시간 차가 작아도 놀라지 말 것 (3-3)
3. **일부러 깨뜨려보기**: ROWS를 100,000으로 올려 실측 — 10만+의 감각.
   (더 큰 설계는 [대량 스키마 문서](../sql/Database-Schema-And-Data.md) 참조) 원복!

## 5. Testing — exercise 풀기

`advanced/step19/exercise`의 TODO 1~3을 채우세요. chunk 500이면 커밋이 몇 번인지
**공식으로 먼저 예측**하고, isBetween(40, 41)로 단언하는 것 — 그리고 왜 정확값이
아닌지 설명할 수 있어야 합니다 (Step 18의 JobParameters 헬퍼 함정도 복습!).

## 6. Lessons Learned

### 사례: 데이터 준비가 3시간 걸린 성능 테스트

- **증상**: 성능 검증 환경 구축 스크립트가 100만 건 적재에 3시간 — 실험은 10분
- **원인**: 건별 INSERT 루프 — 100만 번의 왕복
- **해결**: JDBC batch(+ 실서버는 벌크 로더) — 적재 수 분으로 단축
- **교훈**: 측정의 첫 병목은 측정 대상이 아니라 데이터 준비다.

### 사례: 금요일마다 빨간 성능 테스트

- **증상**: "처리 시간 < 2초" 단언이 CI 부하가 높은 금요일 오후마다 실패
- **원인**: 머신 의존 지표(시간)를 절대값으로 단언
- **해결**: 시간은 로그로, 단언은 커밋 수/처리 건수/왕복 횟수 등 구조 지표로 전환
- **교훈**: 단언할 수 있는 것과 관찰할 것을 구분하는 게 성능 테스트 설계의 절반이다.

### 시니어의 시선

> "chunk 얼마가 좋아요?"라는 질문엔 정답이 없고, "어떻게 정했어요?"라는 질문엔
> 정답이 있습니다 — "측정했어요"가 그것. 이 Step이 준 것은 숫자가 아니라
> 실험 장치(파라미터화된 Job + 구조 지표)입니다. 숫자는 여러분 서버에서 직접 재세요.

## 7. Key Takeaways

- 대량 데이터 생성은 batchUpdate — 왕복 횟수가 비용의 정체
- chunkSize의 JobParameter화(@JobScope) = 실험 가능한 배치 (변인 통제)
- 시간은 관찰(로그), 단언은 구조(commitCount/건수) — flaky의 근절법
- 커밋 수 ≈ ceil(N/chunk), 정확값 대신 관계식/isBetween (구현 디테일에 강건하게)
- chunk 크기는 속도 vs 메모리·롤백·재시작의 트레이드오프 다이얼
- 공유 시드 불가침 — 대량 실습은 전용 테이블에서 (생성/정리는 셀프서비스)

## 8. Next Steps — BatchFlow의 끝, 실무의 시작

심화 트랙의 마지막 조각까지 끝났습니다 (필수 13 + 심화 6).
이제 이 실험 장치를 들고 — **여러분 팀의 실제 배치**에서 재보세요:
"우리 정산 배치의 chunk는 왜 그 숫자인가요? 재본 적 있나요?"
10만+ 스케일의 설계 자료는 `docs/batch/sql/Database-Schema-And-Data.md`에 있습니다.
