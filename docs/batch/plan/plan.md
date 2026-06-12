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
- [x] ~~(선택) Phase 4~6 심화 Step 추가: 비동기 처리, JobOperator~~ → Plan 3으로 구현 완료 (Step 16~17)
- [ ] (선택) 대량 알림 발송 제2 캡스톤 (50-Step 47~50)
- [ ] (선택) 대량 데이터 성능 실습: 기존 10만건 스키마 문서 기반 성능 측정 Step

## Plan 3. 성능/운영 심화 — Step 16~17 (2026-06-12 착수 → 같은 날 ✅ 완료)

### 배경/목표
50-Step 문서의 미구현 성능/운영 파트를 기존 심화(14~15) 형식으로 압축 추가한다.
- **Step 16 (성능)**: 50-Step 30~34 압축 — AsyncItemProcessor/AsyncItemWriter(비동기),
  fetchSize/JDBC batch 튜닝 포인트, 동기 vs 비동기 성능 비교 테스트
- **Step 17 (운영)**: 50-Step 36~37(+39 언급) 압축 — JobOperator/JobRegistry,
  실패 Job 운영 재기동(restart), JobExplorer 실행 이력
- 미채택: 38 알림 연동/40 대시보드(외부 인프라 필요), 47~50 대량 알림(제2 캡스톤 — 분량 과대)

### 설계 결정
| 결정 | 내용 | 이유 |
|------|------|------|
| Step 16 무대 | 휴면회원(DORMANT 15명) 알림 발송 — notification_history 테이블 신설 | 50-Step 47~50의 알림 도메인을 소규모 차용, 기존 시드 재사용 |
| 지연 시뮬레이션 | Processor에 20ms sleep (외부 발송 API 흉내) | 15건 x 20ms = 동기 300ms 바닥 보장 → 비교 테스트 결정성 |
| 비동기 대비 Step 14 | AsyncItemProcessor는 reader가 단일 스레드 유지 — 커서 리더 그대로 OK | 멀티스레드 Step(페이징 강제)과의 차별점이 곧 교육 포인트 |
| 의존성 | spring-batch-integration (Async 2종의 출처, 버전 Boot BOM) | 코어에 없다는 것 자체가 함정/교육 포인트 |
| Step 17 재기동 데모 | 정적 스위치(BROKEN)로 환경 장애 시뮬레이션 → 복구 후 operator.restart | 동일 파라미터 재시작의 결정적 데모 (교보재 규약 준수: 정리 의무) |
| JobRegistry | JobRegistryBeanPostProcessor를 OpsDemoJobConfig에 명시 등록 | 등록 누락 = NoSuchJobException 함정의 교육화 |

### 구현 결과 (2026-06-12)
- Step 16 `f3abdaa` / Step 17 `3c0d68b` — 매 커밋 모듈 테스트 그린 (115건, +10)
- 계획 대비 전부 계획대로 구현. JobOperator는 Boot 자동구성(SimpleJobOperator) 사용 확인
- spring-troubleshoot 진단표에 배치 섹션 신설 (5개 함정 자산화)

## Plan 4. 제2 캡스톤 — 대량 알림 발송 Job (Step 18, 2026-06-12 사용자 요청으로 착수)

### 배경/목표
50-Step 47~50(대량 알림 발송)을 제2 캡스톤으로 구현한다. 제1 캡스톤(13, 필수 트랙 종합)과
구별되는 정체성: **심화 트랙(14~17) 무기의 종합 + 설계 판단** — 어떤 병렬화를 고를 것인가,
부분 실패를 어떻게 다룰 것인가, 재발송을 어떻게 막을 것인가.

