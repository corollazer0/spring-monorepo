# TestCraft 계획서 (Living Document)

> ⚠️ **운영 규칙**: spring-test-onboarding 관련 작업은 **시작 전에 이 문서에 계획을 추가**하고,
> 완료 시 [task.md](./task.md)의 해당 태스크를 체크한다. (규칙 출처: 모듈 `CLAUDE.md`, `step-commit` 스킬)
> 계획이 작업 중 바뀌면 사유와 함께 이 문서를 갱신한다 — 이 문서는 의사결정의 단일 기록이다.

---

## Plan 1. TestCraft 모듈 신설 — Step 0~11 (완료: 2026-06-10)

### 배경/목표
테스트 코드를 한 번도 작성해본 적 없는 SpringBoot 초보자가 **일주일(하루 1~2시간, 자기주도)** 안에
전 레이어(Controller/Service/DAO/Filter/Interceptor/Security/Validation)의 테스트를
스스로 설계·작성하는 기본기를 갖추게 한다.

### 핵심 의사결정 (인터뷰로 확정)
| 결정 | 선택 | 이유 |
|------|------|------|
| 학습 방식 | example/exercise/answer 3종 패키지 | 읽고 이해 → 직접 작성 → 비교의 흐름. answer는 항상 빌드되어 썩지 않음 |
| 커리큘럼 구조 | 문제 주도형(각 Step은 앞 단계의 한계에서 출발) | 암기가 아닌 "도구 선택 기준"을 남기기 위해 |
| 시작점 | Spring 없는 순수 JUnit5/AssertJ → Mockito | 테스트의 본질부터 — 이후 모든 Step의 이해도 상승 |
| DB | H2 `MODE=MSSQLServer` + MS-SQL 방언 SQL | 실무 DB가 MS-SQL — 학습 전이 극대화 |
| Security | 세션+폼로그인 필수 / JWT는 심화 분리 | 일주일 분량 보호 |
| 정답 제공 | 같은 모듈 내 answer 패키지 | 빌드가 정답의 신선도를 보증 |
| 마무리 | 캡스톤(댓글) — 골격 없는 자율 설계 + 체크리스트 | "스스로 활용 가능"이라는 목표의 검증 장치 |

### 구축 중 실증된 리스크/해결
- H2 MSSQLServer 모드에서 `BIGINT IDENTITY`/`OFFSET FETCH`/`GETDATE()` 동작 확인 (한계는 Step 3 문서 비교표)
- **한글 인코딩 버그 실제 발생**(Windows cp949) → UTF-8 전면 고정, Step 3 Lessons Learned로 교재화
- CSRF 활성 상태의 TestRestTemplate 세션 로그인 흐름 동작 확인 (RestSessionHelper)
- RANDOM_PORT 롤백 불가 → `@Sql(AFTER_TEST_METHOD)` 정리 패턴 확립

### 결과
필수 Step 1~9 + 심화 Step 10~11, 테스트 143개 전부 그린, Step 단위 커밋 12개.

---

## Plan 2. README + CLAUDE.md 표준화 (완료: 2026-06-10)

- 루트 README.md 신설 (모듈 현황/빠른 시작/진입로)
- Claude Code 공식 문서 확인 결과 **`.claude.md`(dotfile)는 비표준으로 자동 로드되지 않음** →
  루트+3개 모듈 파일을 `CLAUDE.md`(표준)로 `git mv` + 참조 문자열 일괄 수정

---

## Plan 3. Step 12(View) + 에이전트/스킬 체계 + plan 운영 (완료: 2026-06-10)

### 배경/목표
① REST만 있어 "모든 테스트 통과 후 눈으로 확인할 웹앱"이 없다 → Thymeleaf 화면 + View 테스트 Step 추가.
② TestCraft와 **실제 현업 프로젝트 공용**의 에이전트(≤5)/스킬(≤10) 체계를 Claude Code 표준으로 구축.
③ 계획/태스크의 단일 기록(이 문서)과 유지 규칙 수립.

