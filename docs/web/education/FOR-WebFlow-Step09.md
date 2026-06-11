# [Web Step 9] 캡스톤: 배송 조회 연동 — 요구사항만 들고 스스로

> **소요 시간**: 약 3시간 (설계 30분 + 구현 1.5시간 + 테스트 1시간)
> **이번 Step의 도구**: 🆕 없음! — Step 1~8의 무기를 스스로 고르고 조합하는 것이 과제
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow/external/delivery, test/java/com/webflow/step09}/`

---

## 1. 진행 방법 — 이번엔 순서가 다르다

지금까지는 example을 읽고 exercise를 풀었습니다. 캡스톤은 반대입니다:

1. **요구사항(2장)을 읽고 스스로 설계한다** — 어떤 클래스? 어떤 패키지? 어떤 테스트?
2. 체크리스트(3장)로 설계를 점검한다
3. warmup exercise(준비 운동)로 시동을 건다
4. 구현하고, 테스트를 직접 작성한다
5. **다 끝난 뒤에** answer 스위트·모범 구현과 비교한다 (4장 해설)

> ⚠️ answer를 먼저 보면 캡스톤의 의미가 사라집니다. 막히면 "어느 Step의
> 문제인지"를 먼저 떠올리세요 — 답은 전부 Step 3~4·8 안에 있습니다.

## 2. 요구사항 (기획서 그대로)

> **[기능 요청] 주문 배송 조회**
>
> 1. 고객이 자기 주문의 배송 상태를 조회할 수 있어야 합니다.
>    `GET /api/orders/{orderId}/delivery`
> 2. 배송 정보는 외부 배송사 API에서 가져옵니다:
>    `GET https://delivery.example.com/api/v1/deliveries/{paymentKey}`
>    응답: `{ "status": "SHIPPING", "invoiceNo": "INV-123", "courierName": "한진" }`
>    (status: PREPARING / SHIPPING / DELIVERED)
> 3. **미결제·취소 주문은 배송 조회가 되면 안 됩니다** (결제 완료 주문만).
> 4. 배송사에 아직 송장이 등록 전이면 배송사가 404를 줍니다 —
>    이 경우 고객에겐 "배송 준비 중"으로 보여주세요. (에러가 아닙니다!)
> 5. 배송사 API가 장애일 때 우리 서비스가 같이 죽으면 안 됩니다.

## 3. 설계 체크리스트 — 구현 전 자가 점검

**구조**
- [ ] 배송사 클라이언트를 어느 패키지에? (힌트: PaymentClient의 이웃)
- [ ] 배송사 응답 DTO와 우리 API 응답 DTO를 분리했는가? (경계 DTO는 양방향!)
- [ ] 검증 로직(결제 완료 주문만)은 어느 계층의 일인가?

**외부 연동 (Step 3~4의 자가 복습)**
- [ ] RestTemplate을 `new` 하지 않았는가?
- [ ] 타임아웃 2종이 프로퍼티로 있는가?
- [ ] 재시도에 상한과 백오프가 있는가? (조회 GET은 멱등 — 재시도 부담이 없다!)
- [ ] 배송사 404를 어떻게 다뤘는가? — 요구사항 4를 다시 읽어라
- [ ] 재시도 끝 장애가 503으로 번역되는가?

**테스트 (각 계층의 자기 책임)**
- [ ] 클라이언트: @RestClientTest — 성공 / 404→준비중 / 5xx 재시도 / 회복
- [ ] 서비스: Mockito — 없는 주문 404, 미결제 400, **거부 시 never(track)**
- [ ] API: @WebMvcTest — 200 계약 / 400 / 503
- [ ] "PAID만 허용"인가 "PENDING이면 거부"인가? — 취소 주문 테스트가 그 차이를 가른다

## 4. 모범 설계 해설 (구현을 마친 뒤 읽으세요)

### 4-1. 외부 404의 번역 — 이 캡스톤의 핵심 판단

