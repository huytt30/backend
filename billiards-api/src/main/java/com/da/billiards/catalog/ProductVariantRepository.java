package com.da.billiards.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /**
     * Trừ tồn kho trong một câu UPDATE có điều kiện, nên hai đơn đặt cùng lúc không thể bán quá số lượng còn lại.
     *
     * @return 1 nếu trừ thành công, 0 nếu không đủ hàng
     */
    @Modifying
    @Query("update ProductVariant v set v.stock = v.stock - :quantity where v.id = :id and v.stock >= :quantity")
    int decreaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("update ProductVariant v set v.stock = v.stock + :quantity where v.id = :id")
    int increaseStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Query("""
            select v from ProductVariant v join fetch v.product p
            where v.active = true and p.active = true and v.stock <= :threshold
            order by v.stock asc, p.name asc
            """)
    List<ProductVariant> findLowStock(@Param("threshold") int threshold);
}
