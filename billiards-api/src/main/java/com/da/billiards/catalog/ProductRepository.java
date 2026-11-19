package com.da.billiards.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    Optional<Product> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByBrandId(Long brandId);

    long countByActiveTrue();

    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findTop8ByCategoryIdAndActiveTrueAndIdNotOrderByCreatedAtDesc(Long categoryId, Long excludedId);
}
