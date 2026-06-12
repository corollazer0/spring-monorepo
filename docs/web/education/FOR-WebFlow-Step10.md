# [Web Step 10 · 심화] 서킷 브레이커 — 죽은 서버를 두드리지 않는 법

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 🆕 Resilience4j CircuitBreaker (1.7.x — Java 8 호환 최종 라인), 상태 주입 테스트
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow/external/delivery, test/java/com/webflow/step10}/`
> **전제**: 필수 Step 1~9 완주 (특히 Step 4의 재시도)

---

## 1. Before We Start — 재시도의 사각지대

Step 4의 재시도는 훌륭하지만, 전제가 있었습니다: **장애가 "일시적"이라는 것.**
PG나 배송사가 30분짜리 전면 장애에 빠지면 어떻게 될까요?

- 모든 요청이 재시도 3회 + 백오프를 **풀코스로** 겪고 실패한다
  (요청당 타임아웃 5초 × 3회 + 백오프 = 사용자는 최악 15초를 기다려 503을 받는다)
- 그 30분 내내 우리는 **죽은 서버에 3배의 트래픽**을 쏟아붓는다 — 회복을 방해하면서

이미 죽은 게 확실한데 왜 계속 두드리나요? 누전이 감지되면 회로를 내려버리는
두꺼비집처럼 — **호출 자체를 차단**하는 장치가 서킷 브레이커입니다.

## 2. What We're Building

```
DeliveryClient (Step 9 산출물 강화):
  retry( circuitBreaker( http ) )
  최근 10회 중 실패율 50% ↑ → OPEN (10초간 즉시 거절, HTTP 0회)
  → HALF_OPEN (탐사 2회 허용) → 성공 시 CLOSED / 실패 시 다시 OPEN
```

```
src/test/java/com/webflow/step10/
├── example/DeliveryCircuitBreakerTest.java ← 3상 전부: CLOSED/OPEN전이/즉시차단/회복
├── exercise/HalfOpenExerciseTest.java      ← 반열림 재실패 → 재OPEN
└── answer/HalfOpenAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 상태 기계 — 차단기의 3상

```
        실패율 ≥ 50% (최근 10회)
CLOSED ─────────────────────────→ OPEN
  ↑                                 │ waitDuration(10s) 경과
  │ 탐사 모두 성공                    ↓
  └────────────────────────── HALF_OPEN ──→ (탐사 실패) 다시 OPEN
```

| 상태 | 의미 | 호출은? |
|---|---|---|
| CLOSED | 평상시 | 통과 (결과만 기록) |
| OPEN | 장애 확정 | **즉시 거절** — HTTP 자체가 안 나간다 (CallNotPermittedException) |
| HALF_OPEN | 회복 탐사 | 제한된 수(2회)만 통과시켜 간을 본다 |

핵심 차이를 명확히: **재시도는 "한 요청을 살리는" 기술, 차단기는 "시스템 전체를
살리는" 기술**입니다. 재시도가 개별 요청 안에서 작동한다면, 차단기는 요청들
**사이에서** 학습합니다 — 그래서 상태(메모리)를 가집니다.

### 3-2. 설정값 하나하나가 의사결정이다

```java
CircuitBreakerConfig.custom()
    .slidingWindowSize(10)                 // 최근 10회를 관찰
    .minimumNumberOfCalls(10)              // 표본 10개 전엔 판단 보류 (성급한 차단 방지)
    .failureRateThreshold(50)              // 절반이 죽으면 장애로 본다
    .waitDurationInOpenState(Duration.ofSeconds(10))   // 10초 후 회복 탐사
    .permittedNumberOfCallsInHalfOpenState(2)          // 탐사는 2번만 (탐사도 폭격이 될 수 있다!)
    .recordExceptions(ResourceAccessException.class, HttpServerErrorException.class)
    .build();
```

`recordExceptions`에 주목 — Step 4의 재시도 대상 선별과 같은 고민입니다.
**404(송장 미등록)는 실패로 세지 않습니다**: 배송사가 멀쩡히 응답한 정상 호출이니까요.
비즈니스 거절을 실패로 세면, 미등록 송장 조회가 몰리는 것만으로 회로가 열립니다.

`minimumNumberOfCalls`도 의사결정입니다: 첫 호출 1번 실패 = 실패율 100%라고
회로를 열어버리면 안 되니, **표본이 모일 때까지 판단을 보류**합니다.

### 3-3. retry( circuitBreaker( http ) ) — 합성 순서의 의미

```java
retryTemplate.execute(context ->
        circuitBreaker.executeSupplier(() ->
                restTemplate.getForObject(...)));
```

차단기가 안쪽이라 **시도 하나하나가 기록**됩니다 — 장애 누적이 빠르죠.
그리고 회로가 열리면 `CallNotPermittedException`이 터지는데, 이것은 retryOn
목록에 **없으므로** 재시도 없이 즉시 실패합니다. "차단됐는데 재시도로 또
두드리는" 모순이 타입 수준에서 차단되는 설계입니다.

### 3-4. 상태를 가진 객체의 테스트 — 두 가지 기법

**① reset() — 상태 격리.** 차단기는 테스트 사이에 살아남습니다 (Step 6 캐시와
동일). 앞 테스트가 열어둔 회로가 뒷 테스트를 오염시키지 않게 `@BeforeEach`에서
`reset()` — 살아남는 상태는 직접 격리한다, 세 번째 반복입니다 (캐시 clear,
배치 removeJobExecutions, 그리고 이것).

