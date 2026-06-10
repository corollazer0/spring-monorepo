package com.testonboarding.config;

import com.testonboarding.common.interceptor.AdminCheckInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 설정 — 인터셉터를 어느 경로에 적용할지 등록한다.
 *
 * Filter와 달리 Interceptor는 Bean으로 만들기만 해서는 동작하지 않는다 —
 * 반드시 이렇게 경로와 함께 등록해야 한다. (등록 누락이 단골 버그!)
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AdminCheckInterceptor adminCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminCheckInterceptor)
                .addPathPatterns("/api/admin/**");
    }
}
