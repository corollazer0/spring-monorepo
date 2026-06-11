# [Web Step 7] 스케줄링 — 시간이 트리거인 코드

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 🆕 @EnableScheduling, @Scheduled(fixedDelay), 시각 주입 설계, 테스트에서 스케줄러 끄기
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow/scheduler, test/java/com/webflow/step07}/`

---

## 1. Before We Start — 재고를 물고 있는 유령 주문

주문 테이블에 PENDING_PAYMENT가 쌓입니다 — 결제창까지 갔다가 이탈한 주문들.
문제는 Step 1의 설계를 떠올리면 보입니다: **주문 시점에 재고를 차감**했죠.
미결제 주문이 취소되지 않으면 그 재고는 **영원히 잠깁니다**. 팔 수 있는 키보드가
유령 주문에 묶여 "품절"로 보이는 거죠.

> "30분 넘게 미결제인 주문은 자동 취소하고 재고를 돌려놔 주세요."

사용자 요청 없이 **시간이 트리거**인 작업 — 그리고 곧바로 테스트 질문이 옵니다:
"10분마다 도는 코드는 어떻게 테스트하죠? 10분 기다리나요?"

## 2. What We're Building

```
OrderCleanupService.cancelStaleOrders(cutoff)   ← 로직 (시각은 파라미터!)
  : cutoff 이전 PENDING_PAYMENT → CANCELLED + 재고 복원 (placeOrder의 역연산)
StaleOrderCleanupScheduler                       ← 트리거 (얇게!)
  : @Scheduled(fixedDelay 10분) → cutoff 계산(now-30분) 후 위임
```

```
src/test/java/com/webflow/step07/
├── example/OrderCleanupServiceTest.java     ← 협력 검증 (취소↔복원 짝)
├── example/OrderCleanupIntegrationTest.java ← 진짜 DB 상태 + 멱등성
├── example/StaleOrderCleanupSchedulerTest.java ← cutoff 계산 + 예외 삼킴
├── exercise/StaleBoundaryExerciseTest.java  ← < vs <= 경계 봉인
└── answer/StaleBoundaryAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 트리거와 로직의 분리 — 이 Step의 전부

나쁜 설계부터 봅시다:

```java
@Scheduled(fixedDelay = 600000)
public void cleanup() {
    LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);  // 로직 안의 now()!
    // ... 조회, 취소, 복원 전부 여기에
}
```

이 메서드를 테스트하려면? 트리거를 기다릴 수 없고, now() 때문에 결과가
실행 시각마다 다릅니다. 해법은 둘로 쪼개기:

- **로직** `cancelStaleOrders(LocalDateTime cutoff)` — 시각을 **파라미터로** 받는다.
  테스트는 원하는 cutoff를 넣어 그냥 부른다 (새벽 3시 로직을 대낮에 1ms 만에!)
- **트리거** `@Scheduled runCleanup()` — cutoff 계산과 위임만. 얇아서 틀릴 게 없다.

BatchFlow에서 본 `spring.batch.job.enabled=false`(자동 실행 끄고 테스트가 직접
Job을 launch)와 정확히 같은 철학입니다.

### 3-2. 🆕 fixedDelay vs fixedRate — 겹침의 문제

| 옵션 | 의미 | 작업이 주기보다 오래 걸리면 |
|---|---|---|
| fixedRate | 이전 실행 **시작** 기준 N ms | 실행이 **겹친다** (같은 주문을 두 스레드가 취소?!) |
| **fixedDelay** | 이전 실행 **종료** 기준 N ms | 밀릴 뿐 절대 안 겹친다 |

정리 작업처럼 "겹치면 위험한" 작업은 fixedDelay가 기본값입니다.
(서버가 2대면? @Scheduled는 **양쪽에서 다 돕니다** — 분산 락(ShedLock 등)이
필요해지는 시점. 지금은 단일 서버 전제만 기억해두세요.)

### 3-3. 취소와 복원은 placeOrder의 역연산 — 그리고 한 운명

```java
@Transactional   // 취소만 되고 복원이 빠지면? 재고가 영원히 잠긴다
public int cancelStaleOrders(LocalDateTime cutoff) {
    for (Order order : orderDao.findStaleOrders(cutoff)) {
        orderDao.updateStatus(order.getOrderId(), CANCELLED, null);
        productDao.restoreStock(order.getProductId(), order.getQuantity());  // 짝!
    }
}
```

테스트도 "짝"으로 검증합니다 — 수량이 **다른** 두 주문(1개, 2개)을 주고
`restoreStock(3L, 1)`, `restoreStock(7L, 2)`가 각각 불렸는지. 수량을 같게 주면
뒤섞여도 통과해버립니다 — **시드를 비대칭으로 설계하는 이유**.

참고로 복원(stock + n)은 차감과 달리 조건(WHERE stock >= ...)이 없습니다 —
더하기는 음수가 될 수 없으니까요. 같은 UPDATE라도 위험이 다르면 방어도 다릅니다.

### 3-4. 멱등성 — 두 번 돌아도 같은 결과

스케줄 작업은 **반드시 다시 돌게 됩니다** (재시작, 장애 복구, 수동 실행...).
통합 테스트의 마지막 단언이 그래서 존재합니다:

```java
assertThat(orderCleanupService.cancelStaleOrders(CUTOFF)).isZero();  // 한 번 더 → 0건
```

