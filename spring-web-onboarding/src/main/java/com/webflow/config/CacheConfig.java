package com.webflow.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 캐시 설정 (Step 6).
 *
 * ConcurrentMapCacheManager = JVM 안의 Map 한 장 (학습/소규모용).
 * 실무에선 같은 애노테이션 그대로 Redis 등으로 교체한다 — @Cacheable의 가치는
 * "캐시 기술이 바뀌어도 비즈니스 코드는 그대로"라는 추상화에 있다.
 *
 * 주의: 인메모리 캐시는 서버가 2대면 서로 다른 값을 기억한다(불일치).
 * 그때가 Redis 같은 중앙 캐시로 옮길 시점이다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 캐시 이름은 상수로 — 문자열 오타가 "조용히 다른 캐시"를 만드는 사고 방지 */
    public static final String PRODUCTS = "products";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PRODUCTS);
    }
}
