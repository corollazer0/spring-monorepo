# 현업 프로젝트 `.claude/` 이식 가이드 (운영자용)

> 이 레포의 AI 협업 체계(에이전트 5종 + 스킬 9종)를 현업 프로젝트로 옮길 때의
> 준비·수정·도입 절차. 핵심 원칙: **폴더 복사는 이식의 절반이다** —
> 에이전트들은 "루트/모듈 CLAUDE.md를 읽고 따른다"를 전제하므로,
> 현업 프로젝트의 헌법(CLAUDE.md) 작성까지가 한 세트다.

---

## 1. 이식 대상 분류 — 그대로 / 수정 / 제외

### 에이전트 (.claude/agents/) — 5종 전부 "그대로" 이식 가능

전부 모듈 중립적으로 작성되어 있다 (특정 경로 하드코딩 없음, "CLAUDE.md를 읽는다" 방식).

| 에이전트 | 비고 |
|---------|------|
| `spring-developer` `test-writer` `code-reviewer` `code-analyst` | 무수정 이식 |
| `edu-writer` | 무수정 이식 가능 — 단, testcraft-step-doc 언급 줄은 스킬 제외 시 함께 삭제 |

### 스킬 (.claude/skills/) — 분류표

| 스킬 | 분류 | 이식 시 할 일 |
|------|------|--------------|
| `spring-troubleshoot` | ✅ 그대로 | 진단표는 범용 — 현업에서 발견한 함정을 계속 추가 (자산화 규칙 유지) |
| `code-review-java8` | ✅ 그대로* | *현업 Java 버전 확인 — 17+면 금지 문법 절 재작성 + 스킬명 변경 |
| `education-doc` | ✅ 그대로 | 범용 템플릿 |
| `security-patterns` | 🔧 수정 | "이 레포의 모범" 각주를 현업 코드 위치로 교체 (또는 삭제) |
| `layered-impl` | 🔧 수정 | 모범 구현 각주 교체 + Java 8 제약 절을 현업 버전에 맞게 |
| `mybatis-mssql` | 🔧 수정 | 모범 각주 교체. H2 호환 모드 절은 현업 테스트 DB 전략에 따라 유지/삭제 |
| `spring-test-strategy` | 🔧 수정 | 실전 예제 각주(3모듈 지도)를 이 레포 링크 또는 현업 예제로 교체 |
| `step-commit` | 🔧 대폭 수정 | §1 학습 포인트 섹션(학습 레포 전용)·§3 plan 경로 → 현업 커밋 컨벤션/운영 방식으로 |
| `testcraft-step-doc` | ❌ 제외 | TestCraft 전용 (루트 CLAUDE.md에 명시된 제외 대상) |

### 복사 금지

- `settings.local.json` — 개인 로컬 권한 설정 (이식 대상 아님, .gitignore 권장)

## 2. 사전 점검 체크리스트 (이식 전, 현업 프로젝트에서 확인)

- [ ] **Java/Boot 버전** — 스킬 곳곳의 "Java 8 전용/버전 고정" 규칙의 운명을 결정 (1순위 확인)
- [ ] **기술 스택 일치도** — MyBatis? MS-SQL? Spring Security? 안 쓰는 스택의 스킬은 빼는 게 낫다
  (안 맞는 표준이 로드되면 AI가 잘못된 규칙을 적용한다 — 없느니만 못함)
- [ ] **기존 커밋 컨벤션** — 이모지/타입 형식이 현업 규칙과 충돌하면 step-commit을 현업 쪽에 맞춘다
  (이식의 목적은 체계이지 형식이 아니다)
- [ ] **테스트 문화 현황** — 테스트가 거의 없는 레포라면 spring-test-strategy의 "매 커밋 그린"
  규칙이 첫날부터 불가능 — 적용 범위를 "신규 코드부터"로 명시
- [ ] **AI 사용 정책** — 사내 코드의 외부 LLM 전송 허용 여부, Claude Code 사용 승인 (선결 조건)
- [ ] **plan/task 운영 도입 여부** — 이 레포의 Living Document 방식을 가져갈지 결정.
  도입하면 step-commit §3 경로를 현업 구조로, 안 하면 §3 삭제

