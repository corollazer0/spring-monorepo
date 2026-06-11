package com.webflow.product.dao;

import com.webflow.product.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 상품 DAO — 구현은 resources/mybatis/mapper/ProductMapper.xml
 */
@Mapper
public interface ProductDao {

    Product findById(Long productId);

    void insert(Product product);

    /**
     * 재고 차감 — WHERE stock >= 수량 조건으로 "확인과 차감"을 한 문장에:
     * 두 요청이 동시에 와도 재고가 음수가 되지 않는다 (동시성 안전).
     *
     * @return 영향받은 행 수 — 0이면 재고 부족(또는 없는 상품)으로 차감 실패!
     */
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
