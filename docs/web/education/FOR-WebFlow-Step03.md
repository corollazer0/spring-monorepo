# [Web Step 3] 외부 결제 연동 기초 — 우리가 통제할 수 없는 세계

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 🆕 RestTemplateBuilder, @RestClientTest + MockRestServiceServer, 경계 DTO, 외부 호출 전 방어선
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow/external/payment, test/java/com/webflow/step03}/`

---

## 1. Before We Start — 결제가 없는 쇼핑몰

Step 2까지의 주문은 전부 `PENDING_PAYMENT`에 멈춰 있습니다. 결제가 없으니까요.
그리고 결제는 **우리 서버가 할 수 없는 일**입니다 — 카드사와 연결된 PG사(외부 API)를
불러야 합니다.

지금까지의 코드는 전부 우리 손안에 있었습니다(우리 DB, 우리 로직). 외부 API는
처음으로 만나는 **통제 불가능한 의존성**입니다. 그래서 질문이 바뀝니다:

> 테스트에서 진짜 PG를 호출할 건가요? — 과금되고, 느리고, PG 상태 따라 결과가 다른데?

## 2. What We're Building

```
POST /api/orders/{orderId}/payment
  → OrderService.payOrder: 검증(전) → PaymentClient.approve(외부) → 상태 전이(후)
  → PG 응답 APPROVED → PAID + paymentKey 저장
  → PG 응답 DECLINED → 400, 주문은 PENDING_PAYMENT 보존
```

```
src/main/java/com/webflow/external/payment/
├── PaymentClient.java          ← RestTemplateBuilder로 생성 (new 금지!)
├── PaymentApproveRequest.java  ← 외부 계약 전용 DTO
└── PaymentApproveResponse.java

src/test/java/com/webflow/step03/
├── example/PaymentClientTest.java       ← @RestClientTest + MockRestServiceServer
├── example/OrderPaymentServiceTest.java ← 외부 호출 전/후의 비즈니스 규칙 (Mockito)
├── exercise/PaymentApiExerciseTest.java ← 결제 API HTTP 계약 (@WebMvcTest)
└── answer/PaymentApiAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 RestTemplate은 빌더로 — `new` 하는 순간 테스트 불가

```java
public PaymentClient(RestTemplateBuilder builder,
                     @Value("${external.payment.base-url}") String baseUrl) {
    this.restTemplate = builder.rootUri(baseUrl).build();
}
```

`new RestTemplate()`로 만들면 `@RestClientTest`가 끼어들 자리가 없습니다.
**Boot가 주입하는 RestTemplateBuilder**를 거치면, 테스트에서 Boot가 그 빌더에
MockRestServiceServer를 심어줍니다 — 클라이언트 코드는 한 줄도 안 바꾸고
HTTP가 가로채집니다. "테스트 가능성은 설계에서 나온다"의 교과서적 사례.

### 3-2. 🆕 @RestClientTest — 세 번째 슬라이스

| 슬라이스 | 띄우는 것 | 검증 대상 |
|---|---|---|
| @WebMvcTest | MVC 계층 | **들어오는** HTTP |
| @MybatisTest | MyBatis + DB | SQL |
| 🆕 @RestClientTest | 지정 클라이언트 + Jackson | **나가는** HTTP |

MockRestServiceServer의 문법은 "기대를 먼저 선언, 응답을 예약":

```java
server.expect(requestTo("/api/v1/payments"))   // rootUri 사용 시 상대 경로로 매칭!
      .andExpect(method(HttpMethod.POST))
      .andExpect(jsonPath("$.orderId").value(1))      // 나가는 본문 검증!
      .andRespond(withSuccess("{...}", MediaType.APPLICATION_JSON));
// ... 호출 ...
server.verify();   // 선언한 기대가 전부 소비되었는가
```

검증 대상은 우리 코드의 양 끝입니다 — **요청이 규격대로 나가는가** +
**응답을 규격대로 읽는가**. 그 사이(PG 내부)는 우리 책임이 아닙니다.

### 3-3. 거절(DECLINED)은 오류가 아니다

PG가 HTTP 200으로 `DECLINED`를 줬다면 PG는 일을 잘 한 겁니다 — 한도 초과 카드를
거른 것이니까. 이걸 예외/장애와 섞으면 안 됩니다:

| 사건 | 정체 | 우리 응답 |
|---|---|---|
| APPROVED | 성공 | 200, PAID 전이 |
| DECLINED (HTTP 200) | **정상 응답**, 비즈니스 거절 | 400, 주문 보존 |
| 타임아웃/5xx | **장애** | 503 — Step 4의 주제! |

Client는 거절을 "값"으로 돌려주고, 해석(예외로 바꿀지)은 Service의 몫 —
계층의 책임 분리가 여기서도 적용됩니다.

### 3-4. 방어선은 외부 호출 "전"에 — never의 무게

`OrderPaymentServiceTest`에서 가장 중요한 단언은 이것입니다:

```java
then(paymentClient).should(never()).approve(any());
```

없는 주문, 이미 PAID인 주문 — 검증 실패 시 **PG 호출 자체가 없어야** 합니다.
이미 결제된 주문으로 PG를 또 부르면? 고객 카드에서 두 번 빠집니다(이중 결제 사고).
`never()`는 "부수효과가 없었다"는 가장 강한 안전장치 증명입니다 (TestCraft Step 2의
무기가 실전에서 빛나는 순간).

