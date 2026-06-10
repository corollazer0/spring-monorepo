package com.testonboarding.advanced.step11.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testonboarding.auth.LoginMember;
import com.testonboarding.board.controller.BoardController;
import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.config.SecurityConfig;
import com.testonboarding.member.domain.Role;
import com.testonboarding.support.security.WithMockMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [심화 Step 11 — example A] 커스텀 @WithMockMember
 *
 * @WithMockUser의 한계: principal이 Security 기본 User라서
 * 우리 도메인 정보(memberId, nickname)가 비어있다.
 * @WithMockMember는 LoginMember(도메인 principal)를 통째로 심는다.
 *
 * 구현은 support/security/ 두 파일 — 합쳐서 50줄이 안 된다.
 * 프로젝트의 인증 모델이 커질수록(부서, 권한 목록...) 이 어노테이션의 가치가 커진다.
 */
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
@DisplayName("커스텀 @WithMockMember")
class WithMockMemberTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BoardService boardService;

    @Test
    @WithMockMember(username = "writer2", nickname = "글쓴이이호", memberId = 2L)
    @DisplayName("principal이 우리 도메인 타입(LoginMember)으로 심긴다")
    void withMockMember_도메인principal_주입확인() {
        // when : 테스트 실행 전에 팩토리가 심어둔 SecurityContext를 직접 들여다본다
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // then : @WithMockUser였다면 불가능한 검증 — 도메인 필드가 살아있다
        assertThat(authentication.getPrincipal()).isInstanceOf(LoginMember.class);
        LoginMember principal = (LoginMember) authentication.getPrincipal();
        assertThat(principal.getUsername()).isEqualTo("writer2");
        assertThat(principal.getNickname()).isEqualTo("글쓴이이호");
        assertThat(principal.getMemberId()).isEqualTo(2L);
    }

    @Test
    @WithMockMember(username = "writer2")
    @DisplayName("MockMvc 요청에서도 그대로 동작한다 (Principal → Service 전달)")
    void withMockMember_글작성_username전달() throws Exception {
        // given
        given(boardService.createPost(eq("writer2"), any(PostCreateRequest.class))).willReturn(10L);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PostCreateRequest("제목", "내용")))
                        .with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockMember(username = "admin", role = Role.ADMIN)
    @DisplayName("role 속성으로 ADMIN 인증도 한 줄")
    void withMockMember_관리자_권한확인() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }
}
