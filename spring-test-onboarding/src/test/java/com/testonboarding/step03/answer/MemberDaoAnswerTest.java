package com.testonboarding.step03.answer;

import com.testonboarding.member.dao.MemberDao;
import com.testonboarding.member.domain.Member;
import com.testonboarding.member.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 3 — answer] MemberDaoExerciseTest 모범답안
 *
 * 채점 포인트:
 * - "없는 데이터" 시나리오(null 반환)를 잊지 않았는가
 * - insert 검증을 "객체"가 아니라 "재조회"로 했는가
 * - enum ↔ 문자열 왕복 변환을 검증했는가
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("회원 DAO (모범답안)")
class MemberDaoAnswerTest {

    @Autowired
    private MemberDao memberDao;

    @Test
    @DisplayName("존재하는 아이디로 조회하면 회원이 반환된다")
    void findByUsername_존재하는아이디_회원반환() {
        // when (TODO 1 답)
        Member member = memberDao.findByUsername("writer1");

        // then (TODO 2 답)
        assertThat(member).isNotNull();
        assertThat(member.getNickname()).isEqualTo("글쓴이일호");
    }

    @Test
    @DisplayName("없는 아이디로 조회하면 null이 반환된다")
    void findByUsername_없는아이디_null반환() {
        // when & then (TODO 3 답)
        assertThat(memberDao.findByUsername("ghost")).isNull();
    }

    @Test
    @DisplayName("insert 후 PK가 채번되고, role(enum)이 문자열 컬럼과 정확히 왕복 변환된다")
    void insert_새회원_PK채번및enum매핑확인() {
        // given (TODO 4 답)
        Member member = Member.builder()
                .username("newadmin")
                .password("{noop}spring123!")
                .nickname("새관리자")
                .role(Role.ADMIN)
                .build();

        // when (TODO 5 답)
        memberDao.insert(member);

        // then (TODO 6 답)
        assertThat(member.getMemberId()).isNotNull();

        // then (TODO 7 답) : DB 왕복 후에도 enum이 보존되는지 — 재조회로 검증
        Member reloaded = memberDao.findById(member.getMemberId());
        assertThat(reloaded.getRole()).isEqualTo(Role.ADMIN);
        assertThat(reloaded.getUsername()).isEqualTo("newadmin");
    }
}
