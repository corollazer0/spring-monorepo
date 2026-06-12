# TestCraft 태스크 보드 (Living Document)

> 운영 규칙: 작업 시작 전 [plan.md](./plan.md)에 계획 추가 → 태스크를 여기에 `[ ]`로 등록 →
> 완료 커밋 시 `[x]` 체크 + 커밋 해시 병기. (규칙 출처: 모듈 `CLAUDE.md`, `step-commit` 스킬)

---

## Plan 1: TestCraft 모듈 신설 (Step 0~11) — ✅ 완료

- [x] Step 0: 모듈 스캐폴드 (Gradle 등록, H2 MSSQLServer, 모듈 헌법, 커리큘럼 문서) — `416f37f`
- [x] Step 1: 순수 JUnit5 + AssertJ (PasswordPolicyValidator) — `d903e78`
- [x] Step 2: Service 비즈니스 로직 테스트 / Mockito (도메인/예외/Service 신설) — `fba12cb`
- [x] Step 3: DAO 테스트 @MybatisTest + MS-SQL 방언 (+UTF-8 인코딩 버그 해결) — `87d5f9e`
- [x] Step 4: Controller 테스트 @WebMvcTest (+SecurityFilterChain 방식 SecurityConfig) — `8a6f4c0`
- [x] Step 5: Bean Validation + GlobalExceptionHandler (+Step 4 cliffhanger 보존 처리) — `bd17bb7`
- [x] Step 6: Security 인증/인가 테스트 (401 vs 403, csrf, 소유권) — `5c00c95`
- [x] Step 7: Filter & Interceptor (서블릿 Mock 3총사, MDC 정리) — `eb20762`
- [x] Step 8: 통합/E2E (RANDOM_PORT, RestSessionHelper, @Sql 정리) — `2bdc3a0`
- [x] Step 9: 캡스톤 — 댓글 기능 (요구사항+체크리스트+answer 4계층) — `c117178`
- [x] Step 10(심화): JWT 인증 필터 (jjwt 0.11.5, 이중 체인) — `868279f`
- [x] Step 11(심화): 테스트 품질 (@WithMockMember, Fixture, 치트시트) — `c227e5c`

## Plan 2: README + CLAUDE.md 표준화 — ✅ 완료

- [x] 루트 README.md 신설 + `.claude.md` → `CLAUDE.md` 표준 전환(4파일 git mv + 참조 21건 수정) — `3359104`

## Plan 3: Step 12(View) + 에이전트/스킬 + plan 운영 — ✅ 완료

- [x] Step 12: Thymeleaf 화면 5종 + View 테스트 + EntryPoint 분기 + bootRun 스모크 검증 — `31c7546`
- [x] 에이전트 5종(.claude/agents) + 스킬 9종(.claude/skills) + 루트 CLAUDE.md 안내 — `aee232f`
- [x] plan.md / task.md 신설 + 모듈 CLAUDE.md에 유지 규칙 추가 — `4435f5f`

## 소규모 개선 (수시)

- [x] 완전 초보자용 실행 가이드(모듈 README.md) 신설 + 루트 README 링크 — `2e97d76`

## Plan 5: 심화 확장 (Step 13~14) — 🔄 진행 중

- [x] Step 13(심화): ArchUnit — 아키텍처를 테스트로 봉인 (계층 의존/네이밍/모듈 격리) — `9fefcd1`
- [x] Step 14(심화): Spring REST Docs — 테스트가 문서를 만든다 — (이번 커밋)

## Plan 4: 다음 후보 — 📝 미착수

- [ ] 학습자 파일럿 운영 및 피드백 반영
- [x] spring-web-onboarding 커리큘럼 설계 — WebFlow로 완성 (docs/web/plan 참조)
- [ ] 현업 프로젝트 `.claude/` 이식 + 스킬 보강