## 3. 이식 절차 (반나절 작업)

```
① .claude/ 복사 (testcraft-step-doc, settings.local.json 제외)
② 1장 분류표대로 스킬 수정 — "이 레포의 모범" 각주 전부 처리 (잔존 시 AI가 없는 경로를 참조)
③ 현업 CLAUDE.md 작성 ← 이식의 본체 (아래 4장)
④ 시범 작업 1건으로 검증 (아래 5장)
⑤ 후기를 이 레포 docs/test/plan/plan.md에 기록 → 스킬 보강 (피드백 루프)
```

## 4. 현업 CLAUDE.md 작성 — 이식의 본체

에이전트/스킬은 "무엇을 어떻게"의 표준이고, CLAUDE.md는 "이 프로젝트는 무엇인가"다.
이 레포의 루트+모듈 CLAUDE.md를 골격 삼아 다음 절은 반드시 채운다:

1. **기술 스택 고정표** — 버전 + 업그레이드 금지 명시 (AI의 "최신 문법" 혼입 차단이 최대 효용.
   이 레포의 사례: Batch 5.x 스타일, jakarta.*, MyBatis 3.x 차단)
2. **금지 문법/패턴** — Java 버전 제약, deprecated API (예: WebSecurityConfigurerAdapter)
3. **패키지 구조와 계층 규칙** — layered-impl이 참조할 실제 구조
4. **테스트 규칙** — 네이밍/단언 라이브러리/슬라이스 설정 (현업 현실에 맞는 수준으로)
5. **커밋/브랜치 규칙** — step-commit과 일치시킬 것
6. (모노레포라면) **모듈 격리 규칙** — 그리고 가능하면 ArchUnit로 기계화
   (이 레포 advanced/step13이 본보기 — 규칙은 빌드가 지키게)

## 5. 검증 — 시범 작업 1건

작은 실제 작업(버그 수정 1건 또는 소규모 기능 1개)을 에이전트 체계로 수행해본다:

```
code-analyst로 원인/영향 분석 → spring-developer로 수정 →
test-writer로 테스트 보강(변이 검증 포함) → code-reviewer로 리뷰 → 리뷰 반영
```

**관찰 항목**: ① AI가 현업 CLAUDE.md 규칙을 실제로 따르는가 (금지 문법 혼입 여부)
② 스킬의 잔존 레포 참조로 헛짚는 곳이 있는가 ③ 리뷰 보고서가 현업 코드 맥락에 유효한가
④ 사람 리뷰어 관점에서 산출물이 머지 가능 수준인가

## 6. 운영 규칙 (이식 후)

- **함정 자산화 유지**: 현업에서 발견한 함정은 현업 쪽 spring-troubleshoot 진단표에 추가 —
  범용성 있는 것은 이 레포에도 역이식 (양방향 동기화)
- **스킬 추가 기준**: 같은 지적/설명을 두 번 반복하면 스킬 후보다 (10개 이하 유지 원칙)
- **버전 분기 주의**: 이식 후 두 레포의 스킬은 독립 진화한다 — 분기가 커지면
  "공통 코어 + 프로젝트 오버레이" 구조 검토 (단, 1~2개 프로젝트까지는 복사가 더 싸다)

## 7. 리스크와 대비

| 리스크 | 대비 |
|--------|------|
| 잔존 레포 참조("이 레포의 모범" 각주)로 AI가 없는 파일 참조 | 이식 절차 ②에서 전수 처리 — `Grep "이 레포"` 로 확인 |
| 현업 Java 버전 불일치로 잘못된 제약 적용 | 사전 점검 1번 — code-review-java8/layered-impl의 버전 절 우선 수정 |
| 기존 팀 컨벤션과 충돌 (커밋 형식 등) | 표준은 현업 기존 규칙이 우선 — 스킬을 현업에 맞춘다 (반대 방향 강요 금지) |
| 테스트 없는 레거시에 "매 커밋 그린" 적용 불가 | 적용 범위 선언: "신규/수정 코드부터" — CLAUDE.md에 명시 |
| 스킬 과다로 컨텍스트 낭비 | 안 쓰는 스택 스킬은 과감히 제외 (2장) — 빈 자리는 현업 도메인 스킬로 |

