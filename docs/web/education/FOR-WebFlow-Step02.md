# [Web Step 2] 목록 API — 페이징·검색·정렬 (그리고 인젝션 방어)

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 🆕 PageResponse 규약, MyBatis 동적 SQL(`<where>`/`<sql>` 조각), 정렬 화이트리스트, OFFSET/FETCH
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow, test/java/com/webflow/step02}/`

---

## 1. Before We Start — "목록 주세요"의 무게

Step 1 끝의 요구사항을 다시 봅시다.

> "상품 목록 주세요. 페이지당 5개, '키보드'로 검색, 가격 낮은 순으로요"

한 문장에 **세 가지 문제**가 들어 있습니다:

1. **페이징** — 12,000개를 한 번에 줄 순 없다. 그런데 화면은 "전체 3페이지 중 1페이지"를
   그려야 한다. 응답에 뭐가 더 필요할까?
2. **검색** — 키워드가 있을 때만 WHERE가 붙어야 한다. **동적 SQL**.
3. **정렬** — 사용자가 고른 정렬 키를 ORDER BY에 넣어야 한다. 그런데
   `#{sort}`로는 동작하지 않고, `${sort}`는... **SQL 인젝션 구멍**입니다.

이 중 3번이 이번 Step의 주인공입니다.

## 2. What We're Building

```
GET /api/products?page=2&size=5&keyword=키보드&category=KEYBOARD&sort=priceAsc

응답(PageResponse 규약):
{ "content": [...], "page": 2, "size": 5, "totalCount": 12, "totalPages": 3 }
```

```
src/test/java/com/webflow/step02/
├── example/ProductSearchDaoTest.java   ← 동적 검색/정렬 순서/페이징 경계 (@MybatisTest)
├── example/ProductListApiTest.java     ← PageResponse 계약 + 파라미터 바인딩 + 400
├── exercise/PageContractExerciseTest.java ← totalPages 경계 3종 (순수 단위!)
└── answer/PageContractAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 PageResponse — 목록 응답의 계약

`List<Product>`만 주면 화면은 페이지네이션 바를 못 그립니다. 필요한 건 4종 세트:

| 필드 | 화면이 쓰는 곳 |
|------|----------------|
| `content` | 목록 본문 |
| `page`, `size` | 현재 위치 표시 |
| `totalCount` | "전체 12건" |
| `totalPages` | 페이지네이션 바의 끝 번호 |

totalPages는 **올림** 계산입니다 — `(totalCount + size - 1) / size`.
내림(`12/5=2`)이면 마지막 페이지의 2건이 화면에서 증발합니다.
이런 "산수"는 Spring 없이 **순수 단위 테스트**로 봉인합니다 (exercise가 그것).

### 3-2. 🆕 정렬 화이트리스트 — 이번 Step의 핵심

ORDER BY 컬럼명은 `#{}`(PreparedStatement 파라미터)로 넣을 수 없습니다 —
파라미터는 **값** 자리지 **문법** 자리가 아니니까요. 그래서 `${}`(문자열 치환)의
유혹이 오는데, `sort=price; DROP TABLE product--` 같은 입력이 그대로 SQL이 됩니다.

해법: **사용자 입력을 SQL에 넣지 않는다.** 입력은 '키'일 뿐, SQL은 우리가 미리 써둔
조각 중에서 고릅니다.

```xml
<choose>
    <when test='condition.sort == "priceAsc"'>ORDER BY price ASC, product_id ASC</when>
    <when test='condition.sort == "priceDesc"'>ORDER BY price DESC, product_id DESC</when>
    <when test='condition.sort == "name"'>ORDER BY name ASC, product_id ASC</when>
    <otherwise>ORDER BY product_id DESC</otherwise>
</choose>
```

이중 방어로 완성합니다:
- **바깥 막(Service)**: `ALLOWED_SORTS`에 없으면 `IllegalArgumentException` → 400.
  사용자에게 "그런 정렬 없어요"라고 정직하게 알린다.
- **안쪽 막(XML)**: 어떤 문자열이 새어 들어와도 `<otherwise>`로 — SQL이 될 길이 없다.

그리고 모든 정렬에 `product_id`가 붙어 있는 것, 보이나요?
같은 가격이 여럿이면 **순서가 실행마다 달라질 수 있습니다**(비결정 정렬) —
PK를 타이브레이커로 넣어 페이징 중복/누락을 막습니다.

### 3-3. `<sql>` 조각 공유 — search와 count는 한 몸

목록 쿼리와 카운트 쿼리의 WHERE가 다르면? "전체 12건"인데 목록엔 5건만 —
숫자가 안 맞는 화면이 됩니다. WHERE를 `<sql id="searchWhere">`로 한 번만 쓰고
`<include>`로 양쪽에서 공유하면 **구조적으로** 어긋날 수 없습니다.

```xml
<sql id="searchWhere">
    <where>
        <if test="condition.keyword != null">AND name LIKE CONCAT('%', #{condition.keyword}, '%')</if>
        <if test="condition.category != null">AND category = #{condition.category}</if>
    </where>
</sql>
```

