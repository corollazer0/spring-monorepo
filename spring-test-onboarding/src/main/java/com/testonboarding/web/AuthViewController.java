package com.testonboarding.web;

import com.testonboarding.common.exception.DuplicateUsernameException;
import com.testonboarding.member.dto.MemberSignupRequest;
import com.testonboarding.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;

/**
 * 인증 관련 화면(SSR) — 로그인 페이지 + 회원가입 폼.
 *
 * 주의: POST /login 은 여기 없다! — Spring Security의 필터가 처리한다(SecurityConfig).
 * 컨트롤러는 "화면을 보여주는 일"만 담당한다.
 */
@Controller
@RequiredArgsConstructor
public class AuthViewController {

    private final MemberService memberService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("memberSignupRequest", new MemberSignupRequest());
        return "auth/signup";
    }

    /**
     * 가입 폼 제출.
     * - 형식 검증 실패(@Valid) → 필드 에러와 함께 폼 재표시
     * - 비즈니스 검증 실패(중복 아이디, 비밀번호 정책) → 글로벌 에러로 폼 재표시
     *   (REST에서는 advice가 409/400 JSON으로 번역했지만, 화면에서는 사람이 읽을 폼 안내가 필요하다)
     */
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute MemberSignupRequest memberSignupRequest,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            memberService.signup(memberSignupRequest);
        } catch (DuplicateUsernameException | IllegalArgumentException e) {
            bindingResult.reject("signupFailed", e.getMessage());
            return "auth/signup";
        }

        return "redirect:/login?signup"; // 가입 완료 → 로그인 화면으로 (환영 메시지 표시)
    }
}
