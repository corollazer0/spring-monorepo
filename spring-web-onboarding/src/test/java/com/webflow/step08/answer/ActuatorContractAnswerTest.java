package com.webflow.step08.answer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Web Step 8 — answer] ActuatorContractExerciseTest 모범답안
 *
 * 채점 포인트: TODO 3 — "열려 있지 않음"도 적극적으로 봉인했는가.
 * 보안 테스트의 절반은 "없어야 할 것이 없는지" 확인하는 일이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Actuator 계약 (모범답안)")
class ActuatorContractAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("health 응답에 uploadDir 컴포넌트의 path 상세가 있다")
    void health_uploadDir상세_노출() throws Exception {
        // when & then (TODO 1 답)
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.uploadDir.details.path").exists());
    }

    @Test
    @DisplayName("특정 지표 단건 조회: /actuator/metrics/jvm.memory.used")
    void metrics_단건지표_조회() throws Exception {
        // when & then (TODO 2 답)
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(jsonPath("$.measurements").exists());
    }

    @Test
    @DisplayName("shutdown 엔드포인트는 절대 열려 있으면 안 된다 (서버를 끄는 문!)")
    void shutdown_비노출_404() throws Exception {
        // when & then (TODO 3 답)
        mockMvc.perform(post("/actuator/shutdown"))
                .andExpect(status().isNotFound());
    }
}
