package com.webflow.step07.exercise;

import com.webflow.order.dao.OrderDao;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

/**
 * [Web Step 7 — exercise] cutoff 경계를 봉인해보세요 (@MybatisTest)
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (시드: 미결제 주문의 ordered_at = 2026-06-01 09:00 / 10:00)
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 *
 * 질문: WHERE ordered_at < cutoff — 경계의 "정확히 그 시각" 주문은 포함인가?
 * 이런 한 글자(< vs <=) 차이가 "딱 30분째 주문"의 운명을 가른다.
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step07.md 참고 후 @Disabled를 제거하고 완성하세요")
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("정리 대상 경계 (연습문제)")
class StaleBoundaryExerciseTest {

    @Autowired
    private OrderDao orderDao;

    @Test
    @DisplayName("cutoff가 정확히 09:00이면 09:00 주문은 대상이 아니다 (<는 경계 미포함)")
    void findStaleOrders_경계시각_미포함() {
        // when & then : cutoff = 2026-06-01T09:00 으로 조회하면 0건인지 검증하세요
        //               (09:00 주문도, 10:00 주문도 "< 09:00"이 아니다!)
        // TODO 1
    }

    @Test
    @DisplayName("cutoff가 09:00:01이면 09:00 주문만 대상이 된다")
    void findStaleOrders_경계직후_1건() {
        // when & then : cutoff = 2026-06-01T09:00:01 로 조회하면 정확히 1건이고,
        //               그 주문의 ordered_at이 09:00인지 검증하세요
        // TODO 2
    }

    @Test
    @DisplayName("PAID/CANCELLED는 아무리 오래돼도 대상이 아니다")
    void findStaleOrders_상태필터_검증() {
        // when & then : cutoff를 아주 미래(2027-01-01)로 줘도 결과는 미결제 3건뿐 —
        //               PAID 2건과 CANCELLED 1건이 끼어들지 않는지 검증하세요
        //               (힌트: result의 status를 allSatisfy로)
        // TODO 3
    }
}
