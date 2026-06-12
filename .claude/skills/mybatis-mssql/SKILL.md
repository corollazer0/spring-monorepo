---
name: mybatis-mssql
description: MyBatis Mapper(XML)나 MS-SQL 스타일 SQL을 작성·수정·테스트할 때 사용. MS-SQL 방언 규칙, H2 호환 모드(MODE=MSSQLServer)의 한계, Mapper XML 작성 표준, @MybatisTest 설정을 정의한다.
---

# MyBatis + MS-SQL 표준

## 1. MS-SQL 방언 규칙 (이 조직의 실무 DB)

| 용도 | 표준 문법 | 금지 |
|------|----------|------|
| 페이징 | `ORDER BY ... OFFSET n ROWS FETCH NEXT m ROWS ONLY` (ORDER BY 필수!) | `LIMIT` |
| 자동 채번 | `BIGINT IDENTITY` + `useGeneratedKeys="true" keyProperty="..."` | 시퀀스 가정 |
| 현재 시각 | `GETDATE()` | `NOW()` |
| 문자열 연결 | `CONCAT(...)` (H2와 양쪽 호환) | `+` 연결, `||` |
| 문자 컬럼 | `NVARCHAR` (한글) | |

페이징 ORDER BY는 **유일성이 보장되는 컬럼(PK 포함)** 기준 — 동률 정렬은 페이징 테스트를 흔든다.

## 2. H2 호환 모드의 한계 — 알고 써라

테스트 DB URL: `jdbc:h2:mem:...;MODE=MSSQLServer;DATABASE_TO_LOWER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE`

| H2에서 동작 | H2에서 불가 (실서버 전용 — 사용 금지) |
|------------|--------------------------------------|
| OFFSET/FETCH, GETDATE(), NVARCHAR, [대괄호] 식별자, BIGINT IDENTITY | `IDENTITY(1,1)` 시드 지정, OUTPUT 절, 저장 프로시저(T-SQL), TOP ... WITH TIES, WITH (NOLOCK) 힌트 |

H2 테스트 통과 ≠ 실서버 보장 — 처음 쓰는 문법은 개발계 실서버에서 1회 확인.

## 3. Mapper XML 표준

- 인터페이스에 `@Mapper`, XML namespace = 인터페이스 FQCN, 메서드명 = statement id
- 공통 컬럼은 `<sql id>` + `<include>`로 중복 제거
- 동적 SQL: `<where>` + `<if test="x != null and x != ''">` — AND 처리를 `<where>`에 맡긴다
- **동적 정렬은 `${}` 금지** — 사용자 입력을 ORDER BY에 치환하면 SQL 인젝션.
  `<choose>`로 고정 SQL 조각 중 "선택"만 + Service단 화이트리스트 검증 (이중 방어)
- 검색 목록과 카운트는 WHERE를 `<sql id>` 조각으로 공유 — 둘이 어긋나면 페이지 수가 거짓말한다
- 상태 변경(재고 차감 등)은 **조건부 UPDATE 한 문장 + affected 검증** — SELECT 후 UPDATE는 경쟁 조건
- `map-underscore-to-camel-case: true` 전제 — 컬럼 snake_case, 필드 camelCase
- 도메인 객체에 매핑용 기본 생성자 + setter 필요 (keyProperty 채번 포함)
- enum은 이름 문자열로 저장(기본 EnumTypeHandler) — enum 상수명 변경은 DB 마이그레이션 사안

## 4. DAO 테스트 필수 설정

```java
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 없으면 호환 모드 URL 무시됨!
```

- schema.sql/data.sql 자동 로드 + 테스트별 자동 롤백 = 시드 데이터가 공통 출발선
- 쓰기 검증은 **재조회**로 완성 (채번 + DEFAULT 컬럼 + 매핑 왕복)
- SQL 스크립트 인코딩은 UTF-8 명시(`spring.sql.init.encoding: UTF-8`) — Windows cp949 함정

> 이 레포의 모범: `spring-test-onboarding`(기본 CRUD — step03 테스트),
> `spring-web-onboarding`(동적 검색 + `<choose>` 정렬 화이트리스트 + 원자적 UPDATE — step01~02 테스트)의
> `src/main/resources/mybatis/mapper/*.xml` + `schema.sql`.
