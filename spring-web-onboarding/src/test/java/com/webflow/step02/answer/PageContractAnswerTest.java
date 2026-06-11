package com.webflow.step02.answer;

import com.webflow.common.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Web Step 2 — answer] PageContractExerciseTest 모범답안
 *
 * 채점 포인트: 올림/정확히 나눠떨어짐/0건 — 계산 로직의 세 경계를 모두 다뤘는가.
 * (totalPages 같은 "산수"는 순수 단위로 봉인한다 — BatchFlow 파티셔너와 같은 철학)
 */
@DisplayName("페이지 규약 (모범답안)")
class PageContractAnswerTest {

    @Test
    @DisplayName("totalPages는 올림 계산이다: 11건/5 = 3페이지")
    void of_11건5씩_3페이지() {
        // given & when (TODO 1 답)
        PageResponse<Object> page = PageResponse.of(Collections.emptyList(), 1, 5, 11);

        // then (TODO 2 답)
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("딱 나누어떨어지면 올림 보정이 초과 페이지를 만들지 않는다: 10건/5 = 2페이지")
    void of_10건5씩_2페이지() {
        // when & then (TODO 3 답)
        assertThat(PageResponse.of(Collections.emptyList(), 1, 5, 10).getTotalPages())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("0건이면 0페이지 (1페이지가 아니다!)")
    void of_0건_0페이지() {
        // when & then (TODO 4 답)
        assertThat(PageResponse.of(Collections.emptyList(), 1, 5, 0).getTotalPages())
                .isZero();
    }
}
