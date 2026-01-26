-- ============================================================================
-- Step 01 확인용 SQL 스크립트
-- Spring Batch 메타데이터 테이블이 정상적으로 생성되었는지 확인합니다.
-- ============================================================================

-- 1. BATCH_* 테이블 목록 조회
-- 예상 결과: 6개 테이블이 나와야 합니다.
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'PUBLIC'
  AND TABLE_NAME LIKE 'BATCH%'
ORDER BY TABLE_NAME;

/*
예상 결과:
BATCH_JOB_EXECUTION
BATCH_JOB_EXECUTION_CONTEXT
BATCH_JOB_EXECUTION_PARAMS
BATCH_JOB_INSTANCE
BATCH_STEP_EXECUTION
BATCH_STEP_EXECUTION_CONTEXT
*/

-- ============================================================================

-- 2. 각 테이블의 구조 확인

-- BATCH_JOB_INSTANCE: Job의 고유 식별 정보
SELECT * FROM BATCH_JOB_INSTANCE LIMIT 0;
/*
주요 컬럼:
- JOB_INSTANCE_ID: Job Instance의 고유 ID
- JOB_NAME: Job 이름 (예: "dormantMemberJob")
- JOB_KEY: Job Parameters의 해시값
*/

-- BATCH_JOB_EXECUTION: Job 실행 시도 기록
SELECT * FROM BATCH_JOB_EXECUTION LIMIT 0;
/*
주요 컬럼:
- JOB_EXECUTION_ID: 실행 ID
- STATUS: COMPLETED, FAILED, STARTED 등
- START_TIME, END_TIME: 시작/종료 시간
- EXIT_CODE, EXIT_MESSAGE: 종료 코드와 메시지
*/

-- BATCH_JOB_EXECUTION_PARAMS: Job 실행 파라미터
SELECT * FROM BATCH_JOB_EXECUTION_PARAMS LIMIT 0;
/*
주요 컬럼:
- PARAMETER_NAME: 파라미터 이름 (예: "requestDate")
- PARAMETER_VALUE: 파라미터 값 (예: "2025-01-26")
- PARAMETER_TYPE: 타입 (STRING, LONG, DATE, DOUBLE)
*/

-- BATCH_STEP_EXECUTION: Step 실행 기록
SELECT * FROM BATCH_STEP_EXECUTION LIMIT 0;
/*
주요 컬럼:
- STEP_EXECUTION_ID: Step 실행 ID
- STEP_NAME: Step 이름
- READ_COUNT, WRITE_COUNT: 읽기/쓰기 건수
- COMMIT_COUNT, ROLLBACK_COUNT: 커밋/롤백 횟수
- STATUS, EXIT_CODE: 상태와 종료 코드
*/

-- BATCH_STEP_EXECUTION_CONTEXT: Step ExecutionContext
SELECT * FROM BATCH_STEP_EXECUTION_CONTEXT LIMIT 0;
/*
주요 컬럼:
- STEP_EXECUTION_ID: Step 실행 ID
- SERIALIZED_CONTEXT: JSON 형태의 컨텍스트 데이터
*/

-- BATCH_JOB_EXECUTION_CONTEXT: Job ExecutionContext
SELECT * FROM BATCH_JOB_EXECUTION_CONTEXT LIMIT 0;
/*
주요 컬럼:
- JOB_EXECUTION_ID: Job 실행 ID
- SERIALIZED_CONTEXT: JSON 형태의 컨텍스트 데이터
*/

-- ============================================================================

-- 3. 현재 데이터 확인 (Step 01에서는 비어있어야 정상)
-- 아직 Job을 실행하지 않았으므로 모든 테이블이 비어있어야 합니다.

SELECT COUNT(*) AS job_instance_count FROM BATCH_JOB_INSTANCE;
-- 예상: 0

SELECT COUNT(*) AS job_execution_count FROM BATCH_JOB_EXECUTION;
-- 예상: 0

SELECT COUNT(*) AS step_execution_count FROM BATCH_STEP_EXECUTION;
-- 예상: 0

-- ============================================================================
-- 참고: H2 콘솔 접속 정보
-- ============================================================================
-- URL: http://localhost:8080/h2-console
-- JDBC URL: jdbc:h2:mem:batchdb
-- User Name: sa
-- Password: (비워둠)
-- ============================================================================
