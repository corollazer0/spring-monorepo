# [심화 Step 18] 제2 캡스톤: 대량 알림 발송 — 심화 무기의 종합

> **소요 시간**: 약 2.5시간 (설계 30분 + 테스트 작성 2시간)
> **이번 Step의 도구**: 🆕 없음! — 파티셔닝(15) + Skip(11) + 자연 멱등(12)의 조합 판단이 과제
> **코드 위치**: `spring-batch-onboarding/src/{main/java/com/batchflow/job/massnotify, test/java/com/batchflow/advanced/step18}/`
> **50-Step 매핑**: Step 47(대상자) 48(메시지) 49(파티셔닝) 50(통합 테스트) 압축

---

## 1. 진행 방법 — Step 13과 같은, 그러나 한 층 위

제1 캡스톤(13)이 필수 트랙의 졸업 시험이었다면, 제2 캡스톤은 **심화 트랙의
졸업 시험**입니다. 프로덕션은 제공되고, 과제는 테스트 전략 — 단 이번엔
검증 대상이 "계산"이 아니라 **복원력**(부분 실패, 재실행, 분할 처리)입니다.

1. [요구사항 명세](./FOR-BatchFlow-Step18-Requirements.md)를 읽고 시나리오를 스스로 도출
2. warmup exercise로 시동 → 나머지 시나리오를 직접 작성
3. 끝난 뒤 answer 스위트·이 해설과 비교

## 2. What We're Building

```
massNotificationJob (파라미터: campaign)
  massNotifyManagerStep (gridSize=3 — MemberIdRangePartitioner 재사용!)
    ├─ worker:partition0  member_id 1~17  (ACTIVE 17명)
    ├─ worker:partition1  member_id 18~34 (ACTIVE 13명)
    └─ worker:partition2  member_id 35~50 (0명 — 정상)
  worker = reader(ACTIVE + 내 범위 + NOT EXISTS 미발송) → processor(개인화+발송) → writer(이력 적재)
           + faultTolerant.skip(발송실패).skipLimit(5) + SkipListener
```

## 3. 설계 해설 (과제를 마친 뒤 읽으세요)

### 3-1. 병렬화 선택 — 왜 Async(16)가 아니라 Partitioning(15)인가

50-Step 원문(Step 49)도 파티셔닝을 지정했지만, 우리는 Step 16에서 "발송처럼
가공이 느린 배치엔 Async"라고 배웠습니다. 모순일까요? 아닙니다 — **이 Job의
1순위 요구가 성능이 아니라 부분 실패 허용(skip)이기 때문**입니다.

skip은 "어느 item이 실패했는지"를 chunk 처리 흐름 안에서 알아야 작동합니다.
AsyncItemProcessor에선 실패가 Future 안에 숨었다가 **쓰기 시점(언래핑)에**
터집니다 — process-skip의 의미론이 흐려지고 집계가 꼬입니다.
파티셔닝은 워커 안이 평범한 동기 chunk라서 skip이 교과서대로 작동하면서,
병렬성은 워커 수준에서 확보됩니다. **요구사항의 우선순위가 도구를 고른다.**

### 3-2. 자연 멱등의 보상 — 실패 복구가 공짜로 따라온다

reader의 `NOT EXISTS (이 캠페인으로 발송된 이력)` 한 줄이 세 가지를 동시에 해결합니다:

1. 스케줄러 중복 트리거/운영자 재실행 → 0건 읽고 조용히 종료 (R4)
2. 실패자(skip되어 미적재) → 재실행 시 **그들만** 다시 읽힌다 — 복구 절차가 따로 없다 (R5)
3. "발송했는가"의 진실이 코드 상태가 아니라 **DB(이력 테이블)** 에 있다

Step 12의 가르침("상태가 멱등을 만든다")이 발송 도메인에서 완성된 모습입니다.

### 3-3. skipLimit — 관용의 한도가 곧 시스템 문제의 정의

2명 실패는 "그 회원의 문제"(skip하고 계속), 6명 실패는 "발송 시스템의 문제"
(멈추고 사람을 부른다). skipLimit(5)이 그 경계의 선언입니다 — 한도 없는 skip은
**조용한 대량 누락**으로 가는 길입니다.

### 3-4. 테스트에서 만난 실전 함정 2개 (이 캡스톤의 보너스 수업)

**① JobExecution 반환 헬퍼 금지** — `private JobExecution launch(...)` 헬퍼를
만들자 전 테스트가 `Could not create job execution from method: launch`로
사망했습니다. @SpringBatchTest의 JobScopeTestExecutionListener가 "JobExecution을
반환하는 아무 메서드"를 잡 스코프 팩토리로 오인해 인자 없이 호출하려 들기
때문입니다. 헬퍼는 **JobParameters를 반환**하게 하세요.

**② manager Step은 워커 카운트를 집계해 들고 있다** — skip 합계를 전체
StepExecution에서 합산하면 manager(집계분)+워커(원본)로 **이중 계산**됩니다
(2가 4로!). 파티션 Job의 카운트 검증은 워커만 필터해서.

### 3-5. MS-SQL LIKE의 대괄호 함정 (방언 주의)

멱등 키 접두를 `[SUMMER]`처럼 대괄호로 했다면? H2에선 통과하지만 실서버
MS-SQL의 LIKE에서 `[ ]`는 **문자 집합 와일드카드**라 전혀 다른 매칭이 됩니다 —
그래서 접두는 `SUMMER: ` 콜론 구분입니다. "H2 통과 ≠ 실서버 보장"(mybatis-mssql
스킬)의 살아 있는 예시.

## 4. 완주 체크 — BatchFlow 전 과정 졸업 기준

- [ ] Step 1~18의 example/answer가 전부 그린, 모든 exercise를 스스로 통과시켰다
- [ ] 다음을 막힘없이 답할 수 있다:
  - 같은 파라미터 재실행이 거부되는 이유와 운영적 의미는? (3, 17)
  - chunk 카운트 등식(READ = FILTER + WRITE + SKIP)을 설명할 수 있는가? (6, 11)
  - 커서와 페이징 리더, 각각 언제 쓰나? 멀티스레드에선? (7, 8, 14)
  - 병렬화 3단 사다리와 각 단의 대가는? (14, 15, 16)
  - 실패한 배치의 표준 재기동 절차는? start와 restart의 차이는? (12, 17)
  - 부분 실패 허용과 시스템 실패의 경계를 어떻게 설계하나? (11, 18)

## 5. Next Steps — 다음 Step의 문제

설계 판단까지 끝났습니다. 그런데 이 캡스톤의 시드는 30명 — 진짜 "대량"(수만~수십만)
에서는 어떤 일이 벌어질까요? chunk는 얼마가 적당하죠? 그 전에 —
**측정용 대량 데이터는 어떻게 빨리 만들고, 성능 테스트는 어떻게 flaky하지 않게 짜죠?**

측정하는 법 자체를 배우는 **심화 Step 19: 대량 데이터 성능 실습**입니다.
(잔여 참조: 알림/대시보드 연동, Quartz 클러스터는 00 문서로)

그리고 가장 좋은 다음 단계 — **여러분 팀의 실제 배치**에 이 캡스톤의 질문을
던져보세요: "부분 실패하면 어떻게 되나요? 재실행하면 중복되나요?"
