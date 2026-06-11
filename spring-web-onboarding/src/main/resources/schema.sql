-- =========================================================
-- WebFlow 미니 커머스 스키마 (MS-SQL 스타일 DDL — H2 MODE=MSSQLServer)
--
-- 테이블명 주의: ORDER는 SQL 예약어(ORDER BY)! → orders로 명명
-- (BatchFlow의 bank_transaction과 같은 실무 교훈)
-- =========================================================

DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS product;

CREATE TABLE product (
    product_id  BIGINT IDENTITY PRIMARY KEY,
    name        NVARCHAR(100) NOT NULL,
    category    NVARCHAR(30)  NOT NULL,   -- KEYBOARD / MOUSE / MONITOR
    price       BIGINT        NOT NULL,
    stock       INT           NOT NULL,
    image_path  NVARCHAR(300) NULL,       -- Step 5에서 채워진다
    created_at  DATETIME      NOT NULL DEFAULT GETDATE()
);

CREATE TABLE orders (
    order_id    BIGINT IDENTITY PRIMARY KEY,
    product_id  BIGINT        NOT NULL,
    quantity    INT           NOT NULL,
    total_price BIGINT        NOT NULL,
    status      NVARCHAR(30)  NOT NULL,   -- PENDING_PAYMENT / PAID / CANCELLED
    payment_key NVARCHAR(100) NULL,       -- 결제 승인 키 (Step 3에서 채워진다)
    ordered_at  DATETIME      NOT NULL DEFAULT GETDATE()
);
