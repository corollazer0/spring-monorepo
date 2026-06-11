# [Web Step 6] 캐싱 — 기억의 달콤함과 거짓말의 위험

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 🆕 @EnableCaching, @Cacheable/@CacheEvict, @MockBean DAO + verify(times) 검증법
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow/config, test/java/com/webflow/step06}/`

---

## 1. Before We Start — 같은 SELECT가 초당 수백 번

인기 상품 하나를 초당 수백 명이 조회합니다. 상품 정보는 분 단위로도 잘 안
바뀌는데, 매번 DB까지 갑니다. "한 번 읽은 걸 기억해두자" — 캐싱입니다.

쉬워 보이지만, 캐싱은 분산 시스템의 유명한 격언이 붙은 주제입니다:

> "컴퓨터 과학에서 어려운 건 딱 두 가지다 — **캐시 무효화**와 이름 짓기."

저장은 한 줄(@Cacheable)이고, 진짜 문제는 **언제 그 기억을 버릴 것인가**입니다.
기억이 현실과 어긋난 순간부터 캐시는 거짓말 제조기가 됩니다.

## 2. What We're Building

```
@Cacheable  getProduct(id)        ← 같은 id 두 번째부터 DB 0회
@CacheEvict placeOrder(주문)      ← 재고가 바뀌니 그 상품 캐시 폐기
@CacheEvict uploadProductImage    ← 이미지가 바뀌니 그 상품 캐시 폐기
```

```
src/main/java/com/webflow/config/CacheConfig.java   ← @EnableCaching + 캐시 이름 상수
src/test/java/com/webflow/step06/
├── example/ProductCacheTest.java       ← 히트/키분리/무효화/예외 (4가지 본질)
├── exercise/CacheEvictExerciseTest.java ← 업로드 무효화 봉인
└── answer/CacheEvictAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 @Cacheable — 메서드 본문이 "실행되지 않는다"

```java
@Cacheable(cacheNames = CacheConfig.PRODUCTS, key = "#productId")
public ProductResponse getProduct(Long productId) { ... }
```

캐시에 키가 있으면 **메서드 안으로 들어가지도 않고** 캐시 값을 돌려줍니다.
이 마법의 정체는 프록시(AOP) — 트랜잭션(@Transactional)과 같은 메커니즘입니다.
그래서 두 가지 함정도 똑같이 따라옵니다:

- `new ProductService(...)`로는 캐시가 동작하지 않는다 (프록시가 없으니까) —
  **캐시 테스트에 @SpringBootTest가 필요한 이유**
- 같은 클래스 안에서 `this.getProduct(...)`로 부르면 캐시를 안 탄다 (자기 호출은
  프록시를 우회)

### 3-2. 🆕 캐시 검증법 — "응답이 같다"는 증거가 아니다

캐시가 동작하는지 어떻게 증명할까요? 응답 비교는 무력합니다 — 캐시가 없어도
같은 DB에서 읽으니 응답은 항상 같습니다. 유일한 증거는 **DB가 몇 번 불렸는가**:

```java
@MockBean ProductDao productDao;   // DB 호출을 "셀 수 있는" 대상으로

productService.getProduct(1L);
productService.getProduct(1L);
verify(productDao, times(1)).findById(1L);   // 두 번 조회, DB는 한 번!
```

TestCraft에서 배운 행동 검증(verify)이 캐시에서 본질이 되는 순간입니다.

### 3-3. 무효화 — 데이터를 바꾸는 모든 길목에 @CacheEvict

`getProduct`만 보면 안 됩니다. **product 데이터를 바꾸는 경로를 전부 찾아야**
합니다:

| 변경 경로 | 바뀌는 것 | 조치 |
|---|---|---|
| placeOrder (주문) | stock | @CacheEvict(key = 주문한 상품) |
| uploadProductImage | image_path | @CacheEvict(key = 해당 상품) |
| createProduct | 새 행 | 기존 키와 무관 — 조치 불요 |

placeOrder는 **OrderService**에 있다는 점에 주목하세요 — 캐시 무효화는
캐시를 만든 서비스의 일이 아니라, **데이터를 바꾸는 모든 코드의 일**입니다.
하나라도 빠뜨리면? 품절 상품이 "재고 있음"으로 보이는 거짓말이 시작됩니다.

### 3-4. 예외는 캐시되지 않는다 — 그리고 그게 다행인 이유

없는 상품 조회(404)를 두 번 하면 DB도 두 번 갑니다 — 예외가 던져지면
@Cacheable은 아무것도 저장하지 않습니다. 만약 "없음"이 캐시된다면?
그 상품이 새로 등록돼도 캐시 수명 동안 계속 404 — 곤란하겠죠.

(거꾸로 악의적 트래픽이 없는 키만 골라 두드리면 캐시가 전혀 못 막아주는 문제도
생깁니다 — "캐시 관통". 지금은 "예외는 캐시 안 됨"만 기억하면 충분합니다.)

