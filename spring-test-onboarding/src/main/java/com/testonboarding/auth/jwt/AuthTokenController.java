package com.testonboarding.auth.jwt;

import com.testonboarding.auth.jwt.dto.TokenRequest;
import com.testonboarding.auth.jwt.dto.TokenResponse;
import com.testonboarding.member.dao.MemberDao;
import com.testonboarding.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 토큰 발급/확인 API — /api/v2/** (stateless JWT 체인).
 *
 * 세션을 못 쓰는 클라이언트(모바일 앱, 외부 시스템)의 인증 흐름:
 * 1. POST /api/v2/auth/token 에 아이디/비밀번호 → JWT 발급
 * 2. 이후 모든 요청에 Authorization: Bearer {token}
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthTokenController {

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/api/v2/auth/token")
    public ResponseEntity<TokenResponse> issueToken(@Valid @RequestBody TokenRequest request) {
        Member member = memberDao.findByUsername(request.getUsername());

        if (member == null || !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            log.warn(">>>>> [WARN] 토큰 발급 거부: username={}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtTokenProvider.createToken(member.getUsername(), member.getRole().name());
        log.info(">>>>> [AuthToken] 토큰 발급: username={}", member.getUsername());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    /**
     * 토큰 인증 확인용 — JwtAuthenticationFilter가 심어준 인증 정보를 그대로 보여준다.
     */
    @GetMapping("/api/v2/me")
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        result.put("username", authentication.getName());
        result.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return result;
    }
}
