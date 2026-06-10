package com.testonboarding.auth.jwt.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * 토큰 발급 요청 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class TokenRequest {

    @NotBlank(message = "아이디는 필수입니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    public TokenRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
