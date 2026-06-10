package com.testonboarding.member.dao;

import com.testonboarding.member.domain.Member;
import org.apache.ibatis.annotations.Mapper;

/**
 * 회원 DAO — 구현은 resources/mybatis/mapper/MemberMapper.xml
 */
@Mapper
public interface MemberDao {

    Member findById(Long memberId);

    Member findByUsername(String username);

    void insert(Member member);
}
