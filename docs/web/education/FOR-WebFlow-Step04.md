# [Web Step 4] 타임아웃·재시도·장애 격리 — 외부 장애에서 살아남기

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 🆕 타임아웃(connect/read), RetryTemplate + 백오프, 503 번역, ExpectedCount/순차 expect로 재시도 횟수 검증
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow/external/payment, test/java/com/webflow/step04}/`

---

## 1. Before We Start — PG가 멈추면 우리도 멈춘다?

Step 3의 PaymentClient에는 타임아웃이 없었습니다. 그 구멍으로 들어올 장애의
시나리오는 이렇습니다:

1. PG가 점검/장애로 응답을 안 준다 (연결은 되는데 답이 없음)
2. 결제 요청 스레드가 **무기한** 기다린다
3. 동시에 결제가 100건 → 톰캣 워커 스레드 200개가 순식간에 묶인다
4. **상품 조회까지 전부 멈춘다** — 외부 장애가 우리 전면 장애가 됐다

남의 장애가 내 장애가 되는 것, 이것이 외부 연동의 진짜 위험입니다.
이번 Step은 그 전염을 끊는 3종 세트 — **제한하고(타임아웃), 견디고(재시도),
끊는다(503 격리)**.

## 2. What We're Building

```
PaymentClient 강화:
  타임아웃: connect 3s / read 5s (yml 프로퍼티)
  재시도: 최대 3회, 지수 백오프 100ms→200ms (타임아웃·5xx만! 4xx는 제외)
  번역: 재시도 끝 실패 → ExternalServiceException → 503 (주문은 PENDING 보존)
```

```
src/test/java/com/webflow/step04/
├── example/PaymentClientRetryTest.java  ← 재시도의 세 얼굴: 회복/포기/비재시도
├── example/PaymentOutageApiTest.java    ← 503 번역 (@WebMvcTest)
├── exercise/PaymentOutageExerciseTest.java ← 장애 시 주문 보존 (Mockito)
└── answer/PaymentOutageAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 타임아웃 — 기다림에 상한선을

```java
builder.rootUri(baseUrl)
       .setConnectTimeout(Duration.ofMillis(3000))   // 연결 수립까지
       .setReadTimeout(Duration.ofMillis(5000))      // 연결 후 첫 응답까지
       .build();
```

- **connect** = 상대 서버에 닿기까지 (서버 다운/네트워크 단절이면 여기서 끊김)
- **read** = 닿은 후 응답이 오기까지 (서버가 살아있지만 느릴 때 여기서 끊김)

값은 yml 프로퍼티로 — 환경(개발/운영)마다 다르게, 배포 없이 조정 가능하게.
초과 시 `ResourceAccessException`이 터집니다. **타임아웃 없는 외부 호출은
스레드 풀 고갈 예약**입니다 — 이 모듈의 절대 규칙.

### 3-2. 🆕 RetryTemplate — 일시적 장애는 견딘다, 단 백오프와 함께

네트워크는 가끔 출렁입니다. 0.1초 뒤에 다시 보내면 멀쩡히 성공할 실패로
사용자에게 에러를 보여주는 건 아깝죠. 그래서 재시도 — 단, 규칙이 있습니다:

```java
RetryTemplate.builder()
    .maxAttempts(3)                        // 상한 필수 — 무한 재시도는 자해
    .exponentialBackoff(100, 2.0, 1000)    // 100ms → 200ms (점점 길게)
    .retryOn(Arrays.asList(ResourceAccessException.class,   // 타임아웃·연결 실패
                           HttpServerErrorException.class)) // 5xx
    .build();
```

**백오프 없는 재시도는 금지** — 장애로 허덕이는 서버를 일정 간격으로 더 두드리면
회복을 방해합니다(자기 DDoS). 간격을 점점 늘리는 게 지수 백오프.

**무엇을 재시도하나**가 더 중요합니다:

| 실패 | 재시도? | 이유 |
|---|---|---|
| 타임아웃/연결 실패 | ✅ | 일시적일 가능성 — 다음 시도는 성공할 수 있다 |
| 5xx (서버 오류) | ✅ | 상대의 일시 장애일 가능성 |
| 4xx (요청 오류) | ❌ | **우리 요청이 잘못됐다 — 백 번 보내도 똑같다** |

그리고 결제 같은 **POST 재시도엔 전제가 있습니다**: 첫 요청이 사실 PG에 도달했고
타임아웃만 났다면, 재시도는 같은 결제를 두 번 요청하는 셈. 실무 PG는 주문번호를
**멱등키**로 받아 중복 승인을 막아줍니다 — 그 계약이 있어야 안전한 재시도입니다.

### 3-3. 🆕 재시도 횟수를 테스트로 봉인 — 기대 선언이 곧 검증

"재시도가 3번 일어났다"를 어떻게 증명할까요? MockRestServiceServer에서는
**기대를 몇 번 선언했는지가 곧 검증**입니다:

```java
// 시나리오형: 실패→실패→성공 (각 호출의 응답을 순서대로 연출)
server.expect(requestTo(URL)).andRespond(withServerError());
server.expect(requestTo(URL)).andRespond(withServerError());
server.expect(requestTo(URL)).andRespond(withSuccess(...));

// 횟수형: 같은 응답 N번
server.expect(times(3), requestTo(URL)).andRespond(withServerError());

server.verify();   // 더 많이 불러도, 덜 불러도 실패
```

타임아웃은 `withException(new SocketTimeoutException(...))`으로 연출합니다 —
진짜로 5초 기다리는 게 아니라 "타임아웃이 났을 때"의 동작을 검증하는 것.