### 핵심 의사결정 (인터뷰로 확정)
| 결정 | 선택 | 이유 |
|------|------|------|
| View 기술 | Thymeleaf (+JSP 차이 문서화) | Boot 2.7 jar 공식 지원, **MockMvc가 실제 렌더링** → 템플릿 오류를 테스트로 검출. JSP는 jar 제약 + 렌더링 검증 불가 |
| Step 위치 | **Step 12로 추가** (필수, 권장 순서는 5~6 사이 명시) | 리넘버링은 기존 커밋/문서/패키지 참조를 대량 파괴 — 자산 보존 우선 |
| 화면 범위 | 5화면 + MockMvc까지 (HtmlUnit 제외) | 실행 가능한 웹앱 목적에 충분, 분량 보호 |
| 에이전트/스킬 위치 | 모노레포 루트 `.claude/` | 전 모듈 공용 + 타 프로젝트 폴더 복사 이식 |
| 리뷰 에이전트 권한 | **읽기 전용** | 리뷰와 변경의 경계 분리 — 의도치 않은 수정 사고 차단 |
| 스킬 규모 | 핵심 8종 + 교육 범용/특화 구별 = 9종 | 적게 시작해 쓰며 키운다. 교육 특화는 이식 시 제외 가능한 단위로 분리 |
| plan/task 운영 | CLAUDE.md 규칙화 (살아있는 문서) | 어떤 세션/에이전트가 와도 일관 유지 |

### 에이전트 5종 구성 근거 — "권한으로 역할을 가른다"
| 에이전트 | 권한 | 왜 이 5개인가 |
|---------|------|--------------|
| spring-developer | 전체 | 코드 변경의 **단일 창구** — 변경 권한을 한 곳에 모아 추적/통제 가능 |
| test-writer | 전체 | 구현과 검증의 분리 — 작성자 편향 없는 테스트, 변이 검증 의무화 |
| code-reviewer | **읽기 전용** | 리뷰가 곧 수정이면 검증 경계가 사라진다. 보고서 → developer 반영의 2단 구조 |
| code-analyst | 읽기+진단 실행 | 영향 분석/장애 추적은 실행(재현)이 필요하나 편집은 불필요 |
| edu-writer | 문서만 | 이 조직의 핵심 산출물(온보딩 문서)을 전담 — 실행 캡처 기반 정직한 문서 |
| (워크플로) | | analyst(이해) → developer(구현) ⇄ test-writer(검증) → reviewer(보고) → developer(반영) → edu-writer(기록) — 개발 수명주기 전체를 덮되 겹치지 않는 최소 집합 |

### 스킬 9종 구성 근거 — "검색으로 못 얻는 사내 표준만 스킬로"
범용 지식(Java 문법 등)은 모델이 이미 안다. 스킬은 **이 조직 고유의 결정**을 담는다:
1. `spring-test-strategy` — 계층 매트릭스/함정 진단표 (가장 사용 빈도 높은 표준)
2. `layered-impl` — 레이어 책임/예외 규약/Java 8 제약 (구현 일관성)
3. `mybatis-mssql` — MS-SQL 방언 + H2 한계표 (실무 DB 특화, 외부 자료 없음)
4. `security-patterns` — Security 5.7 표준 + 이중 체인 + EntryPoint 분기 (사고 비용 최대 + 인터넷 자료 구식)
5. `code-review-java8` — 심각도/체크리스트/보고서 형식 (리뷰 표준화)
6. `education-doc` — 범용 교육 템플릿 (현업 온보딩 문서에도 사용)
7. `testcraft-step-doc` — TestCraft 전용 (6과 구별 — 이식 시 제외 가능)
8. `step-commit` — 커밋 형식 + **plan/task 갱신 절차** (운영 규칙의 실행 장치)
9. `spring-troubleshoot` — 실증된 진단표 (이번 구축에서 실제 겪은 사례 축적)

뷰 전용/영향분석 전용 스킬은 만들지 않음 — 각각 layered-impl의 절, code-analyst 프롬프트에 내장 (스킬 수 절제).

### 결과
Step 12 완성(테스트 157개 그린 + 5화면 HTTP 스모크 검증), 에이전트/스킬 14파일, 본 문서 체계.

---

## Plan 4. 다음 후보 (미착수 — 착수 시 이 문서에 계획 구체화)

