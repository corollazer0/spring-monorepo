# Spring Onboarding Monorepo

Spring Boot 기반 사내 온보딩 학습 프로젝트 모음입니다. 각 모듈은 서로 독립적이며,
**문제 주도형(problem-driven) 커리큘럼**과 **Step 단위 커밋**으로 구성되어
git 히스토리 자체가 학습 경로가 되도록 설계되었습니다.

## 모듈 구성

| 모듈 | 주제 | 상태 | 시작점 |
|------|------|------|--------|
| [`spring-test-onboarding`](./spring-test-onboarding) | **TestCraft** — JUnit5 / SpringBootTest 테스트 기본기 (일주일 과정) | ✅ 전 과정 완성 | [커리큘럼](./docs/test/curriculum/00-TestCraft-Curriculum.md) · [실행 가이드](./spring-test-onboarding/README.md) |
| [`spring-batch-onboarding`](./spring-batch-onboarding) | **BatchFlow** — Spring Batch 배치 기본기 (필수 13 + 심화 4, 약 2주) | ✅ 전 과정 완성 | [커리큘럼](./docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md) · [실행 가이드](./spring-batch-onboarding/README.md) |
| [`spring-web-onboarding`](./spring-web-onboarding) | **WebFlow** — 실무 API 종합 (외부연동/파일/캐싱/스케줄링/Actuator, 필수 9 + 심화 1) | ✅ 전 과정 완성 | [커리큘럼](./docs/web/curriculum/00-WebFlow-Curriculum.md) · [실행 가이드](./spring-web-onboarding/README.md) |

## 공통 기술 스택

```
Java 1.8 · Spring Boot 2.7.17 · Gradle 8.x · JUnit 5 · Lombok
```

모듈별 추가 스택은 각 모듈의 `CLAUDE.md`를 참고하세요.

## 빠른 시작

```bash
# 전체 빌드 + 테스트
.\gradlew test

# 모듈 하나만 (예: TestCraft)
.\gradlew :spring-test-onboarding:test

# TestCraft 웹앱 실행 (Thymeleaf 화면)
.\gradlew :spring-test-onboarding:bootRun
# → http://localhost:8080/posts  (로그인: writer1 / spring123!)
```

> 🔰 **처음이신가요?** JDK 설치 확인부터 H2 콘솔, IntelliJ 설정, FAQ까지 담은
> **완전 초보자용 실행 가이드** → [spring-test-onboarding/README.md](./spring-test-onboarding/README.md)

> 요구사항: JDK 8+ (JDK 11 권장). Gradle은 wrapper가 포함되어 있어 별도 설치 불필요.

---

## 🎓 TestCraft (spring-test-onboarding)

**대상**: 테스트 코드를 한 번도 작성해본 적 없는 SpringBoot 초보 개발자
**목표**: 일주일(하루 1~2시간, 자기주도) 안에 전 레이어의 테스트를 스스로 설계·작성하는 기본기

- **스택**: Spring Security(세션+JWT) · MyBatis(XML) · H2(**MODE=MSSQLServer** — 실무 MS-SQL 대응) · Mockito · AssertJ
- **구조**: 필수 Step 1~9, 12 (순수 단위 → Mockito → @MybatisTest → @WebMvcTest → Validation/예외 → Security → Filter/Interceptor → E2E → 캡스톤 → View) + 심화 Step 10~14 (JWT → 테스트 품질 → ArchUnit 구조 봉인 → REST Docs 문서화)
- **학습 방식**: Step마다 `example`(완성 모범) / `exercise`(@Disabled TODO 골격) / `answer`(모범답안) 3종 패키지

| 진입로 | 경로 |
|--------|------|
| 커리큘럼 전체 지도 | `docs/test/curriculum/00-TestCraft-Curriculum.md` |
| Step별 학습 가이드 | `docs/test/education/FOR-Test-Step01~14.md` |
| 캡스톤 요구사항/체크리스트 | `docs/test/education/FOR-Test-Step09-Requirements.md` |
| 어노테이션 치트시트 + 미스터리 진단표 | `docs/test/skills/spring-test-annotations.md` |

## ⚙️ BatchFlow (spring-batch-onboarding)

**대상**: TestCraft를 수료한 SpringBoot 개발자
**목표**: 약 2주 안에 Job 설계 → Chunk → 오류 제어 → 재시작까지, 실무 배치를 스스로 만들고 테스트하는 기본기

- **스택**: Spring Batch 4.3.x (5.x 금지) · JDBC(Cursor/Paging Reader) · H2(MODE=MSSQLServer) · @SpringBatchTest
- **구조**: 필수 Step 1~13 (인프라 → Hello Job → Parameters → Flow → Chunk → Reader/Processor/Writer → 휴면전환 실전 → Skip/Retry → 재시작 → 정산 캡스톤) + 심화 Step 14~17 (멀티스레드/병렬 → 파티셔닝 → 비동기 성능 → JobOperator 운영)

| 진입로 | 경로 |
|--------|------|
| 필수 트랙 커리큘럼 (학습 기준) | `docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md` |
| Step별 학습 가이드 | `docs/batch/education/FOR-BatchFlow-Step01~17.md` |
| 전체 50-Step (심화 참조) | `docs/batch/curriculum/00-BatchFlow-Curriculum.md` |
| 실행 가이드 (초보자용) | `spring-batch-onboarding/README.md` |

## 🌐 WebFlow (spring-web-onboarding)

**대상**: TestCraft를 수료한 SpringBoot 개발자
**목표**: 약 2주 안에 실무 API 서버의 잡기술 — 외부 연동/파일/캐싱/스케줄링/운영 — 을 테스트와 함께

- **스택**: MyBatis(XML) · H2(MODE=MSSQLServer) · RestTemplate(Builder) · spring-retry · Actuator · **Security 없음**
- **도메인**: 미니 커머스 (상품/주문 + 외부 결제·배송 연동)
- **구조**: 필수 Step 1~9 (도메인 → 목록 API → 외부 결제 → 장애 생존 → 파일 → 캐싱 → 스케줄링 → Actuator → 배송 조회 캡스톤) + 심화 Step 10 (Resilience4j 서킷 브레이커)
- **시그니처 기법**: @RestClientTest+MockRestServiceServer(진짜 HTTP 금지), ExpectedCount로 재시도 횟수 봉인, @TempDir 파일 격리, verify(times)로 캐시 적중 증명, 시각 주입 스케줄 테스트

| 진입로 | 경로 |
|--------|------|
| 커리큘럼 전체 지도 | `docs/web/curriculum/00-WebFlow-Curriculum.md` |
| Step별 학습 가이드 | `docs/web/education/FOR-WebFlow-Step01~10.md` |
| 캡스톤 요구사항/체크리스트 | `docs/web/education/FOR-WebFlow-Step09.md` |
| 실행 가이드 (초보자용) | `spring-web-onboarding/README.md` |

---

## 저장소 규칙

- **모듈 간 상호 참조 금지** — 각 모듈은 완전히 독립 (루트 `CLAUDE.md` 참고)
- **Java 1.8 문법만** 사용 (`var`/`record`/`jakarta.*` 금지, `javax.*` 사용)
- 커밋 형식: `✨ feat: [모듈/Step N] 제목` + 본문에 `학습 포인트` 섹션
- AI 어시스턴트 작업 규칙은 루트 및 각 모듈의 `CLAUDE.md`에 정의
