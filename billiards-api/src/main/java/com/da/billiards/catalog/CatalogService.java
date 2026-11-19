package com.da.billiards.catalog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.catalog.CatalogDtos.BrandResponse;
import com.da.billiards.catalog.CatalogDtos.CategoryResponse;
import com.da.billiards.catalog.CatalogDtos.ProductDetail;
import com.da.billiards.catalog.CatalogDtos.ProductSummary;
import com.da.billiards.common.ApiException;
import com.da.billiards.common.PageResponse;
import com.da.billiards.common.Pages;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public List<CategoryResponse> categoryTree() {
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAscNameAsc();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : categoryRepository.countActiveProductsByCategory()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        Map<Long, List<Category>> childrenByParent = groupByParent(categories);
        return childrenByParent.getOrDefault(null, List.of()).stream()
                .map(root -> toTreeNode(root, childrenByParent, counts))
                .toList();
    }

    public List<BrandResponse> brands() {
        return brandRepository.findAllByOrderByNameAsc().stream().map(BrandResponse::from).toList();
    }

    public PageResponse<ProductSummary> searchProducts(ProductFilter filter) {
        Specification<Product> spec = ProductSpecifications.activeOnly()
                .and(ProductSpecifications.nameContains(filter.q()))
                .and(ProductSpecifications.priceBetween(filter.minPrice(), filter.maxPrice()))
                .and(ProductSpecifications.featured(filter.featured()))
                .and(ProductSpecifications.brandSlugIn(parseList(filter.brand())));
        if (filter.category() != null && !filter.category().isBlank()) {
            Category category = categoryRepository.findBySlug(filter.category())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục"));
            spec = spec.and(ProductSpecifications.inCategories(descendantIds(category.getId())));
        }
        Page<Product> page = productRepository.findAll(spec, Pages.of(filter.page(), filter.size(), sortOf(filter.sort())));
        return PageResponse.of(page, ProductSummary::from);
    }

    public ProductDetail productDetail(String slug) {
        return ProductDetail.from(findActiveProduct(slug));
    }

    public List<ProductSummary> relatedProducts(String slug) {
        Product product = findActiveProduct(slug);
        return productRepository
                .findTop8ByCategoryIdAndActiveTrueAndIdNotOrderByCreatedAtDesc(product.getCategory().getId(),
                        product.getId())
                .stream().map(ProductSummary::from).toList();
    }

    /**
     * Lấy id của danh mục cùng toàn bộ danh mục con cháu, để lọc "Cơ bida" ra cả "Cơ lỗ", "Cơ phá"...
     */
    Set<Long> descendantIds(Long rootId) {
        Map<Long, List<Category>> childrenByParent = groupByParent(categoryRepository.findAll());
        Set<Long> result = new HashSet<>();
        List<Long> stack = new ArrayList<>(List.of(rootId));
        while (!stack.isEmpty()) {
            Long current = stack.remove(stack.size() - 1);
            if (result.add(current)) {
                childrenByParent.getOrDefault(current, List.of()).forEach(child -> stack.add(child.getId()));
            }
        }
        return result;
    }

    private Product findActiveProduct(String slug) {
        return productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
    }

    private static Map<Long, List<Category>> groupByParent(List<Category> categories) {
        Map<Long, List<Category>> result = new HashMap<>();
        for (Category category : categories) {
            Long parentId = category.getParent() == null ? null : category.getParent().getId();
            result.computeIfAbsent(parentId, key -> new ArrayList<>()).add(category);
        }
        return result;
    }

    private static CategoryResponse toTreeNode(Category category, Map<Long, List<Category>> childrenByParent,
                                               Map<Long, Long> counts) {
        List<CategoryResponse> children = childrenByParent.getOrDefault(category.getId(), List.of()).stream()
                .map(child -> toTreeNode(child, childrenByParent, counts))
                .toList();
        long total = counts.getOrDefault(category.getId(), 0L)
                + children.stream().mapToLong(CategoryResponse::productCount).sum();
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), category.getImageUrl(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getSortOrder(), total, children);
    }

    private static Sort sortOf(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Order.asc("effectivePrice"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("effectivePrice"), Sort.Order.asc("id"));
            case "name" -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"));
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }

    private static List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public record ProductFilter(String q, String category, String brand, Long minPrice, Long maxPrice,
                                Boolean featured, String sort, int page, int size) {
    }
}