### 3-4. 실패의 위계 — 거절(400) vs 장애(503), 그리고 보존

이제 결제 실패가 세 갈래로 완성됩니다:

| 사건 | 정체 | HTTP | 주문 상태 |
|---|---|---|---|
| DECLINED | 비즈니스 거절 (카드 문제) | 400 | PENDING 보존 — 다른 카드로 재시도 |
| 재시도 끝 장애 | 인프라 사건 | **503** | PENDING 보존 — 잠시 후 재시도 |
| PG가 4xx | **우리 버그** | 500 | 보존 (수정해야 할 코드) |

503의 의미는 "당신 잘못도 아니고 영구적이지도 않다 — 재시도하라"입니다.
장애를 500으로 내보내면 클라이언트는 재시도해야 할지 판단할 수 없습니다.
**상태코드는 책임의 표명입니다.**

그리고 장애 순간의 진실: **결제가 됐는지 안 됐는지 모릅니다** (요청이 PG에
도달했는지조차 불확실). 모르는데 PAID로 바꾸면 거짓말, CANCELLED로 바꿔도
거짓말 — 그래서 **아무것도 안 바꾸는 것**(PENDING 보존)이 유일하게 정직한
처리입니다. exercise의 `never()` 검증이 바로 이 설계의 봉인.

### 3-5. 예외 번역 — 기술 예외를 사건으로

`ResourceAccessException`을 Controller까지 흘려보내면 호출자가 RestTemplate의
존재를 알아야 합니다. Client의 경계에서 **우리 어휘**(ExternalServiceException)로
번역하되, `cause`를 보존해 로그에서 원인 추적이 가능하게 합니다 —
DAO 계층에서 SQLException을 DataAccessException으로 번역하는 것과 같은 원리.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step04.*"
```

1. `PaymentClient` — 빌더의 타임아웃 2종 + RetryTemplate 구성 읽기
2. `PaymentClientRetryTest` — 네 얼굴: 회복(투명!)/포기(3회 정확히)/타임아웃/4xx 비재시도
3. `PaymentOutageApiTest` — 503과 메시지 (내부 사정 은닉)
4. **일부러 깨뜨려보기**: RetryTemplate의 `maxAttempts(3)`을 5로 바꾸면 어떤
   테스트가 어떻게 깨질까? (times(3) 선언과 어긋남 — "정확히 3회"가 봉인된 증거)

## 5. Testing — exercise 풀기

`step04/exercise/PaymentOutageExerciseTest.java`의 TODO 1~4를 채우세요.
step03의 거절 시나리오와 거의 같은 모양인 게 정상입니다 — **사건이 다르면
(400 vs 503) 회귀도 따로 일어나므로 봉인도 따로** 합니다. TODO 4가 본질입니다.

## 6. Lessons Learned

### 사례: 추천 API 장애가 메인 페이지를 죽인 날

- **증상**: 부가 기능인 추천 위젯의 외부 API 장애 → 메인 페이지 전체 응답 불가
- **원인**: 타임아웃 미설정 — 추천 호출 스레드가 무기한 대기, 풀 고갈
- **해결**: connect/read 타임아웃 + 장애 시 위젯만 비우고 페이지는 정상 응답
- **교훈**: 타임아웃은 성능 옵션이 아니라 **생존 설정**이다. 모든 외부 호출에, 예외 없이.

### 사례: 재시도가 만든 3중 결제

- **증상**: 고객 카드에 같은 금액이 3번 결제됨
- **원인**: read 타임아웃 시 무조건 재시도 — 사실 PG는 매번 승인에 성공했고, 응답만 늦었다
- **해결**: 주문번호를 멱등키로 전달, PG가 중복 승인을 거부하도록 계약 변경
- **교훈**: POST 재시도는 멱등성이 보장될 때만. "재시도해도 안전한가"는 코드가 아니라 **계약**의 문제다.

### 시니어의 시선

> 재시도 설정을 보면 연차가 보입니다. 무한 재시도(상한 없음)는 위험을 모르는 것,
> 백오프 없는 재시도는 상대를 배려하지 않는 것, 4xx까지 재시도하는 건 실패의
> 종류를 구별하지 못하는 것. maxAttempts(3) + 지수 백오프 + retryOn 명시 —
> 세 가지가 다 있으면 안심하고 다음 줄을 읽습니다.

## 7. Key Takeaways

- 타임아웃 없는 외부 호출 금지 — connect/read 모두, yml로 환경별 조정
- 재시도는 일시 장애(타임아웃·5xx)만, 상한(maxAttempts) + 지수 백오프 필수, 4xx 제외
- POST 재시도의 전제는 멱등성 — 코드가 아니라 외부와의 계약
- 재시도 횟수는 MockRestServiceServer 기대 선언 수 + verify()로 봉인
- 거절=400, 장애=503, 우리 버그=500 — 상태코드는 책임의 표명
- 장애 = 결제 여부 불확실 → 주문은 건드리지 않는다 (never로 봉인)

## 8. Next Steps — 다음 Step의 문제

결제까지 흐르는 커머스가 됐습니다. 그런데 상품 목록을 보세요 — 텍스트뿐입니다.
운영자가 묻습니다:

> "상품 **이미지**는 어떻게 올리죠?"

파일은 지금까지의 JSON과 전혀 다른 입력입니다. multipart라는 별도 규격,
"아무 파일이나 받으면 되나?"(확장자 검증 — exe를 받으면?), 받은 파일은
어디에 저장하나, 테스트는 진짜 디스크에 써도 되나(@TempDir)?

**Step 5: 파일 업로드/다운로드**에서 MockMultipartFile과 함께 다룹니다.
