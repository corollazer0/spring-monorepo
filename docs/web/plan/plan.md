# WebFlow 계획서 (Living Document)

> ⚠️ **운영 규칙**: spring-web-onboarding 관련 작업은 시작 전에 이 문서에 계획을 추가하고,
> 완료 시 [task.md](./task.md)를 체크한다. (규칙: `step-commit` 스킬)

---

## Plan 1. WebFlow 마스터플랜 — "실무 API 종합" 커리큘럼 (설계 확정, 구현 미착수)

### 배경/목표
TestCraft(테스트 기본기)와 BatchFlow(배치)가 완성된 상태에서, 세 번째 모듈의 정체성을
**실무 API 서버 잡기술 종합**으로 확정 — TestCraft가 다루지 않은 실무 주제
(외부 연동/파일/캐싱/스케줄링/운영)를 같은 형식으로 가르친다.

### 핵심 의사결정 (2026-06-11 인터뷰로 확정)
| 결정 | 선택 | 이유 |
|------|------|------|
| 정체성 | **실무 API 종합** (JPA 중심 아님) | TestCraft와의 차별화 — 도구가 아닌 "실무 장면" 중심 |
| 필수 주제 | 외부 API 연동 / 파일 업·다운로드 / 캐싱+성능 / 스케줄링+Actuator **전부** | 인터뷰에서 4개 모두 선택됨 |
| DB | MyBatis + H2(MODE=MSSQLServer) | TestCraft·실무 일관, 새 주제에 학습 에너지 집중 |
| 형식 | TestCraft 완전 동일 (문제주도형/3종 패키지/FOR 문서/plan-task/초보자 README) | 학습 경험 통일 |
| 기존 CLAUDE.md 초안 | JPA 명시 부분 폐기, 신규 작성 예정 | 정체성 변경에 따른 갱신 |

### 도메인: 미니 커머스 (4주제를 하나로 묶는 무대)
**상품(Product) + 주문(Order)** —
- 주문 생성 시 **외부 결제 API** 호출 (연동/타임아웃/재시도)
- 상품 이미지 **업로드/다운로드** (파일)
- 상품 목록 **캐싱** (조회 빈도 높음 + 변경 시 무효화)
- 미결제 주문 **정리 스케줄러** + **Actuator** (운영)

### 커리큘럼 초안 (필수 ~9 Step + 심화, 각 1~1.5h)

| Step | 제목 | 동기(앞 단계의 한계) | 핵심 도구 |
|------|------|---------------------|----------|
| 1 | 스캐폴드 + 커머스 도메인 | — | MyBatis/MSSQL 모드, Product/Order CRUD (TestCraft 복습 겸 속성 구축) |
| 2 | 목록 API 설계 — 페이징·검색·정렬 | 단건 CRUD만으론 화면을 못 만든다 | 페이징 응답 규약(PageResponse), 동적 검색, 정렬 화이트리스트 |
| 3 | 외부 결제 API 연동 기초 | 우리 서버 밖의 세계와 통신 | RestTemplate, 외부 DTO 매핑, **MockRestServiceServer** 테스트 |
| 4 | 외부 장애에서 살아남기 | 외부가 느리면/죽으면 우리도 죽는다 | 타임아웃, 재시도(+백오프), 장애 격리 패턴, 실패 시나리오 테스트 |
| 5 | 파일 업로드/다운로드 | 이미지 없는 상품은 안 팔린다 | MultipartFile, 확장자/크기 검증, 저장 전략, MockMultipartFile 테스트 |
| 6 | 캐싱 — 같은 질문에 두 번 답하지 않기 | 상품 목록 조회가 DB를 두드려댄다 | @Cacheable/@CacheEvict, 무효화 전략, 캐시 동작 검증 테스트 |
| 7 | 스케줄링 — 미결제 주문 정리 | 사람이 매일 지울 수는 없다 | @Scheduled, 중복 실행 방지, 스케줄 로직의 직접 호출 테스트 |
| 8 | Actuator — 운영의 눈 | 떠 있는지조차 모르는 서버 | health/metrics/info, 커스텀 HealthIndicator, 운영 엔드포인트 보안 |
| 9 | **캡스톤: 배송 조회 연동** | 정답지 없이 — 신규 외부 연동을 스스로 | 외부 API 연동 전 과정 자율 설계 (요구사항+체크리스트+answer) |
| 10(심화) | WebClient 또는 Resilience4j | RestTemplate 너머 | 논블로킹 클라이언트 or 서킷브레이커 |

### 산출물 계획 (구현 회차에서)
- `spring-web-onboarding` 전면 재구축: CLAUDE.md 신규(MyBatis/실무API 규약), 초보자 README
- `docs/web/curriculum/00-WebFlow-Curriculum.md` + Step별 `FOR-WebFlow-StepNN.md`
- Step 단위 커밋 `✨ feat: [Web/Step N]`, 매 커밋 그린
- 기존 `.claude/skills`의 `spring-test-strategy`/`layered-impl`/`mybatis-mssql` 재사용,
  외부 연동 테스트 패턴은 진행하며 `spring-test-strategy`에 절 추가 검토

## Plan 2. 구현 (2026-06-11 착수)

Plan 1 커리큘럼대로 Step 단위 커밋(`✨ feat: [Web/Step N]`)으로 구현한다.
- 패키지: `com.webflow` (기존 org.example 스켈레톤 대체), DB명 `webdb`
- Step 1 커밋에 인프라 전환 + CLAUDE.md 신규 + 00 커리큘럼 포함
- 테이블명 orders (ORDER 예약어 회피 — bank_transaction과 같은 교훈)
- 핵심 패턴 신설: 원자적 재고 차감(조건부 UPDATE + affected) — Step 1
- 외부 연동 테스트는 @RestClientTest + MockRestServiceServer 표준 (Step 3~4, 9)
- 심화 Step 10(WebClient/Resilience4j)은 필수 완성 후 분량 보고 차기 판단
- 매 Step 모듈 테스트 그린 → 커밋, 마지막에 루트 전체 + push
