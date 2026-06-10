package com.testonboarding.common.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 요청 추적 필터 — 모든 요청에 X-Request-Id를 부여한다.
 *
 * 동작:
 * 1. 요청 헤더에 X-Request-Id가 있으면 그대로 사용, 없으면 새로 생성
 * 2. MDC에 넣어 이 요청의 "모든" 로그 라인에 requestId가 찍히게 한다
 * 3. 응답 헤더에도 실어 클라이언트가 문의 시 ID를 알려줄 수 있게 한다
 * 4. 처리 시간(ms)을 로깅한다
 *
 * OncePerRequestFilter: 같은 요청에 필터가 두 번 타는 것(forward 등)을 방지해주는 베이스 클래스.
 * ⚠️ finally에서 MDC를 반드시 정리해야 한다 — 서블릿 스레드는 풀에서 재사용되므로
 *    정리하지 않으면 다음 요청에 이전 요청의 ID가 묻어나온다!
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startMillis = System.currentTimeMillis();
        log.info(">>>>> [Filter] 요청 시작: {} {} (requestId={})",
                request.getMethod(), request.getRequestURI(), requestId);
        try {
            filterChain.doFilter(request, response); // 다음 필터(또는 서블릿)로 진행
        } finally {
            long elapsed = System.currentTimeMillis() - startMillis;
            log.info(">>>>> [Filter] 요청 종료: {} {} - {} ({}ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsed);
            MDC.remove(MDC_KEY); // 스레드 재사용 대비 필수 정리!
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String headerValue = request.getHeader(REQUEST_ID_HEADER);
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return UUID.randomUUID().toString().substring(0, 8);
        }
        return headerValue;
    }
}