**② transitionTo...() — 상태 주입.** OPEN 후 10초를 기다렸다가 HALF_OPEN을
테스트할 건가요? Step 7에서 새벽 3시를 기다리지 않았듯, 시간 의존은 주입으로
대체합니다:

```java
circuitBreaker.transitionToOpenState();
circuitBreaker.transitionToHalfOpenState();   // 10초 대기 없이 탐사 상태로
```

**③ 그리고 "0회"의 증명.** OPEN 테스트에서 mock 서버에 기대 선언이 **0개**인 것 —
요청이 하나라도 나가면 그 자리에서 실패합니다. "호출이 없었다"를 적극적으로
봉인하는 것, Step 3의 never()와 같은 정신입니다.

### 3-5. 산수 검증 — 정확히 10번에서 멈추는가

OPEN 전이 테스트의 호출 산수: track 4번 = 시도 3+3+3+1 = **딱 10번**.
10번째 실패가 기록되는 순간 회로가 열리고, 4번째 track의 재시도 2회차는
HTTP 없이 차단됩니다. `times(10)` 선언 + `verify()`가 "11번째 폭격은 없었다"를
봉인합니다 — Step 4에서 배운 "기대 선언이 곧 검증"의 응용편.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step10.*"
```

1. `DeliveryClient` — 설정값 7개를 하나씩 "왜?"와 함께 읽기
2. `DeliveryCircuitBreakerTest` — 3상 + 전이 4면
3. **일부러 깨뜨려보기**: recordExceptions에서 HttpServerErrorException을 빼면
   어떤 테스트가 깨질까? (5xx가 실패로 안 세져 회로가 안 열린다 — OPEN 전이 테스트)

## 5. Testing — exercise 풀기

`step10/exercise/HalfOpenExerciseTest.java`의 TODO 1~4를 채우세요.
example이 봉인한 "반열림 → 성공 → 닫힘"의 대칭, **"반열림 → 실패 → 재OPEN"**입니다.
상태 기계는 모든 전이가 검증되어야 완성 — 한쪽 문만 잠그면 안 됩니다.

## 6. Lessons Learned

### 사례: 장애 30분, 복구 3시간

- **증상**: PG 장애는 30분 만에 끝났는데 서비스 정상화는 3시간 걸림
- **원인**: 장애 중 쌓인 재시도 트래픽이 PG 복구 직후 한꺼번에 몰려 PG를 다시
  쓰러뜨림(복구 직후 재붕괴 반복) — 차단 없는 재시도의 후폭풍
- **해결**: 서킷 브레이커 도입 — OPEN 동안 트래픽이 끊기고, HALF_OPEN 탐사가
  점진적 복귀를 만든다
- **교훈**: 재시도는 미시(한 요청), 차단기는 거시(시스템) — 외부 연동의 완성형은 둘의 조합.

### 사례: 미등록 송장 이벤트가 연 회로

- **증상**: 대형 프로모션 날 배송 조회가 전부 503 — 배송사는 멀쩡했다
- **원인**: 주문 폭증 직후라 대부분 "송장 미등록(404)" — 이걸 실패로 세서 회로 OPEN
- **해결**: recordExceptions를 인프라 장애(타임아웃·5xx)로 한정
- **교훈**: 비즈니스 결과(404·거절)를 실패율에 넣으면 정상 트래픽이 회로를 연다.
  Step 3의 "거절은 오류가 아니다"가 차단기에서 한 번 더.

### 시니어의 시선

> 차단기 도입 리뷰에서 보는 순서: ① recordExceptions가 인프라 장애로 한정됐는가
> ② minimumNumberOfCalls가 있는가(1실패=차단 방지) ③ HALF_OPEN의 양방향 전이가
> 테스트됐는가 ④ OPEN일 때 "HTTP 0회"가 증명됐는가. 그리고 마지막 질문 —
> "이 임계값들, 운영 지표 보고 다시 조정할 계획 있나요?" 차단기 설정은
> 한 번 쓰고 끝나는 값이 아니라 운영하며 튜닝하는 값입니다.

## 7. Key Takeaways

- 재시도는 한 요청을, 차단기는 시스템을 살린다 — 외부 연동의 완성형은 조합
- 3상 상태 기계: CLOSED(기록) → OPEN(즉시 거절, HTTP 0회) → HALF_OPEN(제한 탐사)
- recordExceptions는 인프라 장애만 — 비즈니스 404/거절을 세면 정상 트래픽이 회로를 연다
- CallNotPermittedException은 재시도 대상에서 제외 — 차단과 재시도의 모순 방지
- 상태 가진 객체의 테스트: reset()으로 격리 + transitionTo()로 시간 대체 + 기대 0개로 "0회" 증명
- 1.7.x = Java 8 호환 최종 라인 (2.x는 Java 17) — 버전 선택도 제약 관리다

## 8. Next Steps — 모듈 너머

WebFlow의 모든 Step이 끝났습니다. 여기서 더 가면:

- **Resilience4j Bulkhead/RateLimiter** — 차단기의 형제들 (동시성 격리, 유량 제한)
- **WebClient** — Boot 3 시대의 논블로킹 클라이언트 (RestTemplate의 후계)
- **Redis 캐시 / ShedLock** — Step 6·7의 "서버 2대" 문제의 해법

그리고 가장 좋은 다음 단계는 — **여러분 팀의 실제 외부 연동 코드**에 오늘의
체크리스트(타임아웃/재시도/차단기/테스트 4종)를 들이대 보는 것입니다.
