package com.testonboarding.auth.jwt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 토큰 발급 응답 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String tokenType = "Bearer";

    public TokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
