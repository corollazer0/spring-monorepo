# Spring Onboarding Monorepo

> ⚠️ 이 파일은 AI 어시스턴트의 **"헌법"**입니다.
> 모든 코드 생성, 리뷰, 수정 시 이 규칙을 **절대적으로** 준수해야 합니다.

---

## 🏗️ 프로젝트 구조 (Monorepo)

이 프로젝트는 세 개의 **독립적인 학습 모듈**로 구성됩니다.

```
spring-monorepo/                      # 루트
├── CLAUDE.md                        # 이 파일 (공통 규칙)
├── docs/{test,batch,web}/           # 모듈별 커리큘럼·교육·plan 문서
├── spring-test-onboarding/           # [Test] TestCraft — 테스트 기본기
│   └── CLAUDE.md
├── spring-batch-onboarding/          # [Batch] BatchFlow — 대용량 배치 처리
│   └── CLAUDE.md
└── spring-web-onboarding/            # [Web] WebFlow — 실무 API 종합
    └── CLAUDE.md
```

| 모듈 | 목적 (기준 패키지) | 상세 규칙 |
|------|------|----------|
| `spring-test-onboarding` | TestCraft — 테스트 기본기 (`com.testonboarding`) | `./spring-test-onboarding/CLAUDE.md` |
| `spring-batch-onboarding` | BatchFlow — 배치 처리 (`com.batchflow`) | `./spring-batch-onboarding/CLAUDE.md` |
| `spring-web-onboarding` | WebFlow — 실무 API 종합 (`com.webflow`) | `./spring-web-onboarding/CLAUDE.md` |

---

## 🚨 CRITICAL RULES - 상호 참조 금지 (MUST)

**AI는 작업 시 다음 규칙을 엄격히 준수해야 합니다:**

### 1. Isolation (격리)

```java
// ❌ 절대 금지 - Web에서 Batch 참조
// spring-web-onboarding(com.webflow) 모듈에서:
import com.batchflow.job.dormant.DormantMemberJobConfig;  // NEVER!

// ❌ 절대 금지 - Batch에서 Web/Test 참조
// spring-batch-onboarding(com.batchflow) 모듈에서:
import com.webflow.product.controller.ProductController;  // NEVER!
import com.testonboarding.member.service.MemberService;   // NEVER!
```

- 어느 모듈에서 작업하든 다른 두 모듈(`com.testonboarding` / `com.batchflow` / `com.webflow`)의
  코드를 **절대 import/참조 금지**
- 이 규칙은 ArchUnit 테스트로도 봉인되어 있다 (Test 심화 Step 13 — 어기면 빌드 실패)

### 2. Dependency Check (의존성 확인)

- `build.gradle`에 명시되지 않은 다른 모듈의 클래스는 **사용 불가**
- 현재 세 모듈은 `implementation project(...)` 로 **연결되어 있지 않음**

### 3. Context Switching (컨텍스트 전환)

| 사용자 요청 | 작업 디렉토리 | 참조할 규칙 |
|-------------|--------------|-------------|
| "테스트 작성해줘", "[Test] ..." | `./spring-test-onboarding` | `spring-test-onboarding/CLAUDE.md` |
| "배치 Job 만들어줘", "[Batch] ..." | `./spring-batch-onboarding` | `spring-batch-onboarding/CLAUDE.md` |
| "API 구현해줘", "[Web] ..." | `./spring-web-onboarding` | `spring-web-onboarding/CLAUDE.md` |

---

## 🔧 공통 기술 스택 (변경 불가)

```
Java:           1.8 (OpenJDK 또는 Oracle JDK)
Spring Boot:    2.7.17
Lombok:         1.18.x
JUnit:          5.x
Gradle:         8.x
```

---

## 📜 공통 Java 1.8 문법 규칙

### ✅ 허용

```java
// Lambda
List<Member> active = members.stream()
    .filter(m -> m.getStatus() == Status.ACTIVE)
    .collect(Collectors.toList());

// Optional
Optional<Member> member = repository.findById(id);
member.ifPresent(m -> process(m));
```

### ❌ 금지

| 문법 | Java 버전 | 예시 |
|------|-----------|------|
| `var` | 10+ | `var list = new ArrayList<>();` |
| `record` | 14+ | `record MemberDto(Long id) {}` |
| Text Block | 15+ | `"""multi line"""` |
| `Stream.toList()` | 16+ | `list.stream().toList()` |
| switch expression | 12+ | `switch(x) { case A -> "a"; }` |
| `jakarta.*` | Spring Boot 3.x | `import jakarta.persistence.*` |

```java
// ✅ javax.* 패키지 사용 (Spring Boot 2.x)
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
```

---

## 🏷️ 공통 네이밍 규칙

### 클래스명

| 유형 | 패턴 | 예시 |
|------|------|------|
| Entity | `{도메인}` | `Member`, `Transaction` |
| Repository | `{도메인}Repository` | `MemberRepository` |
| DTO | `{도메인}{용도}Dto` | `MemberResponseDto` |
| 테스트 | `{대상클래스}Test` | `MemberServiceTest` |

### 테스트 메서드명 (한글 허용)

```java
// 패턴: {대상}_{시나리오}_{예상결과}
void findMember_존재하는회원ID_회원반환()
void createMember_중복이메일_예외발생()
```

---

## 📝 공통 로깅 규칙

