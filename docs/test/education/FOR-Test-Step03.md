# [Step 3] DAO 테스트: @MybatisTest + H2 (MSSQLServer 모드)

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: `@MybatisTest`, `@AutoConfigureTestDatabase(replace = Replace.NONE)`, `schema.sql`/`data.sql`, MyBatis XML(동적 SQL, useGeneratedKeys), MS-SQL 페이징(OFFSET/FETCH)
> **코드 위치**: `spring-test-onboarding/src/test/java/com/testonboarding/step03/`

---

## 1. Before We Start — Step 2의 한계

Step 2에서 Mock 덕분에 DB 없이 Service를 테스트했습니다. 그런데 이런 XML이 있다면?

```xml
<select id="findById" resultType="Post">
    SELECT post_id, writer, title, content, created_at
    FROM post
    WHERE psot_id = #{postId}   <!-- 오타: psot_id -->
</select>
```

**Step 2의 테스트는 전부 통과합니다.** Mock은 XML을 읽지 않으니까요.
SQL 오타, 컬럼-필드 매핑 실수, 페이징 문법 오류, 동적 SQL의 조건 누락 —
이것들은 **진짜 DB에 SQL을 실행해봐야만** 잡을 수 있습니다.

그렇다고 매번 사내 개발 DB에 붙는다면? 느리고, 누가 데이터를 건드리면 테스트가 흔들리고,
내 테스트가 남의 데이터를 망가뜨립니다. 필요한 건 **"내 테스트 전용으로, 메모리에서, 1초 만에
떴다 사라지는 진짜 DB"** — 그것이 인메모리 H2입니다.

---

## 2. What We're Building

```
src/main/
├── java/com/testonboarding/
│   ├── board/dao/BoardDao.java        ← @Mapper 부착 (example 대상)
│   └── member/dao/MemberDao.java      ← @Mapper 부착 (exercise 대상)
└── resources/
    ├── schema.sql                     ← MS-SQL 스타일 DDL (IDENTITY, NVARCHAR, GETDATE())
    ├── data.sql                       ← 시드: 회원 3명, 게시글 15건
    └── mybatis/mapper/
        ├── BoardMapper.xml            ← 페이징(OFFSET/FETCH), 동적 검색(<where><if>)
        └── MemberMapper.xml

src/test/java/com/testonboarding/step03/
├── example/BoardDaoTest.java          ← 완성본
├── exercise/MemberDaoExerciseTest.java
└── answer/MemberDaoAnswerTest.java
```

검증할 것:

| 검증 대상 | 잡아내는 버그 |
|----------|--------------|
| insert 후 PK 채번 (useGeneratedKeys) | keyProperty 오타, IDENTITY 누락 |
| insert 후 **재조회** | 컬럼-필드 매핑 실수, DEFAULT 미동작 |
| OFFSET/FETCH 페이징 | 페이지 크기/순서 계산 오류 |
| `<where>` + `<if>` 동적 검색 | 조건 빠짐, AND 중복, 전체조회 누락 |
| enum(Role) ↔ 문자열 왕복 | enum 이름 변경 시 매핑 깨짐 |

---

## 3. Core Concepts

### 3-1. 슬라이스 테스트 — 필요한 조각만 띄운다

`@SpringBootTest`는 모든 Bean(Controller, Service, Security...)을 띄웁니다. SQL 하나 검증하는 데
그럴 필요가 없죠. `@MybatisTest`는 **MyBatis + DataSource 관련 Bean만** 띄웁니다.

```
@SpringBootTest  : ████████████████████ 전부 (수 초)
@MybatisTest     : ████ MyBatis+DataSource만 (~1초)
```

이렇게 특정 레이어만 잘라 띄우는 것을 **슬라이스 테스트(slice test)** 라고 합니다.
Step 4의 `@WebMvcTest`(MVC 레이어만)도 같은 가족입니다.

