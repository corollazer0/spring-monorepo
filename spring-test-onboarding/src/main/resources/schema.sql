-- =========================================================
-- TestCraft 스키마 (MS-SQL 스타일 DDL — H2 MODE=MSSQLServer에서 실행)
--
-- 실무 MS-SQL과 동일하게 쓰는 것:
--   BIGINT IDENTITY (자동 채번), NVARCHAR, GETDATE()
-- H2 호환 모드의 한계로 다른 것:
--   IDENTITY(1,1) 시드/증가값 지정 문법은 H2가 못 읽음 → IDENTITY만 사용
-- =========================================================

DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS post;
DROP TABLE IF EXISTS member;

CREATE TABLE member (
    member_id   BIGINT IDENTITY PRIMARY KEY,
    username    NVARCHAR(50)   NOT NULL UNIQUE,
    password    NVARCHAR(100)  NOT NULL,
    nickname    NVARCHAR(50)   NOT NULL,
    role        NVARCHAR(20)   NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT GETDATE()
);

CREATE TABLE post (
    post_id     BIGINT IDENTITY PRIMARY KEY,
    writer      NVARCHAR(50)   NOT NULL,
    title       NVARCHAR(200)  NOT NULL,
    content     NVARCHAR(2000) NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT GETDATE()
);

CREATE TABLE comment (
    comment_id  BIGINT IDENTITY PRIMARY KEY,
    post_id     BIGINT         NOT NULL,
    writer      NVARCHAR(50)   NOT NULL,
    content     NVARCHAR(500)  NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT GETDATE()
);
