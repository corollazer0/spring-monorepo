package com.testonboarding.step07.example;

import com.testonboarding.common.filter.RequestLoggingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 7 — example A] 서블릿 필터의 "단위 테스트"
 *
 * 문제 상황: 필터는 모든 요청의 길목에 서 있다. 로직이 틀리면 전 요청이 영향을 받는다.
 * 그런데 MockMvc로는 "필터를 통과한 결과"만 보이지, 필터 자체의 분기
 * (헤더가 있을 때/없을 때, 예외가 났을 때 정리되는지)를 따로 검증하기 어렵다.
 *
 * 해결: Spring이 제공하는 서블릿 Mock 3총사로 필터를 "손에 들고" 테스트한다.
 * - MockHttpServletRequest  : 가짜 요청 (헤더/URI/메서드를 마음대로 조작)
 * - MockHttpServletResponse : 가짜 응답 (필터가 쓴 헤더/상태를 꺼내 검증)
 * - MockFilterChain         : 가짜 체인 ("다음 단계로 넘겼는지"를 기억한다)
 *
 * Spring 컨테이너는 전혀 띄우지 않는다 — 필터도 결국 new 할 수 있는 클래스다.
 */
@DisplayName("요청 추적 필터 (단위 테스트)")
class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        request = new MockHttpServletRequest("GET", "/api/posts");
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("요청에 ID가 없으면 새로 생성해 응답 헤더에 싣고, 체인을 이어간다")
    void doFilter_요청ID없음_새ID생성및체인진행() throws Exception {
        // given
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(request, response, chain);

        // then(1) : 응답 헤더에 새 ID가 실렸다
        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNotBlank();

        // then(2) : 체인이 이어졌다 — MockFilterChain은 자기에게 도달한 요청을 기억한다.
        //           필터가 chain.doFilter를 빠뜨리면(= 모든 요청이 무응답이 되는 대형사고!) 여기서 잡힌다
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("요청에 이미 ID가 있으면 새로 만들지 않고 그대로 사용한다")
    void doFilter_요청ID있음_기존ID유지() throws Exception {
        // given : 게이트웨이/앞단 서버가 이미 ID를 발급한 상황
        request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "gw-abc-123");

        // when
        filter.doFilter(request, response, new MockFilterChain());

        // then : 추적 ID가 구간마다 바뀌면 추적이 끊긴다 — 보존이 핵심
        assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER))
                .isEqualTo("gw-abc-123");
    }

    @Test
    @DisplayName("처리 중에는 MDC에 ID가 있고, 처리가 끝나면 반드시 비워진다")
    void doFilter_처리종료후_MDC정리() throws Exception {
        // given : 체인 안(= Controller가 실행될 시점)에서 MDC 값을 엿보는 가짜 체인
        AtomicReference<String> mdcInsideChain = new AtomicReference<>();
        FilterChain spyingChain = (req, res) ->
                mdcInsideChain.set(MDC.get(RequestLoggingFilter.MDC_KEY));

        // when
        filter.doFilter(request, response, spyingChain);

        // then(1) : 요청 처리 중에는 MDC에 ID가 있었다 → 모든 로그에 ID가 찍힌다
        assertThat(mdcInsideChain.get()).isNotBlank();

        // then(2) : 처리 후에는 비워졌다 — 서블릿 스레드는 재사용되므로
        //           안 비우면 다음 요청 로그에 남의 ID가 묻는다 (실무 단골 사고!)
        assertThat(MDC.get(RequestLoggingFilter.MDC_KEY)).isNull();
    }
}
