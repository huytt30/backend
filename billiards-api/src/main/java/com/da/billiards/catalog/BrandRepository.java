package com.da.billiards.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    boolean existsBySlug(String slug);

    List<Brand> findAllByOrderByNameAsc();
}