---

## 8. 특화: 공통 라이브러리(NCDPcommon형) 이식 — 첫 대상으로 최적

공통 jar는 도메인이 얇고 계약이 명확해서 **테스트 구축 ROI가 가장 높은 이식 대상**이다.
단, 실행 앱이 아니라는 점에서 테스트 전략이 다르다.

### 8-1. 라이브러리의 테스트 도구 사다리 (앱과 다른 점)

| 검증 대상 | 도구 | 비고 |
|----------|------|------|
| 유틸/정책/변환 로직 (대부분!) | 순수 JUnit5 + AssertJ | 라이브러리 테스트의 본진 — Spring 불필요 |
| 자동구성/조건부 빈 (`@ConditionalOn...`) | **`ApplicationContextRunner`** | Boot 공식 라이브러리 테스트 도구 — "이 프로퍼티면 이 빈이 뜬다/안 뜬다"를 컨텍스트 없이 검증 |
| 특정 빈 조합의 동작 | `@SpringBootTest(classes = 작은TestConfig)` | 앱 클래스가 없으니 classes 명시 필수 |
| 소비 프로젝트와의 최종 통합 | 소비 레포에서 [composite build](./COMPOSITE-BUILD-GUIDE.md)로 테스트 실행 | 기동 확인은 여기 소수만 |

`ApplicationContextRunner` 맛보기 (스타터/자동구성을 제공하는 라이브러리라면 필수 패턴):

```java
new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(NcdpAutoConfiguration.class))
        .withPropertyValues("ncdp.feature.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(NcdpFeature.class));
```

### 8-2. 공개 API의 계약 봉인 — 라이브러리의 ArchUnit/문서

공통 모듈의 무서움은 "내 수정이 어느 소비 프로젝트를 깨뜨릴지 모른다"는 것:

- **ArchUnit**: `..internal..` 패키지를 공개 API가 노출하지 않는다 / 공개 타입 네이밍 규약 —
  "무엇이 공개 계약인지"를 빌드가 선언하게 한다 (Test 심화 Step 13의 직접 적용)
- **japicmp(Gradle 플러그인)**: 직전 릴리스 대비 **바이너리 호환성** 검사 —
  시그니처 깨짐이 publish 전에 빌드 실패로 드러난다
- 깨뜨려야 할 땐: @Deprecated 한 버전 유예 + 변경 로그 — 정책도 CLAUDE.md에 명문화

### 8-3. CI downstream 검증 잡 — "소비자가 안 깨진다"의 기계화

공통 레포 파이프라인에 잡 하나 추가: 대표 소비 레포 1~2개를 클론해
`gradlew test --include-build ../NCDPcommon` — publish 전에 소비자 테스트가 도는 구조.
(소비 레포가 테스트를 갖춰갈수록 이 잡의 보증력이 커진다 — 선순환)

### 8-4. 이식 순서 (라이브러리판)

```
① composite build로 개발 루프부터 단축 (COMPOSITE-BUILD-GUIDE — 코드 변경 0)
② .claude 이식 + NCDPcommon CLAUDE.md 작성 (4장 골격 + 공개 API 정책 절 추가)
③ 테스트 구축: 핵심 유틸·자동구성부터 (8-1 사다리) — "수정엔 테스트 동반" 규칙 선언
④ 계약 봉인: ArchUnit 공개 API 규칙 → japicmp (8-2)
⑤ CI downstream 잡 (8-3) — 이때부터 "한 줄 수정"의 검증은 분 단위가 된다
```

---

> **이식의 한 줄 원칙**: 형식이 아니라 체계를 옮겨라 — 핵심은 9개 파일이 아니라
> "헌법(CLAUDE.md)이 표준(스킬)을 거느리고, 함정이 자산(진단표)이 되는" 순환 구조다.
