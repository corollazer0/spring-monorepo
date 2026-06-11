# Spring Onboarding Monorepo

Spring Boot 기반 사내 온보딩 학습 프로젝트 모음입니다. 각 모듈은 서로 독립적이며,
**문제 주도형(problem-driven) 커리큘럼**과 **Step 단위 커밋**으로 구성되어
git 히스토리 자체가 학습 경로가 되도록 설계되었습니다.

## 모듈 구성

| 모듈 | 주제 | 상태 | 시작점 |
|------|------|------|--------|
| [`spring-test-onboarding`](./spring-test-onboarding) | **TestCraft** — JUnit5 / SpringBootTest 테스트 기본기 (일주일 과정) | ✅ 전 과정 완성 | [커리큘럼](./docs/test/curriculum/00-TestCraft-Curriculum.md) · [실행 가이드](./spring-test-onboarding/README.md) |
| [`spring-batch-onboarding`](./spring-batch-onboarding) | **BatchFlow** — Spring Batch 배치 기본기 (필수 13 + 심화 2, 약 2주) | ✅ 필수 트랙 완성 | [커리큘럼](./docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md) · [실행 가이드](./spring-batch-onboarding/README.md) |
| [`spring-web-onboarding`](./spring-web-onboarding) | **WebFlow** — 실무 API 종합 (외부연동/파일/캐싱/스케줄링/Actuator) | 📝 설계 완료 | [마스터플랜](./docs/web/plan/plan.md) |

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
- **구조**: 필수 Step 1~9 (순수 단위 → Mockito → @MybatisTest → @WebMvcTest → Validation/예외 → Security → Filter/Interceptor → E2E → 캡스톤) + 심화 Step 10~11 (JWT, 테스트 품질)
- **학습 방식**: Step마다 `example`(완성 모범) / `exercise`(@Disabled TODO 골격) / `answer`(모범답안) 3종 패키지

| 진입로 | 경로 |
|--------|------|
| 커리큘럼 전체 지도 | `docs/test/curriculum/00-TestCraft-Curriculum.md` |
| Step별 학습 가이드 | `docs/test/education/FOR-Test-Step01~11.md` |
| 캡스톤 요구사항/체크리스트 | `docs/test/education/FOR-Test-Step09-Requirements.md` |
| 어노테이션 치트시트 + 미스터리 진단표 | `docs/test/skills/spring-test-annotations.md` |

## ⚙️ BatchFlow (spring-batch-onboarding)

Spring Batch의 Job/Step/Tasklet부터 Chunk, 오류 제어, 성능 최적화까지 다루는 과정입니다.
`docs/batch/` 아래의 커리큘럼과 education 문서를 따라 진행합니다.

---

## 저장소 규칙

- **모듈 간 상호 참조 금지** — 각 모듈은 완전히 독립 (루트 `CLAUDE.md` 참고)
- **Java 1.8 문법만** 사용 (`var`/`record`/`jakarta.*` 금지, `javax.*` 사용)
- 커밋 형식: `✨ feat: [모듈/Step N] 제목` + 본문에 `학습 포인트` 섹션
- AI 어시스턴트 작업 규칙은 루트 및 각 모듈의 `CLAUDE.md`에 정의
