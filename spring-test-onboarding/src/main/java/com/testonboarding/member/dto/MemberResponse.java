package com.testonboarding.member.dto;

import com.testonboarding.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

/**
 * 회원 응답 DTO — password 같은 민감 정보는 절대 포함하지 않는다.
 */
@Getter
@Builder
public class MemberResponse {

    private final Long memberId;
    private final String username;
    private final String nickname;
    private final String role;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .memberId(member.getMemberId())
                .username(member.getUsername())
                .nickname(member.getNickname())
                .role(member.getRole().name())
                .build();
    }
}