### 3-2. ⚠️ 이 Step 최대의 함정: Replace.NONE

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)  // 필수!
class BoardDaoTest { ... }
```

`@MybatisTest`는 기본적으로 "친절하게도" 설정된 DataSource를 무시하고
**자기 마음대로 기본 H2로 바꿔치기**합니다(replace = ANY가 기본값).
그러면 우리가 정성껏 설정한 `MODE=MSSQLServer` URL이 무시되고,
MS-SQL 문법(OFFSET/FETCH 등)이 실패합니다.

`Replace.NONE`은 "바꿔치기하지 말고 내 application.yml의 DataSource를 그대로 써라"는 뜻입니다.
**exercise에서 이 어노테이션을 지우고 실행해보세요** — 직접 겪어보면 절대 안 잊습니다.

### 3-3. schema.sql / data.sql — 모든 테스트의 공통 출발선

Spring Boot는 embedded DB 기동 시 `schema.sql`(DDL) → `data.sql`(시드)을 자동 실행합니다.
모든 테스트는 "회원 3명, 게시글 15건"이라는 **동일한 출발선**에서 시작합니다.

### 3-4. 트랜잭션 자동 롤백 — 테스트 격리의 비밀

`@MybatisTest`는 각 테스트를 트랜잭션 안에서 실행하고 **끝나면 자동 롤백**합니다.

```
테스트A: 1번 글 삭제 → 검증 → 롤백 (DB 원상복구)
테스트B: count() == 15 → 통과!  (A의 삭제가 보이지 않는다)
```

그래서 example의 `count_롤백덕분에_항상시드그대로` 테스트가 통과하는 겁니다.
테스트 실행 순서가 바뀌어도, 골라서 하나만 돌려도 결과가 같습니다(F.I.R.S.T의 Isolated).

### 3-5. insert 검증은 반드시 "재조회"로

```java
boardDao.insert(post);
assertThat(post.getPostId()).isNotNull();      // (1) 채번 확인 — useGeneratedKeys 동작
Post found = boardDao.findById(post.getPostId());
assertThat(found.getTitle()).isEqualTo("새 글"); // (2) 재조회 — 매핑까지 왕복 검증
assertThat(found.getCreatedAt()).isNotNull();   // (3) DEFAULT GETDATE()가 채웠는지
```

객체만 들여다보면 "INSERT는 됐는데 SELECT 매핑이 깨진" 버그를 놓칩니다.
**쓰기 검증은 읽기로 완성됩니다.**

### 3-6. MS-SQL 스타일 SQL — 실무와 같은 문법으로

| 용도 | MS-SQL 스타일 (이 모듈) | MySQL이라면 |
|------|------------------------|-------------|
| 페이징 | `ORDER BY ... OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY` | `LIMIT 10 OFFSET 0` |
| 자동 채번 | `BIGINT IDENTITY` | `AUTO_INCREMENT` |
| 현재 시각 | `GETDATE()` | `NOW()` |
| 문자열 연결 | `CONCAT('%', #{title}, '%')` | 동일 |

> OFFSET/FETCH는 **ORDER BY 없이는 문법 오류**입니다. "정렬 없는 페이징은 무의미하다"는
> 사실을 문법이 강제하는 셈이죠.

### 3-7. ⚠️ H2 호환 모드 ≠ 진짜 MS-SQL

H2의 `MODE=MSSQLServer`는 어디까지나 흉내입니다. 차이를 알고 써야 합니다.

| 구분 | H2(MSSQLServer 모드)에서 동작 | 비고 |
|------|------------------------------|------|
| `OFFSET/FETCH`, `GETDATE()`, `NVARCHAR`, `[대괄호]` 식별자 | ✅ | 이 모듈에서 사용 |
| `BIGINT IDENTITY` | ✅ | 단, `IDENTITY(1,1)` 시드 지정 문법은 ❌ |
| `OUTPUT` 절, 저장 프로시저(T-SQL), `TOP ... WITH TIES` | ❌ | 실서버 전용 — 이 모듈에서 금지 |
| 트랜잭션 힌트(`WITH (NOLOCK)` 등) | ❌ | 실서버 전용 |

**교훈**: H2 테스트가 통과해도 실서버에서 처음 실행되는 SQL은 한 번은 실서버(개발계)에서
확인하라. H2 테스트는 "거의 모든" 버그를 잡지만 "전부"는 아니다.

---

## 4. Step-by-Step — example 따라잡기

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step03.*"
```

`BoardDaoTest`를 읽는 순서:

1. 클래스 어노테이션 2개의 의미 (특히 Replace.NONE)
2. `Insert` — useGeneratedKeys + 재조회 + DEFAULT 검증 3단 콤보
3. `FindPage` — OFFSET/FETCH, 15건이 10+5로 나뉘는지
4. `Search` — 동적 SQL 3분기: 키워드만 / 복합 / 조건 없음(전체)
5. `UpdateAndDelete` — 마지막 `count` 테스트가 롤백의 증거

**일부러 깨뜨려보기**: `BoardMapper.xml`의 `findById`에서 `post_id`를 `psot_id`로 바꿔보세요.
Step 2 테스트는 멀쩡하고 Step 3 테스트만 깨집니다 — 두 Step의 역할 분담이 그대로 보입니다.

---

## 5. Testing — exercise 풀기

`step03/exercise/MemberDaoExerciseTest.java`의 `@Disabled`를 지우고 TODO 1~7을 채우세요.

추가 실험(강력 추천): `@AutoConfigureTestDatabase(replace = Replace.NONE)`를 지우고 실행해보세요.
어떤 에러가 나는지, 왜 나는지 설명할 수 있다면 이 Step은 졸업입니다.

---

## 6. Lessons Learned

### 사례 1: 한글 데이터가 ??? 로 깨진다 (이 모듈을 만들 때 실제로 겪은 버그)

- **증상**: `search_제목키워드` 테스트가 0건 반환으로 실패. 시드 데이터의 한글이 깨져 있었다
- **원인**: Windows의 기본 charset은 cp949. `data.sql`(UTF-8)을 cp949로 읽어 한글이 전부 깨짐
- **해결**: `spring.sql.init.encoding: UTF-8` + Gradle `options.encoding = 'UTF-8'` 명시
- **교훈**: "내 PC에서만 깨져요"의 단골 범인은 인코딩이다. 인코딩은 **명시적으로 고정**하라.
  그리고 이 버그를 잡아낸 것도 테스트였다 — 시드 데이터 검증 테스트가 없었다면
  Controller 단계에서 한참을 헤맸을 것이다.

### 사례 2: Replace.NONE 누락

- **증상**: application.yml에 분명 MSSQLServer 모드를 설정했는데 MS-SQL 문법이 문법 오류
- **원인**: @MybatisTest가 DataSource를 기본 H2(REGULAR 모드)로 교체
- **해결**: `@AutoConfigureTestDatabase(replace = Replace.NONE)`
- **교훈**: 슬라이스 테스트는 "자동 구성"이 많다. 마법처럼 편하지만, 마법의 기본값이
  내 의도와 다를 수 있다 — 슬라이스가 무엇을 자동으로 하는지 한 번은 의심하라.

### 사례 3: 같은 timestamp 정렬에 의존하는 페이징 테스트

`created_at`이 전부 같은 시각(시드가 한 번에 들어가므로)인데 `ORDER BY created_at`으로
페이징하면 **순서가 실행마다 달라질 수 있습니다**. 그래서 이 모듈은 `ORDER BY post_id DESC`를
씁니다. **정렬 기준은 유일성이 보장되는 컬럼(PK 등)을 포함**해야 페이징 테스트가 흔들리지 않습니다.

### 시니어의 시선

> DAO 테스트의 진짜 가치는 "지금 SQL이 맞다"가 아니라 **"SQL을 고칠 용기"** 를 주는 데 있습니다.
> 운영 3년 차 시스템에서 아무도 안 건드리려 하는 200줄짜리 동적 SQL — 테스트가 있다면
> 리팩토링하고 5분 안에 전체 검증이 끝납니다. 테스트가 없다면? 아무도 안 건드리고,
> 그 SQL은 계속 자라나 괴물이 됩니다.

---

## 7. Key Takeaways

- Mock은 SQL을 검증하지 못한다 — XML의 SQL은 `@MybatisTest`로 진짜 DB에 실행해 검증한다
- `@AutoConfigureTestDatabase(replace = Replace.NONE)` 없이는 MSSQLServer 모드 설정이 무시된다
- 각 테스트는 자동 롤백 — 시드 데이터라는 같은 출발선에서 항상 시작한다
- 쓰기(insert/update) 검증은 재조회(읽기)로 완성한다
- H2 호환 모드는 흉내일 뿐 — 무엇이 되고 안 되는지(비교표) 알고 쓰라

---

## 8. Next Steps — 다음 Step의 문제

이제 Service(Step 2)도 DAO(Step 3)도 검증됐습니다. 그럼 이 요청은 잘 동작할까요?

```
GET /api/posts/1
```

- URL이 `getPost`에 매핑되는 건 누가 보장하죠?
- `Post` 객체가 JSON으로 바뀔 때 필드 이름이 기대대로인 건?
- 없는 글이면 HTTP **404**가 나가는 건? (PostNotFoundException은 Service의 일이고, 404는?)

Controller는 "URL 매핑, 파라미터 바인딩, JSON 직렬화, 상태코드"라는 **완전히 다른 관심사**의
세계입니다. Service/DAO가 멀쩡해도 여기서 얼마든지 깨집니다.
**Step 4에서 @WebMvcTest + MockMvc로 HTTP 세계를 테스트합니다.**