- [ ] 학습자 파일럿: 실제 대상자 1~2명 피드백 → exercise 난이도/문서 분량 조정
- [x] ~~spring-web-onboarding 커리큘럼 설계~~ → WebFlow로 완성 (docs/web/plan 참조)
- [ ] 현업 프로젝트에 `.claude/` 이식 + 적용 후기 → 스킬 보강
- [x] ~~(선택) Step 13 후보: 테스트 데이터 관리 심화 또는 RestDocs~~ → Plan 5로 구현 완료 (ArchUnit + REST Docs)

## Plan 5. 심화 확장 — Step 13 ArchUnit + Step 14 REST Docs (2026-06-12 ✅ 완료)

### 배경/목표
심화 트랙을 "인증(10) → 품질(11) → 구조(13) → 문서(14)"의 졸업 트랙으로 완성한다.
후보 검토 결과(Testcontainers/PIT/Awaitility 보류 — 사유: Docker 의존/빌드 부담/모듈 중복)
ArchUnit과 Spring REST Docs 2종을 채택.

### 설계 결정
| 결정 | 내용 | 이유 |
|------|------|------|
| Step 13 소재 | ArchUnit 1.2.1 (Java 8 호환) — 계층 의존/네이밍/모듈 격리 규칙 봉인 | 레포의 기존 규칙(layered-impl, 루트 헌법)이 그대로 검증 대상 — 새 프로덕션 코드 불필요 |
| ArchUnit 스타일 | archunit 코어 + 일반 @Test (ClassFileImporter, @BeforeAll 캐시) | @AnalyzeClasses 엔진은 @Disabled(3종 규약)와 궁합 불확실 — 일반 JUnit이 규약과 정합 |
| Step 14 소재 | spring-restdocs-mockmvc (Boot BOM 관리) — 스니펫 생성+필드 계약 검증까지 | asciidoctor HTML 변환은 범위 제외 (빌드 무게 최소화, 문서에 안내만) |
| Step 14 무대 | 회원가입 API(문서화 예제) + 게시글 단건 조회(연습) | Step 4 @WebMvcTest 자산 위에 그대로 — @Import(SecurityConfig)+csrf 기존 패턴 재사용 |

### 구현 결과 (2026-06-12)
- Step 13 `9fefcd1` / Step 14 `b0d4e4c` — 매 커밋 모듈 테스트 그린 (173건, +15)
- 심화 트랙 완성: 인증(10) → 품질(11) → 구조(13) → 문서(14) (12는 필수 View)
- ArchUnit 규칙 작성 전 실코드 사전 검증으로 전 규칙 첫 실행 그린 (도입 순서: 측정→봉인)
- spring-troubleshoot 진단표에 REST Docs 함정 2행 추가 (urlTemplate / 필드 불일치)

## Plan 6. 학습자 파일럿 1회차 시작 준비 (2026-06-12 착수)

### 배경/목표
PILOT-GUIDE(운영 방법론)는 완성 — 이제 "당장 시작할 때 손에 쥘 것"을 만든다:
학습자에게 그대로 전달하는 키트 + 학습자가 채우는 진행 로그 템플릿.

### 산출물
| 산출물 | 대상 | 내용 |
|--------|------|------|
| docs/pilot/LEARNER-KIT.md | 학습자 (전달용 1페이지) | 시작 순서, 학습 규칙 3 + AI 규칙, 포크 작업 방식, 보고 방법 |
| docs/pilot/PILOT-LOG-TEMPLATE.md | 학습자 (포크에 복사해 기입) | Step별 미니 설문 표, 막힘 기록(5분류), 완주 인터뷰 자가 기입 |
| PILOT-GUIDE 갱신 | 운영자 | 키트/템플릿 연결 + 시작 시퀀스 명문화 |

### 운영자에게 남는 일 (문서로 못 하는 것)
- 대상자 1~2명 선정 + 일정 확정, 레포 접근 권한(포크 안내)
- Day 0 같은 망 사전 점검 1회 (클론→테스트→bootRun 스모크: gradlew :spring-test-onboarding:bootRun → /posts 200 확인)
