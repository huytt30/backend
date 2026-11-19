package com.da.billiards.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.da.billiards.catalog.CatalogDtos.BrandResponse;
import com.da.billiards.catalog.CatalogDtos.CategoryResponse;
import com.da.billiards.catalog.CatalogDtos.ProductDetail;
import com.da.billiards.catalog.CatalogDtos.ProductSummary;
import com.da.billiards.catalog.CatalogService.ProductFilter;
import com.da.billiards.common.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalogService.categoryTree();
    }

    @GetMapping("/brands")
    public List<BrandResponse> brands() {
        return catalogService.brands();
    }

    @GetMapping("/products")
    public PageResponse<ProductSummary> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalogService.searchProducts(
                new ProductFilter(q, category, brand, minPrice, maxPrice, featured, sort, page, size));
    }

    @GetMapping("/products/{slug}")
    public ProductDetail product(@PathVariable String slug) {
        return catalogService.productDetail(slug);
    }

    @GetMapping("/products/{slug}/related")
    public List<ProductSummary> related(@PathVariable String slug) {
        return catalogService.relatedProducts(slug);
    }
}
