package com.da.billiards.cart;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("""
            select c from CartItem c join fetch c.variant v join fetch v.product
            where c.user.id = :userId order by c.createdAt asc, c.id asc
            """)
    List<CartItem> findByUserIdWithProduct(@Param("userId") Long userId);

    Optional<CartItem> findByUserIdAndVariantId(Long userId, Long variantId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("delete from CartItem c where c.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
