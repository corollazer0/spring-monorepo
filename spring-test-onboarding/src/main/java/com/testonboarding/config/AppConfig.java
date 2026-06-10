package com.testonboarding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 애플리케이션 공통 빈 설정.
 *
 * PasswordEncoder를 SecurityConfig(Step 4)가 아닌 별도 설정으로 분리한 이유:
 * MemberService가 의존하는 빈이므로, Security 설정 변경과 무관하게 항상 존재해야 한다.
 */
@Configuration
public class AppConfig {

    /**
     * DelegatingPasswordEncoder (Spring Security 권장 기본값).
     *
     * 저장된 비밀번호의 접두사({bcrypt}, {noop} 등)를 보고 알맞은 인코더로 위임한다.
     * - 회원가입으로 들어온 비밀번호 → {bcrypt}로 인코딩되어 저장
     * - 학습용 시드 데이터(data.sql) → {noop}평문 으로 읽기 쉽게 저장 (실서비스 금지!)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
