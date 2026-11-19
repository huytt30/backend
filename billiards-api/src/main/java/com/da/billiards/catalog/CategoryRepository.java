package com.da.billiards.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByParentId(Long parentId);

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    @Query("select p.category.id, count(p) from Product p where p.active = true group by p.category.id")
    List<Object[]> countActiveProductsByCategory();
}
