-- =========================================================
-- BatchFlow 시드 데이터 (고정 시나리오 — 모든 테스트의 공통 출발선)
--
-- 회원 50명 분포:
--   1~20  : ACTIVE, 최근 로그인(2026-06-01)              → 휴면 전환 비대상
--   21~30 : ACTIVE, 마지막 로그인 2024-01-15 (1년 이상↑)  → ★휴면 전환 대상 10명
--   31~45 : DORMANT (이미 휴면)
--   46~50 : WITHDRAWN (탈퇴)
--
-- 거래 15건: 2026-06-10자 9건(정산 캡스톤 검증 시나리오) + 다른 날짜 6건
--   [06-10 정산 기대값] m1: +30000/-5000=25000(3건), m2: +50000(1건),
--                       m3: -10000(2건), m4: 0(2건), m5: +8000(1건)
-- =========================================================

INSERT INTO member (name, email, status, last_login_at) VALUES ('회원01', 'member01@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원02', 'member02@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원03', 'member03@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원04', 'member04@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원05', 'member05@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원06', 'member06@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원07', 'member07@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원08', 'member08@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원09', 'member09@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원10', 'member10@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원11', 'member11@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원12', 'member12@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원13', 'member13@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원14', 'member14@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원15', 'member15@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원16', 'member16@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원17', 'member17@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원18', 'member18@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원19', 'member19@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원20', 'member20@test.com', 'ACTIVE', '2026-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원21', 'member21@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원22', 'member22@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원23', 'member23@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원24', 'member24@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원25', 'member25@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원26', 'member26@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원27', 'member27@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원28', 'member28@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원29', 'member29@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원30', 'member30@test.com', 'ACTIVE', '2024-01-15');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원31', 'member31@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원32', 'member32@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원33', 'member33@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원34', 'member34@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원35', 'member35@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원36', 'member36@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원37', 'member37@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원38', 'member38@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원39', 'member39@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원40', 'member40@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원41', 'member41@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원42', 'member42@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원43', 'member43@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원44', 'member44@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at, dormant_at) VALUES ('회원45', 'member45@test.com', 'DORMANT', '2023-05-01', '2024-06-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원46', 'member46@test.com', 'WITHDRAWN', '2022-01-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원47', 'member47@test.com', 'WITHDRAWN', '2022-01-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원48', 'member48@test.com', 'WITHDRAWN', '2022-01-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원49', 'member49@test.com', 'WITHDRAWN', '2022-01-01');
INSERT INTO member (name, email, status, last_login_at) VALUES ('회원50', 'member50@test.com', 'WITHDRAWN', '2022-01-01');

-- ── 2026-06-10 거래 9건 (정산 캡스톤의 고정 시나리오) ──────────────
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (1, 'DEPOSIT',  10000, '2026-06-10');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (1, 'DEPOSIT',  20000, '2026-06-10');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (1, 'WITHDRAW',  5000, '2026-06-10');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (2, 'DEPOSIT',  50000, '2026-06-10');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (3, 'WITHDRAW',  7000, '2026-06-10');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (3, 'WITHDRAW',  3000, '2026-06-10');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (4, 'DEPOSIT',  15000, '2026-06-10');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (4, 'WITHDRAW', 15000, '2026-06-10');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (5, 'DEPOSIT',   8000, '2026-06-10');

-- ── 다른 날짜 거래 6건 (정산 대상 아님 — 날짜 필터 검증용) ──────────
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (1, 'DEPOSIT',  99000, '2026-06-09');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (2, 'WITHDRAW',  1000, '2026-06-09');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (6, 'DEPOSIT',  30000, '2026-06-09');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (7, 'DEPOSIT',  12000, '2026-06-08');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (8, 'WITHDRAW',  2000, '2026-06-08');
INSERT INTO bank_transaction (member_id, tx_type, amount, transaction_date) VALUES (9, 'DEPOSIT',   4000, '2026-06-08');
