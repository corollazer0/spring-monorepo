# WebFlow 커리큘럼 — 실무 API 종합

> **대상**: TestCraft를 수료한 SpringBoot 개발자 (테스트 작성 기본기 전제)
> **목표**: 약 2주(하루 1~2시간) 안에 실무 API 서버의 잡기술 —
> **외부 연동 / 파일 / 캐싱 / 스케줄링 / 운영(Actuator)** — 을 테스트와 함께 갖춘다.
> **스택**: Java 1.8 · Spring Boot 2.7.17 · MyBatis · H2(MS-SQL 호환) · RestTemplate · **Security 없음**

---

## 1. 학습 철학: 문제를 먼저 겪고, 해결책을 배운다 (전 모듈 공통)

```
CRUD              "단건 조회만으론 화면을 못 만든다"
   ↓ 한계
페이징·검색·정렬     "결제는 우리 서버 밖의 세계다"
   ↓ 한계
외부 결제 연동       "외부가 느리면/죽으면 우리도 죽는다"
   ↓ 한계
타임아웃·재시도·격리  "이미지 없는 상품은 안 팔린다"
   ↓ 한계
파일 업·다운로드     "같은 목록 조회가 DB를 두드려댄다"
   ↓ 한계
캐싱               "미결제 주문을 사람이 매일 지울 수는 없다"
   ↓ 한계
스케줄링            "서버가 떠 있는지조차 모른다"
   ↓ 한계
Actuator           "이제 새 외부 연동을 스스로 붙일 수 있는가"
   ↓ 졸업
캡스톤: 배송 조회 연동
```

## 2. 사용 방법 (TestCraft와 동일한 루틴)

```bash
.\gradlew :spring-web-onboarding:test          # 먼저 그린 확인 (skipped = exercise)
.\gradlew :spring-web-onboarding:bootRun       # API 직접 호출해보기 (curl/포스트맨)
# 예: curl http://localhost:8080/api/products/1
```

Step당 1~1.5h: 문서 읽기 → example 따라잡기 → exercise 풀기 → answer 비교.

## 3. 커리큘럼 전체 지도

### 필수 (Step 1~9, 약 2주)

| Step | 제목 | 배우는 이유 (앞 단계의 한계) | 핵심 도구 | 소요 |
|------|------|---------------------------|----------|------|
| 1 | 스캐폴드 + 커머스 도메인 | (시작) TestCraft 복습 겸 무대 구축 | MyBatis/MSSQL, 원자적 재고 차감(affected), 예외 규약 | 1.5h |
| 2 | 목록 API — 페이징·검색·정렬 | 단건 CRUD만으론 화면을 못 만든다 | PageResponse 규약, 동적 검색, **정렬 화이트리스트**(인젝션 방어) | 1.5h |
| 3 | 외부 결제 연동 기초 | 결제는 우리 서버 밖의 세계 | RestTemplate(Builder!), 외부 DTO 분리, **@RestClientTest + MockRestServiceServer** | 1.5h |
| 4 | 외부 장애에서 살아남기 | 외부가 느리면/죽으면 우리도 죽는다 | 타임아웃, RetryTemplate+백오프, 장애 격리(주문 보존+503) | 1.5h |
| 5 | 파일 업로드/다운로드 | 이미지 없는 상품은 안 팔린다 | MultipartFile, 확장자/크기 검증, MockMultipartFile, @TempDir | 1.5h |
| 6 | 캐싱 — 같은 질문에 두 번 답하지 않기 | 목록 조회가 DB를 두드려댄다 | @Cacheable/@CacheEvict, 무효화 전략, **verify(times)로 캐시 적중 검증** | 1h |
| 7 | 스케줄링 — 미결제 주문 정리 | 사람이 매일 지울 수는 없다 | @Scheduled, 시각 주입 설계, 중복 실행 방지 | 1h |
| 8 | Actuator — 운영의 눈 | 떠 있는지조차 모르는 서버 | health/metrics, 커스텀 HealthIndicator, 노출 제한 | 1h |
| 9 | **캡스톤: 배송 조회 연동** | 정답지 없이 — 신규 외부 연동을 스스로 | 외부 연동 전 과정 자율 설계 (요구사항+체크리스트+answer) | 2h |

### 심화 (선택)

| Step | 제목 | 배우는 이유 (앞 단계의 한계) | 핵심 도구 | 소요 |
|------|------|---------------------------|----------|------|
| 10 | 서킷 브레이커 — 죽은 서버를 두드리지 않는 법 | 재시도는 "일시적" 장애 전제 — 죽은 외부엔 폭격이 된다 | Resilience4j 1.7.x(Java 8 호환), 3상 상태 기계, 상태 주입 테스트 | 1.5h |

WebClient는 미채택 (리액티브 스택 도입이 RestTemplate 표준과 충돌 — plan.md 참조).

## 4. 도메인: 미니 커머스

| 테이블 | 시드 | 쓰임 |
|--------|------|------|
| product | 12종 (KEYBOARD 5·품절 1 / MOUSE 4 / MONITOR 3) | 검색/페이징(2), 파일(5), 캐싱(6) |
| orders (ORDER는 예약어!) | 6건 (PAID 2 / PENDING 3·오래된 2 / CANCELLED 1) | 결제(3~4), 정리 스케줄(7), 배송(9) |

주문 상태 흐름: `PENDING_PAYMENT → PAID(결제 승인) / CANCELLED(거절·정리)`

## 5. 이 모듈의 시그니처 테스트 기법

| 기법 | Step | 한 줄 요약 |
|------|------|-----------|
| @RestClientTest + MockRestServiceServer | 3~4, 9 | 진짜 HTTP 없이 "외부 서버인 척" — 요청 검증 + 응답 조작 |
| 재시도 횟수 검증 (ExpectedCount.times) | 4 | "정말 3번 두드렸는가"를 가짜 서버가 센다 |
| MockMultipartFile + @TempDir | 5 | 파일 업로드를 메모리로, 저장을 임시 폴더로 |
| @MockBean DAO + verify(times(1)) | 6 | 캐시 적중 = "두 번 물어도 DAO는 한 번" |
| 스케줄 로직 직접 호출 + 시각 주입 | 7 | @Scheduled를 기다리지 않는다 — 로직을 분리해 부른다 |
| 차단기 상태 주입 (transitionTo + reset) | 10 | OPEN 10초를 기다리지 않는다 — 상태를 직접 전이시킨다 |

## 6. 완주 체크리스트

- [ ] Step 1~8의 모든 exercise를 통과시켰다
- [ ] Step 9 캡스톤에서 배송 연동 테스트를 스스로 설계·작성했다
- [ ] `.\gradlew :spring-web-onboarding:test` 그린
- [ ] "새 외부 API를 붙여라"는 업무를 받았을 때 타임아웃/재시도/격리/테스트를
      체크리스트처럼 떠올릴 수 있다

## 7. 관련 문서

| 문서 | 경로 |
|------|------|
| Step별 교육 가이드 | `docs/web/education/FOR-WebFlow-StepNN.md` |
| 모듈 규칙 | `spring-web-onboarding/CLAUDE.md` |
| 계획/태스크 | `docs/web/plan/plan.md`, `task.md` |
| 테스트 치트시트 (공통) | `docs/test/skills/spring-test-annotations.md` |
