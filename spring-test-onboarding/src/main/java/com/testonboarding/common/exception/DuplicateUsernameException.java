package com.testonboarding.common.exception;

/**
 * 이미 사용 중인 아이디로 회원가입을 시도할 때 발생. (Step 5에서 409로 변환)
 */
public class DuplicateUsernameException extends BusinessException {

    public DuplicateUsernameException(String username) {
        super("이미 사용 중인 아이디입니다: " + username);
    }
}
