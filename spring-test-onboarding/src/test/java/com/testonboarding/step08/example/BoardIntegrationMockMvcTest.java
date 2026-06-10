package com.testonboarding.step08.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testonboarding.board.dto.PostCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 8 — example B] 중간 지대: @SpringBootTest + MockMvc
 *
 * RANDOM_PORT E2E(example A)와의 차이:
 * - 진짜 서버(Tomcat)를 띄우지 않고, 전체 컨텍스트 + MockMvc로 요청을 넣는다
 * - 모든 빈이 진짜다 (@MockBean 없음!) — Service도 DAO도 H2도 실제로 동작
 * - 같은 스레드에서 돌므로 ✅ @Transactional 롤백이 동작한다 → 데이터 정리가 공짜!
 * - 진짜 HTTP/세션/쿠키는 아니므로 @WithMockUser 같은 도구도 그대로 쓸 수 있다
 *
 * 선택 기준:
 * - "빈들이 진짜로 연결된 상태의 전체 흐름"  → 이 방식 (빠르고 편함)
 * - "진짜 HTTP + 진짜 로그인 + 쿠키까지"     → example A (소수 정예)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional // MockMvc 방식이라 롤백이 동작한다 — RANDOM_PORT와의 결정적 차이!
@DisplayName("게시판 통합 (@SpringBootTest + MockMvc)")
class BoardIntegrationMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "writer1")
    @DisplayName("글 작성 후 즉시 조회 — Controller→Service→DAO→H2 전 구간이 진짜로 연결")
    void 글작성후조회_전레이어연결확인() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(
                new PostCreateRequest("통합 테스트 글", "Mock이 하나도 없다"));

        // when : 작성 — 실제 BoardService → BoardDao → H2 INSERT가 일어난다
        MvcResult createResult = mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();
        String location = createResult.getResponse().getHeader("Location");

        // then : 방금 만든 리소스를 실제로 조회 — SELECT 매핑까지 왕복 검증.
        //        슬라이스에서는 Mock이 답하던 자리를, 여기서는 진짜 DB가 답한다
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("통합 테스트 글"))
                .andExpect(jsonPath("$.writer").value("writer1"));

        // 테스트가 끝나면 @Transactional이 INSERT를 롤백한다 — 정리 코드가 필요 없다!
    }
}
