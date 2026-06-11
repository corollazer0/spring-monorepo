-- =========================================================
-- WebFlow 시드 데이터 (고정 시나리오 — 모든 테스트의 공통 출발선)
--
-- 상품 12종: KEYBOARD 5(품절 1 포함) / MOUSE 4 / MONITOR 3
--   검색 기준값: '키보드' 5건, '기계식' 2건 / 최저가: 무선 마우스(35000)
-- 주문 6건: PAID 2 / PENDING_PAYMENT 3(오래된 것 2 = Step 7 정리 대상) / CANCELLED 1
-- =========================================================

INSERT INTO product (name, category, price, stock) VALUES ('기계식 키보드 RED',  'KEYBOARD', 89000, 10);
INSERT INTO product (name, category, price, stock) VALUES ('기계식 키보드 BLUE', 'KEYBOARD', 95000, 10);
INSERT INTO product (name, category, price, stock) VALUES ('저소음 키보드',      'KEYBOARD', 45000, 10);
INSERT INTO product (name, category, price, stock) VALUES ('무접점 키보드',      'KEYBOARD', 159000, 5);
INSERT INTO product (name, category, price, stock) VALUES ('게이밍 키보드',      'KEYBOARD', 120000, 0); -- ★품절
INSERT INTO product (name, category, price, stock) VALUES ('무선 마우스',        'MOUSE',    35000, 10); -- ★최저가
INSERT INTO product (name, category, price, stock) VALUES ('게이밍 마우스',      'MOUSE',    59000, 10);
INSERT INTO product (name, category, price, stock) VALUES ('버티컬 마우스',      'MOUSE',    42000, 10);
INSERT INTO product (name, category, price, stock) VALUES ('트랙볼 마우스',      'MOUSE',    78000, 3);
INSERT INTO product (name, category, price, stock) VALUES ('27인치 모니터',      'MONITOR',  250000, 10);
INSERT INTO product (name, category, price, stock) VALUES ('32인치 모니터',      'MONITOR',  410000, 4);
INSERT INTO product (name, category, price, stock) VALUES ('휴대용 모니터',      'MONITOR',  180000, 6);

-- 주문: ordered_at은 고정값 (현재시각 의존 금지 — 테스트 결정성)
INSERT INTO orders (product_id, quantity, total_price, status, payment_key, ordered_at)
VALUES (1, 1,  89000, 'PAID', 'PAY-001', '2026-06-10 10:00:00');
INSERT INTO orders (product_id, quantity, total_price, status, payment_key, ordered_at)
VALUES (6, 2,  70000, 'PAID', 'PAY-002', '2026-06-10 11:00:00');
INSERT INTO orders (product_id, quantity, total_price, status, ordered_at)
VALUES (3, 1,  45000, 'PENDING_PAYMENT', '2026-06-01 09:00:00'); -- ★오래된 미결제 (정리 대상)
INSERT INTO orders (product_id, quantity, total_price, status, ordered_at)
VALUES (7, 1,  59000, 'PENDING_PAYMENT', '2026-06-01 10:00:00'); -- ★오래된 미결제 (정리 대상)
INSERT INTO orders (product_id, quantity, total_price, status, ordered_at)
VALUES (10, 1, 250000, 'PENDING_PAYMENT', '2026-06-11 08:00:00'); -- 최근 미결제 (정리 비대상)
INSERT INTO orders (product_id, quantity, total_price, status, ordered_at)
VALUES (2, 1,  95000, 'CANCELLED', '2026-05-20 15:00:00');