### 3-5. 거절 시 주문 보존 — 실패의 설계

DECLINED일 때 주문을 CANCELLED로 바꿔버리면? 사용자가 다른 카드로 재시도할 수
없습니다. **PENDING_PAYMENT 보존 = 재시도 가능성**. "실패했을 때 시스템이 어떤
상태로 남는가"는 성공 경로만큼 중요한 설계 결정이고, 그래서 테스트로 봉인합니다
(`updateStatus`가 `never()` 호출).

### 3-6. 경계에는 전용 DTO — 외부 계약과 내부 도메인의 분리

`Order`를 그대로 PG에 보내지 않고 `PaymentApproveRequest`로 변환합니다.
PG가 필드명을 바꾸면? 경계 DTO만 고치면 됩니다. 도메인을 직접 보내고 있었다면
외부 사정으로 내부 도메인이 출렁입니다. **외부 계약의 변화가 안으로 번지지 않게
방화벽을 친다** — `external/` 패키지가 그 방화벽입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step03.*"
```

1. `PaymentClient` — 생성자에서 빌더+rootUri를 확인 (new가 없다!)
2. `PaymentClientTest` — expect/andExpect/andRespond/verify의 4박자
3. `OrderPaymentServiceTest` — 4개 테스트에서 never()가 몇 번 나오는지 세어보기 (3번!)
4. **일부러 깨뜨려보기**: OrderService.payOrder에서 상태 검증 if를 주석 처리하면
   어떤 테스트가 깨질까? (이중 결제 방어 테스트 — never가 fail)

## 5. Testing — exercise 풀기

`step03/exercise/PaymentApiExerciseTest.java`의 TODO 1~6을 채우세요.
@WebMvcTest 슬라이스이므로 **PaymentClient는 등장하지 않습니다** — Service를
@MockBean으로 막은 순간 외부 연동은 이 테스트 밖의 일입니다. 계층마다 자기 책임만:
Client는 @RestClientTest가, 규칙은 Mockito가, HTTP 계약은 여기가 맡습니다.

## 6. Lessons Learned

### 사례: 테스트 돌릴 때마다 100원씩 결제된 신입의 CI

- **증상**: 통합 테스트가 PG 샌드박스가 아닌 운영 키로 진짜 승인 API를 호출
- **원인**: 테스트에서 실제 RestTemplate이 실제 URL로 나감 — 가로채는 층이 없었다
- **해결**: @RestClientTest + MockRestServiceServer (네트워크 자체가 안 나간다)
- **교훈**: 외부 API 테스트의 1원칙 — **진짜 HTTP는 금지**. 빌더 주입이 그 전제다.

### 사례: 장애 때 주문이 전부 CANCELLED가 된 날

- **증상**: PG 점검 시간에 들어온 주문이 모두 취소 처리되어 복구 불가
- **원인**: 거절(DECLINED)과 장애(타임아웃)를 같은 catch로 묶어 CANCELLED 처리
- **해결**: 거절=400+보존 / 장애=503+보존, 별도 사건으로 분리 (Step 4에서 완성)
- **교훈**: 외부 응답의 "실패"는 한 종류가 아니다. 사건을 구별해야 복구가 설계된다.

### 시니어의 시선

> 외부 연동 코드리뷰에서 첫 질문은 "이거 PG 죽으면 어떻게 되나요?"입니다.
> Step 3의 PaymentClient는 그 질문에 아직 답이 없습니다 — 타임아웃이 없으니
> PG가 멈추면 우리 스레드도 같이 멈춥니다. 일부러 남겨둔 구멍입니다.
> 다음 Step에서 그 구멍으로 장애가 들어오는 걸 직접 보게 됩니다.

## 7. Key Takeaways

- RestTemplate은 RestTemplateBuilder로 — new 하면 @RestClientTest가 못 끼어든다
- @RestClientTest + MockRestServiceServer: 나가는 요청 규격 + 들어오는 응답 매핑, 진짜 HTTP 금지
- 거절(200+DECLINED)은 정상 응답, 장애(타임아웃/5xx)와 구별하라
- 방어선은 외부 호출 전에 — never()로 "호출 안 됨"을 봉인 (이중 결제 방지)
- 실패 시 주문 보존 = 재시도 가능성 — 실패 후 상태도 설계다
- 경계에는 전용 DTO — 외부 계약 변화가 도메인으로 번지지 않게

## 8. Next Steps — 다음 Step의 문제

지금 PaymentClient에는 **타임아웃이 없습니다**. PG가 30초간 응답을 안 주면?
우리 요청 스레드가 30초간 묶입니다. 동시에 100명이 결제하면 톰캣 스레드 풀이
바닥나고 — **결제뿐 아니라 상품 조회까지 전부 멈춥니다.** 외부 장애가 우리 전체의
장애가 되는 거죠.

타임아웃은 몇 초가 적당할까요? 일시적 네트워크 출렁임이면 한 번 더 시도해볼 수는
없을까요(재시도, 그리고 백오프)? 그래도 안 되면 사용자에게 뭐라고 답하죠(503)?

외부 장애에서 살아남기 — **Step 4: 타임아웃·재시도·장애 격리**입니다.
