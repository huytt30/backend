package com.da.billiards.catalog;

import java.util.Collection;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

final class ProductSpecifications {

    private ProductSpecifications() {
    }

    static Specification<Product> all() {
        return (root, query, cb) -> cb.conjunction();
    }

    static Specification<Product> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    static Specification<Product> nameContains(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    static Specification<Product> inCategories(Collection<Long> categoryIds) {
        if (categoryIds == null) {
            return null;
        }
        return (root, query, cb) -> root.get("category").get("id").in(categoryIds);
    }

    static Specification<Product> brandSlugIn(Collection<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("brand").get("slug").in(slugs);
    }

    static Specification<Product> priceBetween(Long min, Long max) {
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("effectivePrice"), min, max);
            }
            if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("effectivePrice"), min);
            }
            if (max != null) {
                return cb.lessThanOrEqualTo(root.get("effectivePrice"), max);
            }
            return null;
        };
    }

    static Specification<Product> featured(Boolean featured) {
        if (featured == null || !featured) {
            return null;
        }
        return (root, query, cb) -> cb.isTrue(root.get("featured"));
    }
}
