package com.testonboarding.step05.example;

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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Step 5 — example B] @Valid + GlobalExceptionHandler가 웹 계층에서 실제로 동작하는가
 *
 * example A(순수 Validator)와의 역할 분담:
 * - A: 검증 "규칙"이 맞는가 (케이스를 여기에 많이 쌓는다 — 빠르니까)
 * - B: 규칙 위반 시 "400 + 약속된 에러 JSON"이 나가는가 (대표 케이스 1~2개면 충분)
 *
 * 흐름: 요청 JSON → @Valid 검증 실패 → MethodArgumentNotValidException →
 *       GlobalExceptionHandler → 400 + ErrorResponse(fieldErrors)
 */
@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
@DisplayName("회원가입 검증 (웹 계층 통합)")
class MemberSignupValidationMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("닉네임이 빈 값이면 400과 fieldErrors에 사유가 담긴다")
    void signup_닉네임빈값_400과필드에러() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(
                new MemberSignupRequest("newbie", "spring123!", ""));

        // when & then : 에러 응답의 "형태"가 규약(ErrorResponse)대로인지까지 검증한다
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("입력값 검증에 실패했습니다"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"));
    }

    @Test
    @DisplayName("검증에 실패하면 Service는 호출조차 되지 않는다 (문 앞에서 거른다)")
    void signup_검증실패_서비스호출안됨() throws Exception {
        // given
        String body = objectMapper.writeValueAsString(
                new MemberSignupRequest("newbie", "spring123!", ""));

        // when
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        // then : @Valid가 잘못된 입력을 막아줬으므로 비즈니스 로직은 시작도 안 했다.
        //        이 검증이 있어야 "검증을 우회해 Service까지 흘러가는" 회귀를 잡는다.
        then(memberService).should(never()).signup(any(MemberSignupRequest.class));
    }
}
