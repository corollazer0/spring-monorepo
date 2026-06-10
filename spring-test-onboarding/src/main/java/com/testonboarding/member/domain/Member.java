package com.testonboarding.member.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 회원 도메인.
 *
 * - password: 평문이 아닌 PasswordEncoder로 인코딩된 값만 저장한다
 * - @Setter는 MyBatis 결과 매핑과 useGeneratedKeys(keyProperty)에 필요
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    private Long memberId;
    private String username;
    private String password;
    private String nickname;
    private Role role;
    private LocalDateTime createdAt;
}
