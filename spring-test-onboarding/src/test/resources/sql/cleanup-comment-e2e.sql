-- Step 9 댓글 E2E가 만든 데이터 정리 (RANDOM_PORT는 롤백 불가)
DELETE FROM comment WHERE content = 'E2E 댓글입니다';
