-- =========================================================
-- 시드 데이터
-- 비밀번호는 {noop} 접두사(평문) — DelegatingPasswordEncoder가 해석한다.
-- 학습용이라 읽기 쉽게 평문을 썼을 뿐, 실서비스에서는 절대 금지!
-- (회원가입으로 들어오는 비밀번호는 {bcrypt}로 인코딩되어 저장된다)
-- =========================================================

INSERT INTO member (username, password, nickname, role) VALUES ('writer1', '{noop}spring123!', '글쓴이일호', 'USER');
INSERT INTO member (username, password, nickname, role) VALUES ('writer2', '{noop}spring123!', '글쓴이이호', 'USER');
INSERT INTO member (username, password, nickname, role) VALUES ('admin',   '{noop}spring123!', '운영자',     'ADMIN');

-- 게시글 15건: 페이징 테스트용 (10건 + 5건), 제목에 '테스트'가 들어간 글 3건은 검색 테스트용
INSERT INTO post (writer, title, content) VALUES ('writer1', 'Spring 공부 기록 1',  '오늘은 DI를 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer2', 'Spring 공부 기록 2',  '오늘은 AOP를 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer1', '테스트 작성법 정리 1', 'given-when-then');
INSERT INTO post (writer, title, content) VALUES ('writer2', 'Spring 공부 기록 3',  '오늘은 MVC를 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer1', 'Spring 공부 기록 4',  '오늘은 Bean을 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer2', '테스트 작성법 정리 2', 'Mockito 사용법');
INSERT INTO post (writer, title, content) VALUES ('writer1', 'Spring 공부 기록 5',  '오늘은 JDBC를 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer2', 'Spring 공부 기록 6',  '오늘은 트랜잭션을 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer1', 'Spring 공부 기록 7',  '오늘은 MyBatis를 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer2', 'Spring 공부 기록 8',  '오늘은 H2를 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer1', '테스트 작성법 정리 3', 'MybatisTest 사용법');
INSERT INTO post (writer, title, content) VALUES ('writer2', 'Spring 공부 기록 9',  '오늘은 Validation을 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer1', 'Spring 공부 기록 10', '오늘은 Security를 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer2', 'Spring 공부 기록 11', '오늘은 Filter를 배웠다');
INSERT INTO post (writer, title, content) VALUES ('writer1', 'Spring 공부 기록 12', '오늘은 Interceptor를 배웠다');
