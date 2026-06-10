package com.testonboarding.member.domain;

/**
 * 회원 권한. Spring Security의 hasRole("...")과 연결된다 (Step 6).
 */
public enum Role {
    USER,
    ADMIN
}