배송사의 404는 우리에게 404가 아닙니다. "송장 미등록 = 배송 준비 중"이라는
**정상 비즈니스 상태**죠. 그래서 DeliveryClient는:

```java
} catch (HttpClientErrorException.NotFound e) {
    return DeliveryStatusResponse.preparing();   // 예외가 아니라 값!
}
```

Step 3의 표("거절은 오류가 아니다")가 여기서 변주됩니다 — **외부의 상태코드를
우리의 의미로 번역하는 것**이 클라이언트 계층의 존재 이유입니다. 404를 그대로
흘려보냈다면 고객은 "주문이 없다"는 거짓 안내를 받았을 겁니다.

### 4-2. PAID "만" 허용 — 화이트리스트 사고방식

`!PENDING` 거부가 아니라 `!PAID` 거부로 짰는지 보세요. 차이는 CANCELLED에서
드러납니다 — 그리고 미래에 REFUNDED 같은 상태가 추가될 때 **안전한 쪽으로
닫혀** 있습니다. Step 2의 정렬 화이트리스트, Step 5의 확장자 화이트리스트와
같은 철학이 상태 검증에서 세 번째로 반복됐습니다.

### 4-3. 구조 복기

```
external/delivery/DeliveryClient        ← Step 3~4 골격 재사용 (빌더+타임아웃+재시도+503)
external/delivery/DeliveryStatusResponse ← 배송사 계약
order/dto/OrderDeliveryResponse          ← 우리 계약 (분리!)
order/service/DeliveryService           ← 호출 전 방어선 (404/400 + never)
order/controller/DeliveryController     ← 조회 전용 분리
```

paymentKey가 주문과 배송사를 잇는 연결 고리라는 점 — Step 3에서 저장해둔
값이 Step 9의 입력이 됩니다. **기능은 이어진다, 설계도 이어진다.**

### 4-4. 시니어의 시선

> 캡스톤 리뷰에서 보는 건 코드가 아니라 **판단의 흔적**입니다.
> 404를 catch했는가(요구사항을 읽었다), never를 썼는가(Step 3을 체화했다),
> DTO를 나눴는가(경계를 이해했다), 취소 주문 테스트가 있는가(화이트리스트로
> 생각했다). 네 가지가 보이면 이 모듈은 졸업입니다.

## 5. 완주 체크 — WebFlow 졸업 기준

- [ ] Step 1~9의 example/answer 테스트가 전부 그린
- [ ] 모든 exercise를 @Disabled 제거 후 스스로 통과시켰다
- [ ] 다음 질문에 막힘없이 답할 수 있다:
  - 재고 차감은 왜 UPDATE 한 문장인가? (Step 1)
  - 정렬 키를 ${}로 넣으면 무슨 일이? (Step 2)
  - RestTemplate을 왜 빌더로 만드나? (Step 3)
  - 거절·장애·버그의 상태코드가 왜 다른가? (Step 3~4)
  - 업로드 보안 3원칙은? (Step 5)
  - 캐시 무효화는 누구의 책임인가? (Step 6)
  - 스케줄 로직은 어떻게 트리거 없이 테스트하나? (Step 7)
  - actuator/env는 왜 닫나? (Step 8)

## 6. Next Steps — 심화로 (선택)

필수 트랙 완주를 축하합니다! 더 가고 싶다면:

- **WebClient** — RestTemplate의 후계자(논블로킹). Boot 3 이후의 표준
- **Resilience4j** — 서킷 브레이커: 재시도 너머의 장애 대응 (호출 자체를 차단)
- **Redis 캐시** — ConcurrentMap을 중앙 캐시로 (서버 2대 문제의 해법)
- **ShedLock** — @Scheduled의 분산 락 (Step 7 사례의 해법)

각 주제 모두 "이 모듈에서 배운 테스트 기법"이 그대로 적용됩니다 —
도구가 바뀌어도 검증의 사고방식은 바뀌지 않습니다.
