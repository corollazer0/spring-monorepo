# BatchFlow 계획서 (Living Document)

> ⚠️ **운영 규칙**: spring-batch-onboarding 관련 작업은 **시작 전에 이 문서에 계획을 추가**하고,
> 완료 시 [task.md](./task.md)의 해당 태스크를 체크한다. (규칙: 모듈 `CLAUDE.md`, `step-commit` 스킬)

---

## Plan 1. 필수 트랙 재편 및 완성 — Step 1~15 (완료: 2026-06-11)

### 배경/목표
기존 자산: 50-Step 커리큘럼 문서(3,462줄) 완성, 구현은 Step 2까지(~4%).
이를 **TestCraft 수준 이상의 교육 모듈**로 완성한다 — 전부 구현 대신
문제주도형 필수 트랙(13 Step + 심화 2)으로 재편.

### 핵심 의사결정 (인터뷰로 확정)
| 결정 | 선택 | 이유 |
|------|------|------|
| 범위 | 필수 13 + 심화 2로 압축, 50-Step 문서는 심화 참조 보존(배너+매핑표) | 전부 구현은 작업·학습 분량 모두 과대. 핵심(Phase 1~3 + 정산)을 압축 |
| DB | H2 인메모리 **MODE=MSSQLServer** + JDBC (파일DB/MySQL모드/JPA 제거) | 실무(MS-SQL) 정합 + TestCraft 일관 + 테스트 격리(파일DB 누적 문제) |
| 형식 | TestCraft 완전 동일 (3종 패키지/FOR 문서/plan-task/초보자 README) | 세 모듈의 학습 경험 통일 |
| 버전 | Spring Batch **4.3.x Factory 패턴 고정** (5.x 금지) | Boot 2.7 내장 — 기존 CLAUDE.md 규칙 유지 |
| 도메인 | 기존 스키마 압축: member 50 / bank_transaction 15 / settlement | 학습용 소규모 고정 시나리오 (TRANSACTION 예약어 → 개명) |

### 커리큘럼 (구현 완료)
필수: 1 인프라(메타장부) → 2 Hello → 3 Parameters/Instance → 4 Flow/Decider →
5 ExecutionContext → 6 Chunk+스키마 → 7 Cursor → 8 Paging(+리더 단독 테스트) →
9 Processor(순수 단위) → 10 휴면전환 통합(진짜 커밋/원상복구/자연 멱등) →
11 Skip/Retry/Listener → 12 재시작(saveState 함정) → 13 캡스톤 정산.
심화: 14 멀티스레드/병렬 Flow, 15 Partitioning.

### 구축 중 실증/확립된 것
- H2 MSSQLServer 모드에서 Batch 메타스키마 자동생성 정상 (Step 1 실검증)
- chunk 카운트 등식(READ=FILTER+WRITE+SKIP)과 COMMIT/ROLLBACK_COUNT 수치 전부 실검증
- **상태 전이 쿼리 + 위치 저장 = 누락** 함정 → saveState(false) 패턴 확립 (Step 12)
- 변경(커밋) 테스트의 Before/AfterEach 원상복구 패턴 — 실행 순서 의존 제거 (Step 14에서 보강)
- 교보재 규약: 정적 스위치/수집함(Sabotage, SEEN_THREADS)은 운영 금지 + 정리 의무 명시

## Plan 2. 다음 후보 (미착수)

- [ ] 학습자 파일럿 및 피드백 반영
- [ ] (선택) Phase 4~6 심화 Step 추가: 비동기 처리, JobOperator/스케줄링, 대량 알림 프로젝트
- [ ] (선택) 대량 데이터 성능 실습: 기존 10만건 스키마 문서 기반 성능 측정 Step
