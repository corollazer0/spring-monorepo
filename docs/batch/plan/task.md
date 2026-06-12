# BatchFlow 태스크 보드 (Living Document)

> 운영 규칙: 작업 전 [plan.md](./plan.md)에 계획 추가 → 완료 커밋 시 `[x]` + 해시 병기.

---

## Plan 1: 필수 트랙 재편 및 완성 (Step 1~15) — ✅ 완료

- [x] Step 1: 인프라 재정비 — MSSQLServer 전환, JPA 제거, 01 커리큘럼, CLAUDE.md 갱신 — `7aab541`
- [x] Step 2: Hello Job 3종 보강 — 실행과 장부의 연결 — `c494196`
- [x] Step 3: JobParameters & JobInstance — 재실행 거부 사건 — `0c0c68a`
- [x] Step 4: Flow 제어 — 분기와 Decider — `4e532cf`
- [x] Step 5: ExecutionContext — Step 간 공유 — `e91cd16`
- [x] Step 6: Chunk 첫 경험 + 도메인 스키마 — `6b2a163`
- [x] Step 7: JdbcCursorItemReader — `ad2d127`
- [x] Step 8: JdbcPagingItemReader + 리더 단독 테스트 — `510cf29`
- [x] Step 9: Processor 순수 단위 — `aa39a14`
- [x] Step 10: 휴면전환 통합 (진짜 커밋/원상복구/자연 멱등) — `adf2816`
- [x] Step 11: Skip/Retry/Listener — `44cf751`
- [x] Step 12: 재시작과 멱등성 (saveState 함정) — `44b7cbc`
- [x] Step 13: 캡스톤 일일 정산 — `3e7b05c`
- [x] Step 14(심화): 멀티스레드/병렬 Flow + step10/12 AfterEach 보강 — `93d2058`
- [x] Step 15(심화): Partitioning — `263bd17`
- [x] 초보자 README + plan/task 문서 — `6104efc`

## Plan 3: 성능/운영 심화 (Step 16~17) — 🔄 진행 중

- [x] Step 16(심화): 비동기 처리 — AsyncItemProcessor/Writer + 동기 vs 비동기 성능 비교 (50-Step 30~34) — `f3abdaa`
- [x] Step 17(심화): JobOperator와 실행 이력 — 운영자의 콘솔 (50-Step 36~37·39) — (이번 커밋)

## Plan 2: 다음 후보 — 📝 미착수

- [ ] 학습자 파일럿 운영
- [ ] (선택) 대량 알림 발송 제2 캡스톤 (50-Step 47~50)
- [ ] (선택) 대량 데이터 성능 실습 Step (10만건 스키마 문서 기반)
