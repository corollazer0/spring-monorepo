package com.webflow.step08.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [Web Step 8 — exercise] 운영 계약을 직접 봉인해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 ActuatorEndpointTest)
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 *
 * 왜 이런 테스트가 필요한가: 모니터링 시스템(Prometheus 등)은 이 응답 모양을
 * "긁어가도록" 설정된다 — 응답 구조가 바뀌면 알람이 침묵한다. 이것도 API 계약이다.
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step08.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Actuator 계약 (연습문제)")
class ActuatorContractExerciseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("health 응답에 uploadDir 컴포넌트의 path 상세가 있다")
    void health_uploadDir상세_노출() throws Exception {
        // when & then : GET /actuator/health 에서
        //   $.components.uploadDir.details.path 가 존재하는지 검증하세요
        //   (힌트: jsonPath("...").exists())
        // TODO 1
    }

    @Test
    @DisplayName("특정 지표 단건 조회: /actuator/metrics/jvm.memory.used")
    void metrics_단건지표_조회() throws Exception {
        // when & then : GET /actuator/metrics/jvm.memory.used 가 200이고
        //   $.name 이 "jvm.memory.used"인지, $.measurements 가 존재하는지 검증하세요
        // TODO 2
    }

    @Test
    @DisplayName("shutdown 엔드포인트는 절대 열려 있으면 안 된다 (서버를 끄는 문!)")
    void shutdown_비노출_404() throws Exception {
        // when & then : POST /actuator/shutdown 이 404인지 검증하세요
        //   (이 문이 열려 있으면 누구나 HTTP 한 방으로 서버를 내릴 수 있다)
        // TODO 3
    }
}
