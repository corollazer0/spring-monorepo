package com.testonboarding.step04.exercise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testonboarding.config.SecurityConfig;
import com.testonboarding.member.controller.MemberController;
import com.testonboarding.member.service.MemberService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [Step 4 — exercise] 회원가입 API 테스트를 직접 작성해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 BoardControllerTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 검증 대상: POST /api/members (permitAll — 로그인 없이 가능)
 * - 정상 가입 → 201 Created + Location 헤더 "/api/members/{새 ID}"
 *
 * 힌트:
 * - 요청 본문: objectMapper.writeValueAsString(new MemberSignupRequest(...))
 * - POST는 .with(csrf()) 필요! 다 완성한 뒤 csrf()를 빼고 돌려보세요 — 어떤 상태코드가 나오나요?
 * - Location 헤더 검증: .andExpect(header().string("Location", "..."))
 *   (import static ...MockMvcResultMatchers.header)
 */
@Disabled("과제: docs/test/education/FOR-Test-Step04.md 참고 후 @Disabled를 제거하고 완성하세요")
@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
@DisplayName("회원 컨트롤러 (연습문제)")
class MemberControllerExerciseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;

    @Test
    @DisplayName("정상 회원가입 요청이면 201과 Location 헤더를 반환한다")
    void signup_정상요청_201과Location헤더() throws Exception {
        // given :
        // TODO 1: memberService.signup(...)이 5L을 돌려주도록 stubbing 하세요
        //         (힌트: 파라미터 매처 any(MemberSignupRequest.class) 사용)
        // TODO 2: MemberSignupRequest("newbie", "spring123!", "새내기")를 JSON 문자열로 만드세요

        // when & then :
        // TODO 3: POST /api/members 를 수행하세요 — contentType, content, with(csrf()) 잊지 말기!
        // TODO 4: 상태코드가 201 Created인지 검증하세요
        // TODO 5: Location 헤더가 "/api/members/5"인지 검증하세요
    }

    @Test
    @DisplayName("회원가입은 로그인 없이 가능하다 (permitAll 확인)")
    void signup_미인증요청_401이아니다() throws Exception {
        // 이 테스트의 목적: SecurityConfig의 permitAll 설정이 깨지면 알아채는 것.
        // TODO 6: 위와 같은 요청을 보내되, 상태코드가 401이 "아님"을 검증하세요
        //         (힌트: status().isUnauthorized()가 아니라... example에 비슷한 게 없으니 직접 찾아보세요!
        //          MockMvcResultMatchers에는 is(int)도 있고, 그냥 isCreated()를 다시 써도 됩니다)
    }
}
