package com.testonboarding.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 *
 * 역할 분담에 주목:
 * - 형식 검증(필수/길이/문자 종류) → 여기(Bean Validation)
 * - 비즈니스 검증(중복 아이디, 비밀번호 정책) → MemberService
 */
@Getter
@Setter
@NoArgsConstructor
public class MemberSignupRequest {

    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다")
    @Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 영문 소문자와 숫자만 사용할 수 있습니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다")
    private String nickname;

    public MemberSignupRequest(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }
}
