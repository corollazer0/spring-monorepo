package com.testonboarding.step04.answer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testonboarding.config.SecurityConfig;
import com.testonboarding.member.controller.MemberController;
import com.testonboarding.member.dto.MemberSignupRequest;
import com.testonboarding.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 4 — answer] MemberControllerExerciseTest 모범답안
 *
 * 채점 포인트:
 * - POST에 contentType + content + with(csrf()) 3종 세트를 갖췄는가
 * - 201뿐 아니라 Location 헤더(생성된 리소스의 주소)까지 검증했는가
 * - permitAll 검증의 의미를 이해했는가 (보안 설정 회귀 방지)
 */
@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
@DisplayName("회원 컨트롤러 (모범답안)")
class MemberControllerAnswerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("정상 회원가입 요청이면 201과 Location 헤더를 반환한다")
    void signup_정상요청_201과Location헤더() throws Exception {
        // given (TODO 1~2 답)
        given(memberService.signup(any(MemberSignupRequest.class))).willReturn(5L);
        String body = objectMapper.writeValueAsString(
                new MemberSignupRequest("newbie", "spring123!", "새내기"));

        // when & then (TODO 3~5 답)
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/members/5"));
    }

    @Test
    @DisplayName("회원가입은 로그인 없이 가능하다 (permitAll 확인)")
    void signup_미인증요청_401이아니다() throws Exception {
        // given (TODO 6 답)
        given(memberService.signup(any(MemberSignupRequest.class))).willReturn(5L);
        String body = objectMapper.writeValueAsString(
                new MemberSignupRequest("newbie", "spring123!", "새내기"));

        // when & then : 로그인 정보 없이 요청했는데 201이 나온다 = permitAll이 살아있다.
        // 누군가 SecurityConfig에서 이 줄을 지우면 이 테스트가 401로 깨지며 알려준다.
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isCreated());
    }
}