```java
// 정보 로그: >>>>> 접두사
log.info(">>>>> [{}] 처리 시작", serviceName);
log.info(">>>>> [{}] {} 건 처리 완료", serviceName, count);

// 경고 로그
log.warn(">>>>> [WARN] 예상과 다른 상황: {}", description);

// 에러 로그: 예외 객체 포함 필수
log.error(">>>>> [ERROR] 처리 실패: {}", e.getMessage(), e);

// 디버그 로그
log.debug(">>>>> [DEBUG] 상세 정보: {}", data);
```

---

## 📊 공통 커밋 메시지 규칙

### 형식

```
[type]: [모듈/Step] 간단한 설명

상세 설명 (선택)

학습 포인트:
- 포인트 1
- 포인트 2
```

### 타입

| 타입 | 이모지 | 용도 |
|------|--------|------|
| feat | ✨ | 새 기능 |
| fix | 🐛 | 버그 수정 |
| test | ✅ | 테스트 |
| refactor | ♻️ | 리팩토링 |
| docs | 📝 | 문서화 |
| config | ⚙️ | 설정 변경 |

### 예시

```
✨ feat: [Batch/Step 9] Chunk 기반 처리 모델 구현

ItemReader/ItemProcessor/ItemWriter 분리 구조 적용

학습 포인트:
- Chunk 모델의 트랜잭션 범위 이해
```

```
✨ feat: [Web] 회원 조회 API 구현

GET /api/members/{id} 엔드포인트 추가
```

---

## 📚 모듈별 상세 규칙

각 모듈의 상세한 규칙은 해당 모듈의 `CLAUDE.md` 파일을 참조하세요.

| 모듈 | 파일 위치 | 주요 내용 |
|------|----------|----------|
| Test | `spring-test-onboarding/CLAUDE.md` | TestCraft 학습 모듈, example/exercise/answer 규약, plan/task 운영 |
| Batch | `spring-batch-onboarding/CLAUDE.md` | Spring Batch 4.x 규칙(5.x 금지), Job/Step 규칙, 배치 테스트 패턴 |
| Web | `spring-web-onboarding/CLAUDE.md` | 외부 연동/파일/캐싱/스케줄링 규칙, Security 없음·JPA 금지 |

---

## 🤖 에이전트 & 스킬 (AI 협업 체계)

이 레포에는 공용 서브에이전트 5종(`.claude/agents/`)과 스킬 9종(`.claude/skills/`)이 구성되어 있다.
타 프로젝트에 이식할 때는 `.claude/` 폴더를 복사한다 (TestCraft 특화인 `testcraft-step-doc` 스킬은 제외 가능).

### 에이전트 — 역할별 위임 (각자 권한이 다르다)

| 에이전트 | 역할 | 권한 |
|---------|------|------|
| `spring-developer` | 기능 구현/수정, 리뷰 반영 | 전체 (코드 변경의 단일 창구) |
| `test-writer` | 테스트 전략 수립 + 작성 + 변이 검증 | 전체 |
| `code-reviewer` | 심각도별 리뷰 보고서 | **읽기 전용** (수정은 developer에게) |
| `code-analyst` | 영향 분석, 장애 원인 추적, 구조 파악 | 읽기 + 진단 실행 |
| `edu-writer` | 교육/온보딩/기술 문서 | 문서 파일만 |

### 스킬 — 조직 표준의 지식 단위

구현: `layered-impl` `mybatis-mssql` `security-patterns` /
테스트: `spring-test-strategy` / 리뷰: `code-review-java8` /
문서: `education-doc`(범용) `testcraft-step-doc`(TestCraft 전용) /
운영: `step-commit`(커밋+plan/task 갱신) `spring-troubleshoot`(진단표)

규칙: 에이전트와 직접 작업 모두, 해당 영역의 스킬 표준을 우선 적용한다.
새로 발견한 함정은 `spring-troubleshoot` 진단표에 추가해 자산화한다.

---

## ❓ AI 요청 가이드

### 모듈 명시 요청 (권장)

```
"[Batch] Step 15의 휴면회원 전환 Job을 구현해줘"
"[Web] 회원 조회 API를 구현해줘"
```

### 모듈 미명시 시 AI 행동

1. 요청 내용에서 모듈 추론 (Job/Batch → Batch / API·외부연동·파일 → Web / 테스트 기법·Security → Test)
2. 추론 불가 시 → 사용자에게 모듈 확인 질문

### 버그 수정 요청

```
"[Batch] 이 에러 해결해줘
[에러 메시지 전체 복사]

현재 코드:
[관련 코드 복사]"
```

---

## 🔗 관련 문서

모듈별 문서는 전부 루트 `docs/{test,batch,web}/` 아래에 있다 (curriculum / education / plan).

| 문서 | 경로 |
|------|------|
| Test 커리큘럼 | `docs/test/curriculum/00-TestCraft-Curriculum.md` |
| Batch 커리큘럼 (학습 기준) | `docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md` |
| Web 커리큘럼 | `docs/web/curriculum/00-WebFlow-Curriculum.md` |
| 모듈별 계획/태스크 (Living) | `docs/{test,batch,web}/plan/{plan.md,task.md}` |
| Batch DB 스키마 (대량 데이터 참조) | `docs/batch/sql/Database-Schema-And-Data.md` |
