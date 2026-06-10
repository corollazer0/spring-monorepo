package com.testonboarding.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원가입 요청 DTO. (검증 어노테이션은 Step 5에서 추가)
 */
@Getter
@Setter
@NoArgsConstructor
public class MemberSignupRequest {

    private String username;
    private String password;
    private String nickname;

    public MemberSignupRequest(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }
}
