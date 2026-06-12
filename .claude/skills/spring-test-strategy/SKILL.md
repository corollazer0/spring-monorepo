---
name: spring-test-strategy
description: Spring 테스트를 설계·작성·리뷰할 때 사용. 어떤 검증을 어느 계층(순수 단위/Mockito/@MybatisTest/@WebMvcTest/@SpringBootTest/E2E)에서 할지 결정하는 매트릭스, 테스트 네이밍/구조 규약, 단골 함정 진단표를 제공한다.
---

# Spring 테스트 전략 표준

## 1. 계층 선택 매트릭스 — "무엇을 어디서 검증하나"

| 검증 대상 | 계층/도구 | 원칙 |
|----------|----------|------|
| 순수 로직(정책/유틸/도메인 규칙) | JUnit5 + AssertJ (Spring 없음) | 케이스를 가장 많이 — ms 단위 |
| Service 판단 로직 | `@ExtendWith(MockitoExtension)` + `@Mock`/`@InjectMocks` | Mock은 I/O 경계에만, 순수 협력자는 진짜 사용 |
| SQL/매핑 (MyBatis XML) | `@MybatisTest` + `Replace.NONE` | Mock은 SQL을 못 잡는다 — 진짜 DB(인메모리)로 |
| HTTP 계약(URL/JSON/상태코드) | `@WebMvcTest` + `@Import(SecurityConfig)` + `@MockBean` | jsonPath = 프론트와의 계약 봉인 |
| 화면(SSR) | `@WebMvcTest` + `view()`/`model()`/`content()` | 렌더링 HTML까지 검증 (Thymeleaf 한정) |
| 검증 규칙(@Valid) | 순수 Validator(케이스 多) + MockMvc(배선 1~2개) | 무거운 테스트에 케이스를 쌓지 마라 |
| 빈 연결 + 전체 흐름 | `@SpringBootTest` + MockMvc + `@Transactional` | Mock 없는 통합, 롤백 공짜 |
| 진짜 HTTP/로그인/쿠키 | `@SpringBootTest(RANDOM_PORT)` + TestRestTemplate | 여정의 "연결"만 소수 정예 |
| 외부 API 클라이언트(나가는 HTTP) | `@RestClientTest` + MockRestServiceServer | 진짜 HTTP 금지! 기대 선언 수+verify()로 재시도 횟수까지 봉인 |
| 파일 저장/검증 | 순수 단위 + `@TempDir` + MockMultipartFile | 경로는 생성자 주입 — 실제 경로 오염 금지, 정리는 자동 |
| 캐시/차단기 등 상태 가진 부품 | `@SpringBootTest` + `@MockBean` DAO + verify(times) | 증거는 응답이 아니라 호출 횟수. 상태는 @BeforeEach에서 clear/reset |
| 스케줄·시간 의존 로직 | 트리거(@Scheduled)와 로직 분리 → 시각/상태를 주입해 직접 호출 | 새벽 3시도, OPEN 10초도 기다리지 않는다 |
| Batch Job/Step | `@SpringBatchTest` + JobLauncherTestUtils | @BeforeEach removeJobExecutions 격리 + StepExecution 카운트 검증 |
| 구조 규칙(계층 의존/네이밍) | ArchUnit (`ClassFileImporter` + `noClasses()/classes()`) | 동작이 아니라 의존 그래프를 봉인 — 규칙엔 because 필수 |
| API 문서 계약 | `@AutoConfigureRestDocs` + `document()` | 테스트 통과가 문서 생성 조건 — 필드 누락/허위는 실패 |

**피라미드 원칙**: 위(빠른 계층)에 많이, 아래(느린 계층)에 적게. 같은 검증을 두 계층에서 중복하지 않는다.

## 2. 작성 규약

- 메서드명: `{대상}_{시나리오}_{예상결과}` (한글 허용) + `@DisplayName` 병기
- 구조: `// given` `// when` `// then` 주석 필수, 시나리오당 테스트 1개
- 단언: **AssertJ만** (`assertThat`, `assertThatThrownBy` + 메시지까지 검증). JUnit Assertions 금지
- Mockito: BDD 스타일(`given/willReturn`), void는 `willThrow(...).given(...)`, 행위 검증에 `never()` 적극 사용, 전달 객체는 `ArgumentCaptor`
- 반복 입력은 `@ParameterizedTest`, 그룹화는 `@Nested`, 준비 코드 중복은 Fixture(Object Mother)로

## 3. 품질 게이트 (작성 후 자가 검증)

1. **변이 검증**: 프로덕션을 일부러 망가뜨려 테스트가 깨지는지 확인 — 안 깨지면 가짜 테스트
2. **반복 실행**: 두 번 연속 통과해야 완성 (특히 RANDOM_PORT E2E는 롤백 불가 → `@Sql(AFTER_TEST_METHOD)` 정리)
3. **격리**: ThreadLocal(SecurityContextHolder/MDC) 사용 시 정리했는가

## 4. 단골 함정 진단표 (증상 → 1순위 의심)

| 증상 | 원인 |
|------|------|
| permitAll인데 401 | @WebMvcTest에 `@Import(SecurityConfig)` 누락 |
| 로그인했는데 403 | `with(csrf())` 누락 — CSRF가 인증보다 먼저 |
| 화면 미인증이 302가 아닌 401 | `accept(TEXT_HTML)` 누락 — 브라우저로 인식 안 됨 |
| DB 방언 SQL이 문법 오류 | `@AutoConfigureTestDatabase(replace=Replace.NONE)` 누락 |
| E2E 재실행 시 실패 | RANDOM_PORT 롤백 불가 + 정리 누락 |
| 혼자는 통과, 같이 돌리면 실패 | ThreadLocal 미정리 또는 테스트 간 데이터 의존 |
| UnnecessaryStubbingException | 그 테스트에서 안 일어나는 호출을 stubbing — 시나리오 오해 신호 |
| 갑자기 느려진 빌드 | @MockBean 구성 차이로 컨텍스트 캐시 무효화 |

> 이 레포에서는 상세 치트시트가 `docs/test/skills/spring-test-annotations.md`에 있고,
> 실전 예제가 세 모듈의 example 패키지에 있다 —
> 기본기: `spring-test-onboarding` (step01~12 + advanced/step10~14: JWT·품질·ArchUnit·RESTDocs) /
> 외부연동·파일·캐시·스케줄: `spring-web-onboarding` (step01~10) /
> 배치·비동기·운영: `spring-batch-onboarding` (step01~13 + advanced/step14~17).
