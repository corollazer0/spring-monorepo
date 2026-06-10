# [Step 9 캡스톤] 댓글 기능 — 요구사항 명세 & 평가 체크리스트

> 이 문서는 캡스톤의 **요구사항 명세서**입니다. 프로덕션 코드는 완성되어 제공됩니다
> (`com.testonboarding.comment` 패키지) — **여러분의 과제는 테스트 전략 수립과 작성**입니다.

---

## 1. 기능 요구사항

### R1. 댓글 목록 조회 — `GET /api/posts/{postId}/comments`
- 누구나(비로그인 포함) 조회할 수 있다
- 댓글은 **등록순**(오래된 것 먼저)으로 반환된다
- 댓글이 없으면 빈 배열 `[]`을 반환한다 (에러가 아니다)

### R2. 댓글 작성 — `POST /api/posts/{postId}/comments`
- **로그인 사용자만** 작성할 수 있다 (비로그인 → 401)
- 내용(content)은 필수이며 500자 이하 (위반 → 400 + fieldErrors)
- **존재하지 않는 게시글**에는 작성할 수 없다 (→ 404)
- 성공 시 **201 Created** + `Location: /api/comments/{새 댓글 ID}` 헤더
- 작성자(writer)는 **로그인한 사용자**로 기록된다 (요청 본문으로 위조 불가)

### R3. 댓글 삭제 — `DELETE /api/comments/{commentId}`
- **작성자 본인만** 삭제할 수 있다 (타인 → 403)
- 존재하지 않는 댓글 → 404
- 성공 시 **204 No Content**

---

## 2. 제공되는 것

| 항목 | 위치 |
|------|------|
| 프로덕션 코드 전체 | `src/main/java/com/testonboarding/comment/` |
| 댓글 테이블 DDL | `schema.sql`의 `comment` 테이블 |
| 시드 데이터 | `data.sql` — 1번 글에 2건(writer2→writer1 등록순), 2번 글에 1건, 3번 글엔 없음 |
| 예외→상태코드 매핑 | `GlobalExceptionHandler` (404/403 추가됨) |
| E2E 도우미 | `support/RestSessionHelper` |

---

## 3. 과제

`src/test/java/com/testonboarding/step09/exercise/` 아래에 테스트를 작성하세요.
**어떤 요구사항을 어느 계층(단위/슬라이스/E2E)에서 검증할지부터 결정**하는 것이 진짜 과제입니다.

권장 구성 (강제는 아닙니다 — 본인의 전략에 이유가 있다면 그게 정답):

| 클래스 | 도구 | 참고 Step |
|--------|------|----------|
| `CommentServiceTest` | Mockito | Step 2 |
| `CommentDaoTest` | @MybatisTest | Step 3 |
| `CommentControllerTest` | @WebMvcTest | Step 4~6 |
| `CommentE2eTest` | RANDOM_PORT | Step 8 |

---

## 4. 평가 체크리스트 (자가 채점)

### 전략 설계 (가장 중요!)
- [ ] 각 테스트가 "왜 이 계층에서" 검증되는지 설명할 수 있다
- [ ] 같은 검증이 여러 계층에서 중복되지 않는다 (피라미드 위로 갈수록 적게)
- [ ] E2E는 "여정의 연결" 1~2본으로 절제했다

### Service 단위 (Mockito)
- [ ] 없는 게시글에 작성 → 예외 + **insert 미호출**(never)까지 검증
- [ ] 정상 작성 → ArgumentCaptor로 postId/writer/content가 채워졌는지 검증
- [ ] 타인 댓글 삭제 → 예외 + **delete 미호출** 검증
- [ ] 본인 댓글 삭제 → delete 호출 verify
- [ ] 없는 댓글 삭제 → CommentNotFoundException

### DAO (@MybatisTest)
- [ ] `@AutoConfigureTestDatabase(replace = Replace.NONE)`을 붙였다 (왜인지 안다)
- [ ] 등록순 정렬을 **순서까지** 검증했다 (건수만 세지 않았다)
- [ ] 댓글 없는 글 → null이 아닌 **빈 목록** 검증
- [ ] insert 후 **재조회**로 왕복 검증했다 (PK 채번 + DEFAULT 컬럼)

### Controller (@WebMvcTest)
- [ ] `@Import(SecurityConfig.class)`를 붙였다 (안 붙이면 무슨 일이 나는지 안다)
- [ ] 비로그인 조회 200 / 비로그인 작성 401 — permitAll 경계 검증
- [ ] 로그인 작성 201 + **Location 헤더** 검증
- [ ] 빈 내용 400 + **fieldErrors의 field가 content**인지 검증
- [ ] 없는 게시글 404 — advice 번역까지 확인 (message 검증)
- [ ] POST에 `with(csrf())`를 빼먹지 않았다

### E2E (RANDOM_PORT)
- [ ] 진짜 로그인(시드 계정) → 작성 → 목록에서 확인의 "연결"을 검증했다
- [ ] 만든 데이터를 `@Sql(AFTER_TEST_METHOD)`로 치웠다 (두 번 연속 실행해도 통과한다!)

### 공통 품질
- [ ] 테스트명이 `{대상}_{시나리오}_{예상결과}` 규칙을 따른다
- [ ] given/when/then 구조가 보인다
- [ ] 프로덕션 코드를 일부러 망가뜨렸을 때(예: 소유자 검증 제거) 테스트가 깨진다

---

## 5. 막혔을 때

30분 이상 같은 곳에서 막히면 `step09/answer/`의 모범답안을 보세요.
보고 이해했다면 **answer를 닫고 스스로 다시 작성** — 그래야 남습니다.
