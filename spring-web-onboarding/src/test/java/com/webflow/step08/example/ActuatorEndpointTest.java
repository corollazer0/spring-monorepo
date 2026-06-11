package com.webflow.step08.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Web Step 8 — example B] Actuator 엔드포인트 검증 — 열린 곳과 "닫힌 곳" 모두
 *
 * @WebMvcTest로는 안 된다 — Actuator 엔드포인트는 우리 Controller가 아니라
 * 관리 자동구성이 만든다 → @SpringBootTest + @AutoConfigureMockMvc.
 *
 * 보안 관점의 핵심은 마지막 테스트: "노출 제한이 지켜지는가"를 404로 봉인한다.
 * env에는 DB 접속 정보가, beans에는 내부 구조가 통째로 들어있다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Actuator 엔드포인트")
class ActuatorEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("health: 전체 UP + 컴포넌트별 상세 (db, uploadDir, diskSpace)")
    void health_전체와컴포넌트_UP() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))           // Boot 자동 등록
                .andExpect(jsonPath("$.components.uploadDir.status").value("UP"));   // 우리가 만든 것!
    }

    @Test
    @DisplayName("metrics: 지표 목록이 열려 있다 (jvm.memory.used 등)")
    void metrics_지표목록_노출() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").value(hasItem("jvm.memory.used")));
    }

    @Test
    @DisplayName("닫힌 곳: env/beans는 404 — 노출 제한의 봉인 (DB 비밀번호가 새는 문)")
    void 비노출엔드포인트_404() throws Exception {
        // include: health,info,metrics 에 없는 것들은 존재 자체가 없어야 한다
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/configprops")).andExpect(status().isNotFound());
    }
}
