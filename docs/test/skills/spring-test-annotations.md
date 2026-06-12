# Spring 테스트 어노테이션 치트시트 — "언제 무엇을 쓰나"

> TestCraft 전 과정의 요약. 새 테스트를 만들기 전에 이 표에서 출발하세요.

---

## 1. 테스트 종류 선택표

| 검증하고 싶은 것 | 도구 | 뜨는 빈 | 속도 | 배운 곳 |
|----------------|------|---------|------|--------|
| 순수 로직 (정책, 유틸, 도메인 규칙) | JUnit5 + AssertJ만 | 없음 | ms | Step 1 |
| Service 판단 로직 (협력자 있음) | `@ExtendWith(MockitoExtension.class)` | 없음 (Mock 주입) | ms | Step 2 |
| MyBatis SQL / 매핑 | `@MybatisTest` + `Replace.NONE` | MyBatis+DataSource | ~1s | Step 3 |
| URL/JSON/상태코드/검증/보안 경계 | `@WebMvcTest` + `@Import(SecurityConfig)` | MVC 레이어 | ~2s | Step 4~6 |
| 화면(SSR): 뷰 이름/모델/렌더링 HTML | `@WebMvcTest` + `view()`/`model()`/`content()` | MVC+Thymeleaf | ~2s | Step 12 |
| Filter/Interceptor 내부 분기 | 서블릿 Mock 3총사 (new로 직접) | 없음 | ms | Step 7 |
| 빈 연결 + 전체 흐름 (가짜 HTTP) | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional` | 전부 | 수 초 | Step 8 |
| 진짜 HTTP / 진짜 로그인 / 쿠키 | `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate` | 전부+Tomcat | 수 초+ | Step 8 |
| 구조 규칙 (계층 의존/네이밍/모듈 격리) | ArchUnit `ClassFileImporter` + `noClasses()/classes()` | 없음 (바이트코드 분석) | ~수 초 (import 1회) | Step 13 |
| API 문서 + 계약 (필드 누락/허위 검증) | `@WebMvcTest` + `@AutoConfigureRestDocs` + `document()` | MVC 레이어 | ~2s | Step 14 |

**기본 원칙: 표의 위쪽(빠른 것)에 케이스를 많이, 아래쪽(느린 것)에 여정만 소수 정예.**

---

## 2. 자주 쓰는 어노테이션/도구 요약

### 단위 (Spring 없음)
| 도구 | 용도 |
|------|------|
| `@Test`, `@DisplayName`, `@Nested`, `@BeforeEach` | 기본 구조/그룹화/격리 |
| `@ParameterizedTest` + `@ValueSource`/`@CsvSource`/`@NullAndEmptySource` | 입력만 다른 반복 |
| `assertThat`, `assertThatThrownBy(...).hasMessageContaining` | 단언 (AssertJ만 사용) |
| `@Mock` / `@InjectMocks` / `given().willReturn()` / `willThrow().given()` (void용) | Mockito |
| `verify(mock)`, `then(mock).should(never())`, `ArgumentCaptor` | 행위 검증 |
| `MockHttpServletRequest/Response`, `MockFilterChain`, 람다 FilterChain | 필터/인터셉터 단위 |
| `Validation.buildDefaultValidatorFactory().getValidator()` | DTO 검증 규칙 단위 |

### 슬라이스
| 도구 | 용도 | 함정 |
|------|------|------|
| `@MybatisTest` | DAO+XML 검증, 자동 롤백 | `@AutoConfigureTestDatabase(replace=Replace.NONE)` 없으면 DataSource 바꿔치기! |
| `@WebMvcTest(X.class)` | HTTP 계약 검증 | `@Import(SecurityConfig.class)` 없으면 전부 401! |
| `@MockBean` | 컨테이너의 빈을 Mock으로 교체 | 구성이 다르면 컨텍스트 캐시가 깨져 느려진다 |
| `jsonPath("$.field")`, `header().string(...)`, `andDo(print())` | 응답 검증/디버깅 | |
| `@AutoConfigureRestDocs` + `andDo(document(...))` | 테스트 통과 시에만 문서 스니펫 생성 | 경로 변수는 `RestDocumentationRequestBuilders`로! (Step 14) |

### 보안 (spring-security-test)
| 도구 | 용도 |
|------|------|
| `@WithMockUser(username, roles)` | 로그인 상태 흉내 (절차 생략) |
| `@WithMockMember(...)` (커스텀) | 도메인 principal(LoginMember)까지 필요할 때 |
| `with(csrf())` | POST/PUT/DELETE 필수 — 없으면 403 (인증보다 먼저!) |
| `with(user("name"))` | 요청 단위 인증 지정 |
| 401 vs 403 | 인증 없음("너 누구야") vs 인가 실패("너 안 돼") |

### 통합/E2E
| 도구 | 용도 | 함정 |
|------|------|------|
| `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional` | Mock 없는 전체 흐름, 롤백 공짜 | |
| `@SpringBootTest(webEnvironment=RANDOM_PORT)` + `TestRestTemplate` | 진짜 HTTP/로그인 | **롤백 불가!** `@Sql(AFTER_TEST_METHOD)`로 정리 |
| `@Sql(scripts, executionPhase)` | 데이터 준비/정리 | |

---

## 3. 단골 미스터리 진단표

| 증상 | 1순위 의심 |
|------|-----------|
| permitAll인데 401 | @WebMvcTest에 `@Import(SecurityConfig)` 누락 (Step 4) |
| 화면 미인증이 302가 아닌 401 | MockMvc에 `accept(TEXT_HTML)` 누락 — 브라우저로 인식 안 됨 (Step 12) |
| 폼 검증 실패가 JSON 400으로 | `BindingResult` 파라미터 누락 (@Valid 바로 뒤!) (Step 12) |
| 로그인했는데 403 | `with(csrf())` 누락 — CSRF가 인증보다 먼저 (Step 6) |
| MS-SQL 문법이 H2에서 문법 오류 | `Replace.NONE` 누락 (Step 3) |
| 한글 데이터가 ??? | 인코딩 — sql.init.encoding / compile encoding (Step 3) |
| E2E 첫 실행 통과, 재실행 실패 | RANDOM_PORT 롤백 불가 + 정리 누락 (Step 8) |
| 혼자 돌리면 통과, 같이 돌리면 실패 | ThreadLocal(SecurityContextHolder/MDC) 미정리 (Step 7) |
| UnnecessaryStubbingException | 그 테스트에서 안 일어나는 호출을 stubbing (Step 2) |
| 테스트가 갑자기 느려짐 | @MockBean 구성 차이로 컨텍스트 캐시 무효화 (Step 11) |
| REST Docs "urlTemplate not found" | `MockMvcRequestBuilders` 사용 — pathParameters는 `RestDocumentationRequestBuilders` (Step 14) |
| REST Docs 필드 누락/잉여로 실패 | 문서↔페이로드 불일치(설계된 동작!) — 스텁 응답의 null 필드도 의심 (Step 14) |

---

## 4. 컨텍스트 캐싱 — 빌드가 느려질 때 보는 절

Spring은 테스트 컨텍스트를 **설정이 같으면 재사용(캐싱)** 합니다. 캐시 키에 들어가는 것:
사용한 어노테이션 구성, @MockBean 대상 목록, 프로퍼티, @ActiveProfiles 등.

- `@MockBean BoardService` 가 있는 클래스와 없는 클래스는 **서로 다른 컨텍스트** → 각각 기동
- 통합 테스트들이 같은 구성을 공유하도록 베이스 클래스/공통 어노테이션으로 통일하면 기동 1회로 수렴
- `@DirtiesContext`는 캐시를 버리는 폭탄 — 정말 필요한지 세 번 의심하라
- 진단법: 빌드 로그에서 Spring 배너(또는 "Started ...Application")가 몇 번 찍히는지 세어보라
