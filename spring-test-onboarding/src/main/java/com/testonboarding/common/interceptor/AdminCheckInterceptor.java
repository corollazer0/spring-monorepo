package com.testonboarding.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 관리자 구역(/api/admin/**) 인터셉터 — WebConfig에서 경로를 매핑한다.
 *
 * 역할:
 * 1. 관리자 접근 감사(AUDIT) 로그 — 누가 언제 어떤 관리 기능에 접근했는지 기록
 * 2. 심층 방어(defense in depth) — SecurityConfig의 hasRole이 실수로 풀려도
 *    여기서 한 번 더 차단한다 (보안은 한 겹으로 충분하지 않다)
 *
 * preHandle이 false를 반환하면 요청은 Controller에 도달하지 못한다.
 */
@Slf4j
@Component
public class AdminCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isAdmin(authentication)) {
            log.info(">>>>> [AUDIT] 관리자 접근: user={}, uri={}",
                    authentication.getName(), request.getRequestURI());
            return true; // 통과 — Controller로 진행
        }

        log.warn(">>>>> [WARN] 관리자 구역 차단: uri={}", request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":403,\"message\":\"관리자만 접근할 수 있습니다\"}");
        return false; // 차단 — Controller에 도달하지 않는다
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
