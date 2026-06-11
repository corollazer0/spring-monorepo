package com.webflow.product.dao;

import com.webflow.product.domain.Product;
import com.webflow.product.dto.ProductSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 상품 DAO — 구현은 resources/mybatis/mapper/ProductMapper.xml
 */
@Mapper
public interface ProductDao {

    Product findById(Long productId);

    void insert(Product product);

    /** 동적 검색 + 화이트리스트 정렬 + MS-SQL 페이징 (Step 2) */
    List<Product> search(@Param("condition") ProductSearchCondition condition,
                         @Param("offset") int offset,
                         @Param("size") int size);

    /** search와 같은 WHERE의 전체 건수 — PageResponse의 totalCount */
    long countBySearch(@Param("condition") ProductSearchCondition condition);

    /**
     * 재고 차감 — WHERE stock >= 수량 조건으로 "확인과 차감"을 한 문장에:
     * 두 요청이 동시에 와도 재고가 음수가 되지 않는다 (동시성 안전).
     *
     * @return 영향받은 행 수 — 0이면 재고 부족(또는 없는 상품)으로 차감 실패!
     */
    int decreaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    /** 이미지 경로 갱신 (Step 5) */
    int updateImagePath(@Param("productId") Long productId, @Param("imagePath") String imagePath);
}
