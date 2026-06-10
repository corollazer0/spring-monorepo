package com.testonboarding.member.dao;

import com.testonboarding.member.domain.Member;

/**
 * 회원 DAO 인터페이스. (구현은 Step 3에서 @Mapper + XML로)
 */
public interface MemberDao {

    Member findById(Long memberId);

    Member findByUsername(String username);

    void insert(Member member);
}