### 설계 결정
| 결정 | 내용 | 이유 |
|------|------|------|
| 형식 | Step 13과 동일 — 프로덕션 모범 제공, 테스트 전략 수립이 과제 (Requirements + warmup + answer) | 캡스톤 형식 일관성 |
| 무대 | ACTIVE 30명에게 캠페인 알림 → notification_history 적재 (시드 무변경) | WITHDRAWN 발송=사고 — 대상자 필터가 첫 요구사항 |
| 병렬화 | **Partitioning 채택** (MemberIdRangePartitioner 재사용, gridSize 3) | 50-Step 49와 정합 + 기존 자산 재사용. Async+skip 조합은 Future 언래핑 시점에 skip 의미론이 꼬임 — 미채택 사유를 교육 포인트로 문서화 |
| 부분 실패 | faultTolerant skip(NotificationSendException) + skipLimit + 전용 SkipListener 로그 | Step 11 재적용 — 1명 실패가 30명 발송을 막으면 안 된다 |
| 재발송 방지 | reader 쿼리의 NOT EXISTS (campaign 접두 메시지) — **자연 멱등** | Step 12 철학. 실패 복구 시나리오(스킵된 2명만 재발송)가 보상으로 따라온다 |
| 발송기 | MarketingNotificationSender + FAIL_MEMBER_IDS 정적 스위치 (교보재 규약: 운영 금지+정리 의무) | 결정적 실패 연출 (지연 시뮬은 Step 16에서 다뤘으므로 생략 — 캡스톤은 복원력 중심) |
| 메시지 접두 | `캠페인명: ` 콜론 구분 (대괄호 금지) | MS-SQL LIKE에서 [ ]는 와일드카드 — H2에선 통과하고 실서버에서 깨지는 함정 회피 |

### 구현 결과 (2026-06-12)
- 계획대로 구현 (해시는 task.md). 테스트 작성 중 실전 함정 2건 발견·자산화:
  ① @SpringBatchTest에서 JobExecution 반환 헬퍼 = 잡 스코프 팩토리 오인 → 전 테스트 사망
  ② 파티션 manager Step의 카운트 집계 → 전체 합산 시 이중 계산 (워커만 필터)
  → 둘 다 spring-troubleshoot 진단표 + Step 18 해설(3-4)에 기록

## Plan 5. 대량 데이터 성능 실습 — Step 19 (2026-06-12 사용자 요청으로 착수)

### 배경/목표
50-Step 31~33(성능 측정/DB 튜닝/메모리)의 "측정 실습" 버전. Step 16이 비동기라는
처방을 가르쳤다면, Step 19는 **측정하는 법** 자체를 가르친다 — 대량 데이터를 만들고,
재고, 구조적 지표로 봉인하는 실습.

### 설계 결정
| 결정 | 내용 | 이유 |
|------|------|------|
| 데이터 격리 | 전용 테이블 bulk_member/bulk_archive (schema.sql 추가, 시드 0) — 테스트가 생성/정리 | 공유 시드(50명) 무변경 원칙. 카운트 기반 기존 테스트 불침범 |
| 규모 | 2만 건 (생성 비교는 5천 x 2) | H2 인메모리에서 수 초 내 — "대량의 감각"과 테스트 속도의 절충. 10만+ 설계는 sql 문서 참조로 연결 |
| 단언 원칙 | **시간은 로그(관찰), 단언은 구조(커밋 횟수/건수)** | 성능 수치는 머신 의존 = flaky. Step 16 3원칙의 확장: 바닥 없는 시간 비교는 단언하지 않는다 |
| 실험 A | 생성 전략: 건별 INSERT vs JdbcTemplate.batchUpdate (각 5천) | 대량 데이터는 "만드는 것"부터 기술 — 왕복 횟수의 체감 |
| 실험 B | chunk 크기(100 vs 2000)와 commitCount — chunkSize를 JobParameter로 (@JobScope Step) | 커밋 횟수는 결정적 구조 지표 — ceil(N/chunk)(+종료 커밋) 공식 체득 |
| 커밋 수 단언 | 정확값 대신 관계식(commits100 > 10 x commits2000) + exercise에서 공식(40 또는 41) | 마지막 빈 chunk의 +1 커밋 구현 디테일에 강건하게 |

### 구현 결과 (2026-06-12, Plan 5)
- 계획대로 구현 (해시는 task.md). 단언 원칙 확립: 시간은 로그 관찰 / 단언은 구조
  (commitCount 관계식·isBetween — 종료 커밋 +1 구현 디테일에 강건하게)
- 배치 모듈 테스트 클래스패스에 Lombok 부재 확인 (기존 테스트 관례대로 LoggerFactory 직접 사용)
