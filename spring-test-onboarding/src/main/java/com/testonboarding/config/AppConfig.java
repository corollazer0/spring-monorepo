package com.testonboarding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 애플리케이션 공통 빈 설정.
 *
 * PasswordEncoder를 SecurityConfig(Step 4)가 아닌 별도 설정으로 분리한 이유:
 * MemberService가 의존하는 빈이므로, Security 설정 변경과 무관하게 항상 존재해야 한다.
 */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