첫 실행에서 CANCELLED로 바뀐 주문은 `WHERE status = 'PENDING_PAYMENT'`에 다시
안 걸립니다 — **상태 전이 자체가 멱등성을 만든다**. BatchFlow Step 12(재시작과
멱등성)에서 본 패턴이 스케줄러에서 재등장했습니다.

### 3-5. 테스트에선 스케줄러를 끈다

```yaml
# test/application.yml
app:
  scheduling:
    enabled: false   # @ConditionalOnProperty로 @EnableScheduling 차단
```

안 끄면? @SpringBootTest가 도는 도중 **배경 스레드가 멋대로 정리 작업을 실행** —
verify 횟수가 흔들리고, 실패가 간헐적(flaky)이라 원인 추적이 지옥이 됩니다.
트리거는 운영의 것, 테스트는 로직만 직접 부릅니다.

### 3-6. 스케줄 스레드에서 예외를 새게 하지 마라

`runCleanup`이 try-catch로 감싸는 이유: 스케줄 스레드로 예외가 전파되면
로그도 어중간하고, (설정에 따라) 이후 실행이 위협받을 수 있습니다.
**잡아서, 기록하고, 다음 주기를 기약한다** — 스케줄 작업의 예외 규약입니다.
`assertThatCode(...).doesNotThrowAnyException()`이 그 봉인.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step07.*"
```

1. `OrderCleanupService` — cutoff가 파라미터인 것, @Transactional, 역연산 짝
2. `StaleOrderCleanupScheduler` — 얇음 그 자체 (계산 + 위임 + 예외 삼킴)
3. 통합 테스트 — 시드 2건 정리 + 무사한 것들 + 멱등성
4. **일부러 깨뜨려보기**: cancelStaleOrders에서 restoreStock 줄을 지우면?
   (Mockito 짝 검증과 통합 테스트의 재고 단언, 두 겹이 동시에 깨진다)

## 5. Testing — exercise 풀기

`step07/exercise/StaleBoundaryExerciseTest.java`의 TODO 1~3을 채우세요.
`<` vs `<=` — 경계 **정확히**(0건)와 **1초 뒤**(1건), 두 지점을 모두 짚어야
한 글자 회귀를 막을 수 있습니다. TODO 3은 상태 필터가 시각과 AND로 작동하는지
— PAID가 끼어들면 결제된 주문을 취소하는 대형 사고입니다.

## 6. Lessons Learned

### 사례: 매일 새벽 3시에만 죽는 서버

- **증상**: 운영 서버가 가끔 새벽에 OOM — 낮에는 재현 불가
- **원인**: 스케줄 작업이 전체 테이블을 메모리에 적재 — 데이터가 쌓이자 폭발.
  테스트가 없어서(­"트리거를 어떻게 기다려요") 아무도 로직을 검증한 적이 없었다
- **해결**: 로직을 서비스로 분리해 시각 주입 테스트 작성 + 조회 조건 수정
- **교훈**: "스케줄러라 테스트 못 해요"는 설계 문제다. 로직을 분리하면 테스트는 평범해진다.

### 사례: 두 대가 된 날부터 시작된 이중 취소

- **증상**: 같은 주문에 취소 로그가 2번 — 서버 증설 직후부터
- **원인**: @Scheduled는 서버마다 돈다 — 2대가 같은 작업을 동시에 실행
- **해결**: 분산 락(ShedLock) 도입 + 상태 전이 조건(WHERE status=...)으로 멱등화
- **교훈**: 스케줄 작업은 "여러 번, 동시에" 실행될 수 있다는 전제로 설계하라.

### 시니어의 시선

> 스케줄 작업 리뷰의 첫 질문은 "이거 두 번 돌면 어떻게 되나요?"입니다.
> 멱등하면 합격선, 아니면 설계로 돌려보냅니다. 두 번째 질문은
> "로직을 @Scheduled 없이 부를 수 있나요?" — 못 부르면 테스트가 없다는
> 뜻이고, 테스트 없는 새벽 코드는 시한폭탄입니다.

## 7. Key Takeaways

- 트리거(@Scheduled)와 로직(서비스)을 분리 — 시각은 파라미터로 주입
- fixedDelay = 종료 기준이라 겹치지 않는다 (겹치면 위험한 작업의 기본값)
- 취소+복원은 @Transactional 한 운명 — placeOrder의 정확한 역연산
- 스케줄 작업은 멱등하게 — 상태 전이 조건이 멱등성을 만든다
- 테스트에선 스케줄러를 끈다 (@ConditionalOnProperty) — flaky의 싹 제거
- 스케줄 스레드에 예외를 새게 하지 않는다 — 잡고, 기록하고, 다음 주기

## 8. Next Steps — 다음 Step의 문제

기능은 다 갖췄습니다. 그런데 운영을 시작하면 질문이 바뀝니다:

> "지금 서버 살아있나요?" "PG 연동 정상인가요?" "업로드 디스크 안 찼나요?"
> — 로그 뒤지지 말고 **한 번에 보여주세요.**

서버의 건강 상태를 밖에서 들여다보는 창구 — Spring Boot **Actuator**입니다.
단, 창구를 여는 순간 "어디까지 보여줄 것인가"라는 보안 질문이 따라옵니다.
**Step 8: Actuator**에서 health/metrics 노출 제한과 커스텀 HealthIndicator를 다룹니다.
