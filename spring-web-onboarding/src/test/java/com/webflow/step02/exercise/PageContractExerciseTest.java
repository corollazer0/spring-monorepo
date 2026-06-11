package com.webflow.step02.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * [Web Step 2 — exercise] 페이지 계산과 검색 일관성을 검증해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (PageResponse는 순수 클래스 — Spring 불필요!)
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step02.md 참고 후 @Disabled를 제거하고 완성하세요")
@DisplayName("페이지 규약 (연습문제)")
class PageContractExerciseTest {

    @Test
    @DisplayName("totalPages는 올림 계산이다: 11건/5 = 3페이지")
    void of_11건5씩_3페이지() {
        // given & when : PageResponse.of(빈 리스트, 1, 5, 11)을 만드세요
        // TODO 1

        // then : totalPages가 3인지 검증하세요 (11/5=2.2 → 올림 3 — 내림이면 마지막 1건 증발!)
        // TODO 2
    }

    @Test
    @DisplayName("딱 나누어떨어지면 올림 보정이 초과 페이지를 만들지 않는다: 10건/5 = 2페이지")
    void of_10건5씩_2페이지() {
        // when & then : (10 + 5 - 1) / 5 = 2 — 경계에서 3이 되지 않는지 검증하세요
        // TODO 3
    }

    @Test
    @DisplayName("0건이면 0페이지 (1페이지가 아니다!)")
    void of_0건_0페이지() {
        // when & then : totalCount 0일 때 totalPages가 0인지 검증하세요
        // TODO 4
    }
}
