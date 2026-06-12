# WebFlow 태스크 보드 (Living Document)

> 운영 규칙: 작업 전 [plan.md](./plan.md)에 계획 추가 → 완료 커밋 시 `[x]` + 해시 병기.

---

## Plan 1: 마스터플랜 수립 — ✅ 완료

- [x] 정체성/주제/DB/형식 의사결정 (인터뷰) + 커리큘럼 초안(필수 9 + 심화) — `fc4430b`

## Plan 2: 필수 트랙 구현 — 🔄 진행 중

- [x] Step 1: 스캐폴드 + 커머스 도메인 (MyBatis/MSSQL) — `a09868e`
- [x] Step 2: 목록 API — 페이징·검색·정렬 — `abe0fe8`
- [x] Step 3: 외부 결제 API 연동 기초 (MockRestServiceServer) — `e496b27`
- [x] Step 4: 타임아웃·재시도·장애 격리 — `d7c02fb`
- [x] Step 5: 파일 업로드/다운로드 — `f811b76`
- [x] Step 6: 캐싱 (@Cacheable + 무효화) — `64f5821`
- [x] Step 7: 스케줄링 — 미결제 주문 정리 — `aa1bfcb`
- [x] Step 8: Actuator — 운영의 눈 — `35b3465`
- [x] Step 9: 캡스톤 — 배송 조회 연동 자율 설계 — `58f47d9`
- [x] CLAUDE.md 신규 / 커리큘럼·FOR 문서 (Step 1~9, 각 Step 커밋에 포함)
- [x] 초보자 README + 루트 README Web 행 ✅ + plan/task 마감 — (이번 커밋)

## Plan 3: 심화 — ✅ 완료

- [x] Step 10(심화): 서킷 브레이커 (Resilience4j 1.7.1, WebClient 미채택 — plan.md Plan 3) — `91b1679`