`@Param("condition")`으로 넘겼으므로 XML에선 `condition.keyword`로 접근합니다.

### 3-4. OFFSET/FETCH — MS-SQL 페이징 (LIMIT 아님!)

```sql
ORDER BY ... OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY
```

MySQL의 `LIMIT`은 MS-SQL에 없습니다. 그리고 OFFSET/FETCH는 **ORDER BY가 필수** —
정렬 없는 페이징은 문법 오류입니다(순서가 없는데 '5번째부터'가 성립하지 않으니
당연한 제약). `offset = (page - 1) * size` 변환은 Service의 일입니다.

### 3-5. 정렬 테스트는 "순서"를 검증한다

`hasSize(12)`는 정렬을 검증하지 못합니다 — 어떤 순서든 12건이니까.
**첫 항목과 끝 항목**(또는 `extracting + containsExactly`)으로 순서 자체를 박제하세요.
시드 기준값(최저가 35000 무선 마우스, 최고가 410000 32인치 모니터)이 그래서 존재합니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step02.*"
```

1. `ProductMapper.xml`의 `<choose>`와 `searchWhere` 조각을 먼저 읽기
2. `ProductSearchDaoTest` — 동적 WHERE(키워드/복합/조건없음), 정렬 순서, 페이징 경계(5/5/2)
3. `ProductListApiTest` — PageResponse 5필드 계약, Captor로 바인딩 검증, 나쁜 sort → 400
4. **일부러 깨뜨려보기**: XML의 `priceAsc` `<when>`에서 `, product_id ASC`를 지우면?
   (지금은 가격이 전부 달라 통과하지만, 같은 가격 상품을 시드에 추가하면 비결정 정렬의
   민낯이 드러납니다 — 페이징과 조합되면 실서비스 장애)

## 5. Testing — exercise 풀기

`step02/exercise/PageContractExerciseTest.java`의 TODO 1~4를 채우세요.
`PageResponse.of()`는 순수 자바 — `@SpringBootTest`가 전혀 필요 없습니다.
**올림 / 정확히 나눠떨어짐 / 0건**, 계산 로직의 세 경계를 모두 다루는 것이 채점 포인트.

## 6. Lessons Learned

### 사례: 정렬 파라미터로 뚫린 관리자 페이지

- **증상**: 보안 점검에서 `?sort=name;WAITFOR DELAY '0:0:5'--`로 응답 5초 지연 재현
- **원인**: `ORDER BY ${sort}` — 사용자 입력이 SQL 문법 자리에 직접 치환됨
- **해결**: `<choose>` 화이트리스트 + Service 검증 (이중 방어)
- **교훈**: `${}`를 쓰는 순간 그 파라미터는 SQL의 일부다. ORDER BY처럼 `#{}`가
  불가능한 자리는 **입력을 넣지 말고 고르게 하라**.

### 사례: 2페이지에 또 나온 상품

- **증상**: 가격순 목록에서 1페이지 마지막 상품이 2페이지 첫머리에 또 등장
- **원인**: 동일 가격 상품들 사이 순서가 미정의 — 쿼리마다 다른 순서로 잘림
- **해결**: 모든 정렬에 PK 타이브레이커 (`price ASC, product_id ASC`)
- **교훈**: 페이징과 정렬은 세트다. 정렬이 결정적(deterministic)이어야 페이징이 안전하다.

### 시니어의 시선

> 목록 API 코드리뷰에서 제가 보는 순서: ① `${}`가 있는가 (있으면 그 자리에서 중단)
> ② count와 search의 WHERE가 같은 조각인가 ③ 정렬에 PK가 붙어 있는가
> ④ totalPages가 올림인가. 넷 다 통과하면 나머지는 취향입니다.

## 7. Key Takeaways

- 목록 응답은 PageResponse 규약 (content/page/size/totalCount/totalPages) — totalPages는 올림
- 정렬은 화이트리스트: Service 검증(400) + XML `<choose>` 고정 조각 (이중 방어, `${}` 금지)
- 모든 정렬에 PK 타이브레이커 — 비결정 정렬은 페이징 중복/누락을 만든다
- `<sql>` 조각 공유로 search/count WHERE 일치를 구조적으로 보장
- MS-SQL 페이징은 OFFSET/FETCH + ORDER BY 필수
- 계산 로직(totalPages)은 순수 단위로, SQL은 @MybatisTest로, HTTP 계약은 @WebMvcTest로 — 각자의 자리

## 8. Next Steps — 다음 Step의 문제

목록과 주문까지 갖췄습니다. 그런데 지금 주문 상태를 보세요 — 전부 `PENDING_PAYMENT`.
**결제가 없으니까요.**

결제는 우리 서버가 못 합니다. PG사(외부 API)를 불러야 하죠. 그 순간 새 문제들이:

> 테스트에서 진짜 PG를 호출할 건가요? (과금! 느림! 비결정!)
> PG가 응답을 안 주면 우리 API도 같이 멈추나요?

외부 세계와의 첫 만남 — **Step 3: 외부 결제 연동**에서 RestTemplate과
`@RestClientTest` + MockRestServiceServer를 배웁니다.
