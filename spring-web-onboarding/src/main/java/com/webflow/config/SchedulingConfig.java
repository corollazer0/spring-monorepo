package com.webflow.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화 (Step 7).
 *
 * @ConditionalOnProperty: 테스트에선 app.scheduling.enabled=false로 끈다 —
 * 테스트 도중 배경 스레드가 멋대로 정리 작업을 돌리면 검증이 흔들린다.
 * (Spring Batch의 spring.batch.job.enabled=false와 같은 철학:
 *  "실행 트리거"와 "로직"을 분리하고, 테스트는 로직만 직접 부른다)
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
