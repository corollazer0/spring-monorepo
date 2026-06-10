-- Step 8 E2E가 만든 데이터 정리
-- RANDOM_PORT 방식에서는 @Transactional 롤백이 동작하지 않으므로 직접 치워야 한다!
DELETE FROM post WHERE writer = 'journey1';
DELETE FROM member WHERE username = 'journey1';
