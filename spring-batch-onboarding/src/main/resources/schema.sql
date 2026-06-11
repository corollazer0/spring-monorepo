-- =========================================================
-- BatchFlow 도메인 스키마 (MS-SQL 스타일 DDL — H2 MODE=MSSQLServer)
--
-- 테이블명 주의: MS-SQL에서 TRANSACTION은 예약어(BEGIN TRANSACTION)라서
-- 테이블명으로 쓰면 매번 대괄호 인용이 필요하다 → bank_transaction으로 명명.
-- (예약어를 피해 이름 짓는 것 자체가 실무 교훈!)
-- =========================================================

DROP TABLE IF EXISTS settlement;
DROP TABLE IF EXISTS bank_transaction;
DROP TABLE IF EXISTS member;

CREATE TABLE member (
    member_id     BIGINT IDENTITY PRIMARY KEY,
    name          NVARCHAR(50)  NOT NULL,
    email         NVARCHAR(100) NOT NULL,
    status        NVARCHAR(20)  NOT NULL,   -- ACTIVE / DORMANT / WITHDRAWN
    last_login_at DATETIME      NULL,
    dormant_at    DATETIME      NULL
);

CREATE TABLE bank_transaction (
    transaction_id   BIGINT IDENTITY PRIMARY KEY,
    member_id        BIGINT       NOT NULL,
    tx_type          NVARCHAR(20) NOT NULL, -- DEPOSIT / WITHDRAW
    amount           BIGINT       NOT NULL,
    transaction_date DATE         NOT NULL
);

-- 일일 정산 결과 (Step 13 캡스톤에서 채워진다)
CREATE TABLE settlement (
    settlement_id     BIGINT IDENTITY PRIMARY KEY,
    settlement_date   DATE         NOT NULL,
    member_id         BIGINT       NOT NULL,
    total_deposit     BIGINT       NOT NULL,
    total_withdraw    BIGINT       NOT NULL,
    net_amount        BIGINT       NOT NULL,
    transaction_count INT          NOT NULL,
    created_at        DATETIME     NOT NULL DEFAULT GETDATE()
);
