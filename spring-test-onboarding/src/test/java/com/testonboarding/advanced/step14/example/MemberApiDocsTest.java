package com.testonboarding.advanced.step14.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testonboarding.config.SecurityConfig;
import com.testonboarding.member.controller.MemberController;
import com.testonboarding.member.dto.MemberSignupRequest;
import com.testonboarding.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [심화 Step 14 — example] Spring REST Docs — 테스트가 문서를 만든다
 *
 * 원리: 평소의 @WebMvcTest에 .andDo(document(...))를 더하면,
 * "테스트가 통과할 때만" 그 요청/응답으로 문서 조각(스니펫)이 생성된다.
 * 문서가 코드와 어긋날 방법이 없다 — 어긋나면 테스트가 빨가니까.
 *
 * 그리고 document는 기록기이자 "검증기"다: requestFields에 적은 필드 목록과
 * 실제 페이로드가 다르면(누락/잉여) 테스트가 실패한다 — API 계약의 봉인.
 */
@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs   // MockMvc에 문서화 설정 장착 + 스니펫 출력 build/generated-snippets
@DisplayName("회원 API 문서화")
class MemberApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 문서화: 테스트가 통과하면 스니펫 파일이 실제로 생긴다")
    void signup_문서화_스니펫생성() throws Exception {
        // given
        given(memberService.signup(any())).willReturn(1L);
        MemberSignupRequest request = new MemberSignupRequest("newbie01", "spring123!", "새내기");

        // when : 평소의 @WebMvcTest + andDo(document) 한 단계 — 이게 전부다
        mockMvc.perform(post("/api/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("member-signup",
                        requestFields(
                                fieldWithPath("username").description("아이디 (영문 소문자+숫자, 4~20자)"),
                                fieldWithPath("password").description("비밀번호 (정책: 8자 이상 + 특수문자)"),
                                fieldWithPath("nickname").description("닉네임 (2~10자)")),
                        responseHeaders(
                                headerWithName("Location").description("생성된 회원 리소스 URI"))));

        // then : 문서가 "정말로" 생겼다 — 손으로 만질 수 있는 산출물
        Path snippetDir = Paths.get("build", "generated-snippets", "member-signup");
        assertThat(Files.exists(snippetDir.resolve("http-request.adoc"))).isTrue();
        assertThat(Files.exists(snippetDir.resolve("request-fields.adoc"))).isTrue();
        assertThat(Files.exists(snippetDir.resolve("curl-request.adoc"))).isTrue();
    }

    @Test
    @DisplayName("계약 검증: 필드 문서화를 빠뜨리면 테스트가 실패한다 — 썩은 문서가 불가능한 이유")
    void signup_필드누락_문서화실패() throws Exception {
        // given
        given(memberService.signup(any())).willReturn(1L);
        MemberSignupRequest request = new MemberSignupRequest("newbie01", "spring123!", "새내기");

        // when & then : nickname을 문서에서 빠뜨렸다 — REST Docs가 그냥 넘어가지 않는다!
        //               (위키였다면 조용히 어긋난 채 몇 달을 갔을 누락이다)
        assertThatThrownBy(() -> mockMvc.perform(post("/api/members").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("member-signup-incomplete",
                        requestFields(
                                fieldWithPath("username").description("아이디"),
                                fieldWithPath("password").description("비밀번호")))))
                .hasMessageContaining("nickname");   // 누락된 필드 이름을 정확히 알려준다
    }
}
