# [Web Step 1] 스캐폴드 + 커머스 도메인 — 무대 구축

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: TestCraft 복습(Mockito/@MybatisTest/@WebMvcTest) + 🆕 원자적 재고 차감, Security 없는 @WebMvcTest
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow, test/java/com/webflow/step01}/`

---

## 1. Before We Start — 새 모듈, 익숙한 무기

WebFlow에 오신 것을 환영합니다. 이 모듈의 주제는 **실무 API 서버의 잡기술** —
외부 연동, 파일, 캐싱, 스케줄링, 운영입니다. TestCraft에서 배운 테스트 무기를
그대로 들고, 이번엔 "장면"이 실무로 바뀝니다.

Step 1은 그 무대(미니 커머스)를 빠르게 세우는 단계 — TestCraft 복습이자,
**첫 신무기 하나**(원자적 재고 차감)가 등장합니다.

## 2. What We're Building

```
상품(product) 12종 + 주문(orders) 6건 시드
GET/POST /api/products, /api/orders — 기본 CRUD + 검증/예외 규약(TestCraft 패턴)
주문 흐름: 재고 차감 → PENDING_PAYMENT 생성 (결제는 Step 3에서!)
```

```
src/test/java/com/webflow/step01/
├── example/OrderServiceTest.java    ← Mockito 복습 + 원자적 차감 패턴
├── example/ProductDaoTest.java      ← @MybatisTest 복습 + 시드 봉인
├── exercise/ProductApiExerciseTest.java ← @WebMvcTest 복습 (Security 없이!)
└── answer/ProductApiAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 원자적 재고 차감 — 확인과 변경을 한 문장에

초보 구현의 함정:

```java
Product p = dao.findById(id);          // ① 재고 확인: 1개 남음
if (p.getStock() >= qty) {             // ② 두 요청이 동시에 여기를 통과!
    dao.updateStock(id, p.getStock() - qty);  // ③ 둘 다 차감 → 재고 -1 😱
}
```

①과 ③ 사이에 다른 요청이 끼어들 수 있습니다 (check-then-act 경쟁 조건).
해법은 **확인과 차감을 UPDATE 한 문장**으로:

```sql
UPDATE product SET stock = stock - #{quantity}
WHERE product_id = #{productId} AND stock >= #{quantity}
```

affected = 0이면 차감 실패(재고 부족) — DB가 원자성을 보장합니다.
`ProductDaoTest`의 품절 차감 테스트가 이 계약을 봉인합니다.

### 3-2. Security가 없는 @WebMvcTest — TestCraft 졸업 문제

이 모듈에는 starter-security가 없습니다. 그래서:

- `@Import(SecurityConfig)` 불필요 — 임포트할 보안 설정 자체가 없다
- `with(csrf())` 불필요 — CSRF 필터가 안 뜬다
- 보안 자동구성은 **클래스패스에 의존**한다는 것을 체감하는 기회

"TestCraft에선 왜 필요했고 여기선 왜 불필요한가"를 설명할 수 있다면
슬라이스의 동작 원리를 제대로 이해한 것입니다.

### 3-3. 예약어, 두 번째 만남

BatchFlow의 TRANSACTION에 이어 이번엔 **ORDER** — `ORDER BY`의 그 ORDER라서
테이블명은 `orders`입니다. 예약어 회피는 우연이 아니라 체크리스트입니다.

### 3-4. @Transactional — 차감과 INSERT는 한 운명

`placeOrder`에서 재고 차감 후 주문 INSERT가 실패하면? 차감만 남으면 재고가 샙니다.
`@Transactional`이 둘을 한 트랜잭션으로 묶어 함께 성공/함께 롤백을 보장합니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step01.*"
```

1. schema/data.sql — 시드 기준값 표 (12종/품절 1/검색 키워드)
2. `OrderServiceTest` — Captor로 총액/상태, never로 부수효과 차단 (전부 복습!)
3. `ProductDaoTest` — 원자적 차감의 성공/실패 양면
4. **일부러 깨뜨려보기**: ProductMapper.xml의 `AND stock >= #{quantity}`를 지우면
   어떤 테스트가 깨질까? 예측 → 실행 → 원복 (경쟁 조건 방어가 사라지는 순간)

## 5. Testing — exercise 풀기

`step01/exercise/ProductApiExerciseTest.java`의 TODO 1~7을 채우세요.
TestCraft Step 4~5의 복습입니다 — csrf 없이 쓰는 것이 어색하다면 3-2를 다시.

## 6. Lessons Learned

### 사례: 이벤트 날 재고가 음수가 된 쇼핑몰

- **증상**: 한정 수량 이벤트에서 재고 -3, 초과 주문 발생
- **원인**: SELECT 확인 후 UPDATE — 동시 요청이 확인을 동시에 통과
- **해결**: 조건부 UPDATE 한 문장 (3-1) — affected로 성패 판정
- **교훈**: "확인하고 변경"은 동시성의 적신호. DB에게 원자성을 맡겨라.

### 시니어의 시선

> 이 Step이 1.5시간 만에 끝난다면 TestCraft가 제 역할을 한 겁니다 —
> 같은 작업이 처음이라면 며칠짜리니까요. 기본기의 가치는 "새 무대를 세우는 속도"로
> 측정됩니다. 이제 그 무대 위에서, 진짜 새로운 것(외부 세계)을 배웁니다.

## 7. Key Takeaways

- 원자적 차감: 확인+변경을 UPDATE 한 문장 + affected 검증 (check-then-act 금지)
- Security 없는 모듈 = @Import/csrf 불필요 — 자동구성은 클래스패스를 따른다
- ORDER도 예약어 — orders (예약어 회피 체크리스트)
- @Transactional: 차감과 INSERT는 한 운명

## 8. Next Steps — 다음 Step의 문제

상품 단건 조회는 되는데, 화면을 만들려니 바로 막힙니다.

> "상품 **목록** 주세요. 페이지당 5개, '키보드'로 검색, **가격 낮은 순**으로요"

페이징 응답엔 어떤 필드가 필요할까요(전체 건수? 전체 페이지?).
그리고 정렬 — 사용자 입력을 ORDER BY에 그대로 꽂으면 **SQL 인젝션**입니다.
안전한 정렬(화이트리스트)까지, **Step 2: 목록 API**에서 다룹니다.
