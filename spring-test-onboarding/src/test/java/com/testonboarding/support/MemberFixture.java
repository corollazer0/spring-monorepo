package com.testonboarding.support;

import com.testonboarding.member.domain.Member;
import com.testonboarding.member.domain.Role;

/**
 * [심화 Step 11] 회원 테스트 데이터 공장 (Object Mother 패턴)
 *
 * 목적: 테스트마다 반복되는 builder 체인을 한 곳에 모아
 * - 준비 코드 중복 제거
 * - "이 테스트에서 중요한 값"만 드러나게 (나머지는 합리적 기본값)
 * - 도메인에 필드가 추가될 때 수정 지점을 한 곳으로
 *
 * 사용:
 *   Member member = MemberFixture.writer1();                       // 시드와 동일한 대표 회원
 *   Member custom = MemberFixture.aMember().nickname("별명").build(); // 일부만 바꿔서
 */
public class MemberFixture {

    private MemberFixture() {
    }

    /** 시드 데이터(data.sql)의 writer1과 동일한 모습의 대표 회원 */
    public static Member writer1() {
        return aMember().build();
    }

    public static Member admin() {
        return aMember()
                .memberId(3L)
                .username("admin")
                .nickname("운영자")
                .role(Role.ADMIN)
                .build();
    }

    /** 합리적 기본값이 채워진 builder — 테스트는 관심 있는 필드만 덮어쓴다 */
    public static Member.MemberBuilder aMember() {
        return Member.builder()
                .memberId(1L)
                .username("writer1")
                .password("{noop}spring123!")
                .nickname("글쓴이일호")
                .role(Role.USER);
    }
}
