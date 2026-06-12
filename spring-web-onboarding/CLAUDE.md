# WebFlow - 실무 API 종합 온보딩 모듈

> ⚠️ 이 파일은 `spring-web-onboarding` 모듈 전용 규칙입니다.
> 공통 규칙은 **루트 `CLAUDE.md`** 를 참조하세요.

---

## 🎯 모듈 개요

기본 CRUD를 넘어 **실무 API 서버의 잡기술**(외부 연동/파일/캐싱/스케줄링/운영)을
약 2주(하루 1~2시간, 자기주도) 안에 갖추게 하는 학습 모듈입니다.
TestCraft(테스트 기본기) 수료를 전제로 합니다.

- **커리큘럼**: `docs/web/curriculum/00-WebFlow-Curriculum.md` (필수 Step 1~9 + 심화 Step 10 서킷 브레이커)
- 도메인: **미니 커머스** (상품/주문 + 외부 결제·배송 연동)
- 학습 철학: 문제 주도형 + example/exercise/answer 3종 (TestCraft·BatchFlow와 동일)

---

## 🔒 기술 스택 (변경 불가)

```
Java:           1.8
Spring Boot:    2.7.17
MyBatis:        2.3.2 (3.x 금지 — Boot 3 전용)
H2:             인메모리 + MODE=MSSQLServer (DB명 webdb)
RestTemplate:   외부 연동 (RestTemplateBuilder 주입 — @RestClientTest 호환)
spring-retry:   재시도 (Boot BOM 관리, RetryTemplate 프로그래매틱 사용)
Resilience4j:   1.7.1 서킷 브레이커 (Java 8 호환 최종 라인 — 2.x는 Java 17 전용!)
                코어 라이브러리 프로그래매틱 사용 (spring-boot2 스타터 AOP 미사용)
Security:       ❌ 없음! (TestCraft에서 다룸 — 이 모듈은 실무 잡기술에 집중)
JPA:            ❌ 사용 금지
```

Security가 없으므로 @WebMvcTest에 `@Import(SecurityConfig)`/`with(csrf())`가 **불필요**하다
(보안 자동구성은 starter-security가 클래스패스에 있을 때만 활성).

---

## 🏗️ 패키지 구조

```
src/main/java/com/webflow/
├── WebFlowApplication.java
├── config/                  # CacheConfig(Step 6), SchedulingConfig(Step 7 — 테스트에선 off)
├── common/exception/        # BusinessException 계열 + ErrorResponse + Handler
├── product/                 # domain/ dao/ service/ controller/ dto/
├── order/                   # domain/ dao/ service/ controller/ dto/
├── external/                # 외부 연동 클라이언트
│   ├── payment/             # Step 3~4: PaymentClient
│   └── delivery/            # Step 9 캡스톤: DeliveryClient
├── file/                    # Step 5: 파일 저장/서비스
└── scheduler/               # Step 7: 정리 스케줄러

src/test/java/com/webflow/
├── step01/ ~ step10/        # {example, exercise, answer} 3종 (TestCraft 규약 — step10은 심화)
└── support/                 # 공통 테스트 지원
```

---

## 📜 핵심 규칙

### 외부 연동 (Step 3~)
- RestTemplate은 **RestTemplateBuilder로 생성** (직접 new 금지) — 타임아웃 명시 필수
- 외부 클라이언트는 `external/` 아래 `{대상}Client` — 외부 DTO와 내부 도메인을 섞지 않는다
- 재시도는 RetryTemplate(프로그래매틱) — 백오프 없는 재시도 금지
- 외부 장애가 우리 데이터를 망치지 않게: 실패 시 주문은 PENDING 보존 + 503

### DB/SQL
- `mybatis-mssql` 스킬 준수 (OFFSET/FETCH, IDENTITY, 예약어 회피 — orders!)
- 재고 차감 등 상태 변경은 **원자적 UPDATE**(WHERE 조건부) — affected 검증

### 테스트
- `spring-test-strategy` 스킬 준수 (계층 매트릭스, 3종 규약, 네이밍/AAA/AssertJ)
- 외부 연동: `@RestClientTest` + MockRestServiceServer (진짜 HTTP 금지!)
- 파일: MockMultipartFile + @TempDir / 캐싱: @MockBean DAO + verify(times) /
  스케줄: 로직 직접 호출(시각 주입)

## 📋 계획/태스크 운영 규칙 (MUST)

1. 작업 시작 전: `docs/web/plan/plan.md` 계획 추가 + `docs/web/plan/task.md` 등록
2. 커밋 시: task.md 체크 `[x]` + 해시 병기 (상세: `step-commit` 스킬)

## 📊 커밋 규칙

`✨ feat: [Web/Step N] 제목` + 학습 포인트 섹션. Step 단위 커밋, 매 커밋 전
`.\gradlew :spring-web-onboarding:test` 그린.

## 📚 교육 문서 규칙

- 파일: `docs/web/education/FOR-WebFlow-StepNN.md` (`education-doc` 스킬 템플릿)
- Step 추가/수정 시 00 커리큘럼 표 갱신

## 📚 참조 문서

| 문서 | 경로 |
|------|------|
| 커리큘럼 (학습 기준) | `docs/web/curriculum/00-WebFlow-Curriculum.md` |
| 계획/태스크 | `docs/web/plan/plan.md`, `task.md` |
| 테스트 치트시트 (전 모듈 공통) | `docs/test/skills/spring-test-annotations.md` |
