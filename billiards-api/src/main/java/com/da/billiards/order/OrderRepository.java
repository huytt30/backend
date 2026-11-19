package com.da.billiards.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByCode(String code);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "user"})
    Optional<Order> findByCodeAndUserId(String code, Long userId);

    @EntityGraph(attributePaths = {"items", "user"})
    Optional<Order> findWithItemsById(Long id);

    @Query("""
            select o from Order o
            where (:status is null or o.status = :status)
              and (:q is null
                   or lower(o.code) like lower(concat('%', :q, '%'))
                   or lower(o.recipientName) like lower(concat('%', :q, '%'))
                   or o.phone like concat('%', :q, '%'))
            """)
    Page<Order> search(@Param("status") OrderStatus status, @Param("q") String q, Pageable pageable);

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status = :status")
    long sumTotalByStatus(@Param("status") OrderStatus status);

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countGroupByStatus();

    List<Order> findByStatusAndCreatedAtGreaterThanEqual(OrderStatus status, LocalDateTime from);

    @Query("select o.createdAt from Order o where o.createdAt >= :from")
    List<LocalDateTime> findCreatedAtSince(@Param("from") LocalDateTime from);
}
