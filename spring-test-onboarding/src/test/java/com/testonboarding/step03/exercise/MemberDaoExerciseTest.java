package com.testonboarding.step03.exercise;

import com.testonboarding.member.dao.MemberDao;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

/**
 * [Step 3 — exercise] MemberDao 테스트를 직접 작성해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 BoardDaoTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 시드 데이터(data.sql): writer1, writer2 (USER), admin (ADMIN)
 *
 * 힌트: 클래스에 이미 붙어있는 두 어노테이션이 왜 필요한지 설명할 수 있나요?
 *       특히 Replace.NONE을 지우면 어떤 일이 생기는지 직접 지워보고 실행해보세요!
 */
@Disabled("과제: docs/test/education/FOR-Test-Step03.md 참고 후 @Disabled를 제거하고 완성하세요")
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("회원 DAO (연습문제)")
class MemberDaoExerciseTest {

    @Autowired
    private MemberDao memberDao;

    @Test
    @DisplayName("존재하는 아이디로 조회하면 회원이 반환된다")
    void findByUsername_존재하는아이디_회원반환() {
        // when : 시드 데이터의 "writer1"을 조회하세요
        // TODO 1

        // then : null이 아니고, nickname이 "글쓴이일호"인지 검증하세요
        // TODO 2
    }

    @Test
    @DisplayName("없는 아이디로 조회하면 null이 반환된다")
    void findByUsername_없는아이디_null반환() {
        // when & then : "ghost"를 조회하면 null인지 검증하세요
        // TODO 3
    }

    @Test
    @DisplayName("insert 후 PK가 채번되고, role(enum)이 문자열 컬럼과 정확히 왕복 변환된다")
    void insert_새회원_PK채번및enum매핑확인() {
        // given : Member.builder()로 새 회원을 만드세요 (role은 Role.ADMIN으로!)
        // TODO 4

        // when : insert 하세요
        // TODO 5

        // then(1) : memberId가 채번되었는지 검증하세요
        // TODO 6
        // then(2) : findById로 다시 조회해서 role이 Role.ADMIN 그대로인지 검증하세요
        //           (DB에는 'ADMIN' 문자열로 저장됐다가 enum으로 돌아온다 — 이게 깨지는 사고가 실무에 많다!)
        // TODO 7
    }
}
