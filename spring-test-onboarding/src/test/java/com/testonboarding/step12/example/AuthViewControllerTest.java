package com.testonboarding.step12.example;

import com.testonboarding.common.exception.DuplicateUsernameException;
import com.testonboarding.config.SecurityConfig;
import com.testonboarding.member.dto.MemberSignupRequest;
import com.testonboarding.member.service.MemberService;
import com.testonboarding.web.AuthViewController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * [Step 12 — example B] 인증 화면 테스트: 로그인 페이지 + 가입 폼의 두 갈래 에러
 *
 * 가입 폼의 에러는 두 종류로 나뉘어 표시된다:
 * - 형식 검증(@Valid) 실패 → "필드" 에러 (해당 입력칸 밑에)
 * - 비즈니스 검증(중복 아이디 등) 실패 → "글로벌" 에러 (폼 상단에)
 * REST에서는 각각 400/409 JSON이었던 것이, 화면에서는 폼 재표시로 번역된다.
 */
@WebMvcTest(AuthViewController.class)
@Import(SecurityConfig.class)
@DisplayName("인증 화면 컨트롤러")
class AuthViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("로그인 페이지가 폼과 함께 렌더링된다 (permitAll)")
    void loginPage_비로그인_200과폼렌더링() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                // 폼의 action과 Security 기본 파라미터명이 렌더링됐는지까지 확인
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")));
    }

    @Test
    @DisplayName("로그인 실패로 돌아오면(?error) 안내 문구가 표시된다")
    void loginPage_error파라미터_실패안내렌더링() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("올바르지 않습니다")));
    }

    @Test
    @DisplayName("가입 폼 제출 성공 → 로그인 페이지로 redirect")
    void signup_정상제출_로그인으로redirect() throws Exception {
        // given
        given(memberService.signup(any(MemberSignupRequest.class))).willReturn(5L);

        // when & then
        mockMvc.perform(post("/signup")
                        .param("username", "newbie")
                        .param("password", "spring123!")
                        .param("nickname", "새내기")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?signup"));
    }

    @Test
    @DisplayName("형식 검증 실패(짧은 아이디) → 필드 에러와 함께 폼 재표시")
    void signup_형식위반_필드에러와폼재표시() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("username", "ab")          // @Size(min=4) 위반
                        .param("password", "spring123!")
                        .param("nickname", "새내기")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().attributeHasFieldErrors("memberSignupRequest", "username"));
    }

    @Test
    @DisplayName("비즈니스 검증 실패(중복 아이디) → 글로벌 에러와 함께 폼 재표시")
    void signup_중복아이디_글로벌에러와폼재표시() throws Exception {
        // given : REST라면 advice가 409로 번역했을 예외 — 화면에서는 폼 안내로
        given(memberService.signup(any(MemberSignupRequest.class)))
                .willThrow(new DuplicateUsernameException("newbie"));

        // when & then
        mockMvc.perform(post("/signup")
                        .param("username", "newbie")
                        .param("password", "spring123!")
                        .param("nickname", "새내기")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"))
                .andExpect(model().hasErrors())
                .andExpect(content().string(containsString("이미 사용 중인 아이디입니다")));
    }
}
