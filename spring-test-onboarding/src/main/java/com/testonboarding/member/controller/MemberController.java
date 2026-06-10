package com.testonboarding.member.controller;

import com.testonboarding.member.dto.MemberResponse;
import com.testonboarding.member.dto.MemberSignupRequest;
import com.testonboarding.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.security.Principal;

/**
 * 회원 REST API — Step 4 연습문제(exercise) 대상.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입 — SecurityConfig에서 permitAll (비회원이 쓰는 기능이므로)
     */
    @PostMapping
    public ResponseEntity<Void> signup(@Valid @RequestBody MemberSignupRequest request) {
        Long memberId = memberService.signup(request);
        return ResponseEntity.created(URI.create("/api/members/" + memberId)).build();
    }

    /**
     * 내 정보 조회 — 로그인 상태 확인용 (Step 6, Step 8에서 활용)
     */
    @GetMapping("/me")
    public MemberResponse me(Principal principal) {
        return MemberResponse.from(memberService.getByUsername(principal.getName()));
    }
}
