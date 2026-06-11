package com.webflow.step01.example;

import com.webflow.common.exception.OutOfStockException;
import com.webflow.common.exception.ProductNotFoundException;
import com.webflow.order.dao.OrderDao;
import com.webflow.order.domain.Order;
import com.webflow.order.dto.OrderCreateRequest;
import com.webflow.order.service.OrderService;
import com.webflow.product.dao.ProductDao;
import com.webflow.product.domain.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * [Web Step 1 — example] 주문 서비스 단위 테스트 — TestCraft 복습이자 WebFlow의 출발선
 *
 * TestCraft Step 2에서 배운 그대로: 협력 객체(DAO 2개)는 Mock, 판단 로직만 검증.
 * WebFlow에서 새로 등장한 비즈니스 패턴 하나 —
 * "재고 확인+차감을 원자적 UPDATE 한 방에"(decreaseStock, affected 0 = 부족).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 서비스 (Mockito 단위)")
class OrderServiceTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private ProductDao productDao;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("정상 주문: 총액 = 단가 × 수량, 상태는 PENDING_PAYMENT로 저장된다")
    void placeOrder_정상주문_총액과상태() {
        // given
        given(productDao.findById(1L)).willReturn(
                Product.builder().productId(1L).name("기계식 키보드 RED").price(89000).stock(10).build());
        given(productDao.decreaseStock(1L, 2)).willReturn(1); // 차감 성공
        willAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setOrderId(100L); // useGeneratedKeys 흉내 (TestCraft Step 2의 기법)
            return null;
        }).given(orderDao).insert(any(Order.class));
        given(orderDao.findById(100L)).willReturn(
                Order.builder().orderId(100L).productId(1L).quantity(2)
                        .totalPrice(178000).status(Order.STATUS_PENDING_PAYMENT).build());

        // when
        orderService.placeOrder(new OrderCreateRequest(1L, 2));

        // then : INSERT된 주문의 내용물을 Captor로 검증 — 총액 계산과 초기 상태
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderDao).insert(captor.capture());

        Order saved = captor.getValue();
        assertThat(saved.getTotalPrice()).isEqualTo(89000L * 2);
        assertThat(saved.getStatus()).isEqualTo(Order.STATUS_PENDING_PAYMENT);
    }

    @Test
    @DisplayName("없는 상품 주문 → 404 예외 + 차감/INSERT 시도 없음")
    void placeOrder_없는상품_예외및부수효과없음() {
        // given
        given(productDao.findById(99L)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(new OrderCreateRequest(99L, 1)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");

        // 예외만으론 부족 — DB 변경이 "시도조차" 안 됐음을 증명 (TestCraft never의 철학)
        then(productDao).should(never()).decreaseStock(any(), org.mockito.ArgumentMatchers.anyInt());
        then(orderDao).should(never()).insert(any(Order.class));
    }

    @Test
    @DisplayName("재고 부족(차감 affected=0) → 409 예외 + 주문 INSERT 없음")
    void placeOrder_재고부족_예외및주문없음() {
        // given : 상품은 있지만 원자적 차감이 실패하는 상황 (stock < 수량)
        given(productDao.findById(5L)).willReturn(
                Product.builder().productId(5L).name("게이밍 키보드").price(120000).stock(0).build());
        given(productDao.decreaseStock(5L, 1)).willReturn(0); // ★ affected 0 = 부족

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(new OrderCreateRequest(5L, 1)))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("5");

        then(orderDao).should(never()).insert(any(Order.class));
    }
}