### 3-5. 캐시는 테스트 사이에 살아남는다

@MockBean은 테스트마다 자동 리셋되지만 **캐시는 컨텍스트와 함께 계속 삽니다**.
앞 테스트가 데운 캐시가 뒷 테스트의 verify 횟수를 바꾼다면? 실행 순서에 따라
성패가 달라지는 최악의 테스트가 됩니다. 그래서:

```java
@BeforeEach
void clearCache() {
    cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
}
```

BatchFlow의 "@AfterEach 원상복구"와 같은 철학 — **공유 상태는 테스트가 직접
격리한다.**

### 3-6. ConcurrentMapCacheManager — 시작은 Map 한 장

학습용으론 JVM 안의 Map이면 충분합니다. 실무에서 서버가 2대가 되면?
서버 A가 비운 캐시를 서버 B는 모릅니다(불일치) — 그때가 Redis 같은 중앙 캐시로
가는 시점입니다. 중요한 건 **@Cacheable 코드는 한 줄도 안 바뀐다**는 것 —
캐시 기술 추상화가 이 애노테이션의 진짜 가치입니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step06.*"
```

1. `CacheConfig` — @EnableCaching, 캐시 이름 상수
2. `ProductCacheTest` — 4가지 본질: 히트(times 1)/키 분리/무효화/예외 비캐시
3. **일부러 깨뜨려보기**: OrderService.placeOrder의 @CacheEvict를 지우면
   어떤 테스트가 깨질까? → 재고 거짓말 시나리오가 테스트로 봉인되어 있다는 뜻

## 5. Testing — exercise 풀기

`step06/exercise/CacheEvictExerciseTest.java`의 TODO 1~5를 채우세요.
"캐시 항목이 사라졌다"(상태)와 "재조회가 DB로 갔다"(행동) **둘 다** 검증하는
것이 채점 포인트 — 횟수 계산에 업로드 내부의 존재 확인 호출도 들어갑니다.

## 6. Lessons Learned

### 사례: 품절인데 주문 버튼이 살아있던 4시간

- **증상**: 품절 상품이 목록에서 계속 "구매 가능"으로 노출, 주문 시도 폭주
- **원인**: 상품 조회에 캐시 도입 — 주문(재고 차감) 경로에 무효화 누락
- **해결**: 데이터를 바꾸는 전 경로 조사 후 @CacheEvict 배치 + 그 봉인 테스트
- **교훈**: @Cacheable을 붙이는 순간, "이 데이터를 바꾸는 모든 코드"가 네 책임이 된다.

### 사례: 혼자만 통과하는 캐시 테스트

- **증상**: 단독 실행은 통과, 전체 실행은 실패 — verify 횟수가 안 맞음
- **원인**: 앞 테스트가 데운 캐시가 살아있어 뒷 테스트의 DB 호출 수가 달라짐
- **해결**: @BeforeEach에서 캐시 전체 clear
- **교훈**: 컨텍스트가 공유되면 캐시도 공유된다. Mock은 리셋돼도 캐시는 남는다.

### 시니어의 시선

> 캐시 도입 PR에서 제가 보는 것: @Cacheable 옆에 **무효화 경로 목록**이
> 주석이나 PR 설명으로 있는가입니다. "어디서 비우는지"를 나열하지 못하면
> 아직 도입할 준비가 안 된 겁니다. 캐시는 성능 기능이 아니라
> **일관성 책임**을 사는 일입니다.

## 7. Key Takeaways

- @Cacheable = 프록시 — new로는 동작 안 함(@SpringBootTest 필요), 자기 호출 우회
- 캐시의 증거는 응답이 아니라 호출 횟수 — @MockBean DAO + verify(times(N))
- 무효화는 데이터를 바꾸는 "모든" 경로에 — 다른 서비스(placeOrder)도 예외 없다
- 예외는 캐시되지 않는다 — "없음"이 박제되지 않아 다행
- 캐시는 테스트 사이에 살아남는다 — @BeforeEach clear로 격리
- ConcurrentMap → Redis로 바꿔도 @Cacheable 코드는 그대로 (추상화의 가치)

## 8. Next Steps — 다음 Step의 문제

주문 테이블을 보니 PENDING_PAYMENT 주문이 계속 쌓입니다 — 결제창까지 갔다가
이탈한 주문들. 문제는 이들이 **재고를 물고 있다**는 겁니다 (주문 시점에 차감했으니).
방치하면 팔 수 있는 재고가 유령 주문에 잠깁니다.

> "30분 넘게 미결제인 주문은 자동 취소하고 재고를 돌려놔 주세요."

사용자 요청 없이 **시간이 트리거**인 작업 — 처음 만나는 모양입니다.
@Scheduled는 어떻게 쓰고, "새벽 3시에 도는 코드"는 어떻게 테스트하죠?
**Step 7: 스케줄링**에서 다룹니다.
