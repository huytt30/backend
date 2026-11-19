package com.da.billiards.catalog;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.da.billiards.catalog.CatalogDtos.AdminProductRow;
import com.da.billiards.catalog.CatalogDtos.BrandRequest;
import com.da.billiards.catalog.CatalogDtos.BrandResponse;
import com.da.billiards.catalog.CatalogDtos.CategoryRequest;
import com.da.billiards.catalog.CatalogDtos.CategoryResponse;
import com.da.billiards.catalog.CatalogDtos.ProductDetail;
import com.da.billiards.catalog.CatalogDtos.ProductRequest;
import com.da.billiards.common.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;

    @GetMapping("/products")
    public PageResponse<AdminProductRow> products(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) String category,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return adminCatalogService.listProducts(q, category, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductDetail product(@PathVariable Long id) {
        return adminCatalogService.getProduct(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetail createProduct(@Valid @RequestBody ProductRequest request) {
        return adminCatalogService.createProduct(request);
    }

    @PutMapping("/products/{id}")
    public ProductDetail updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return adminCatalogService.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hideProduct(@PathVariable Long id) {
        adminCatalogService.hideProduct(id);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return adminCatalogService.listCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return adminCatalogService.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return adminCatalogService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        adminCatalogService.deleteCategory(id);
    }

    @GetMapping("/brands")
    public List<BrandResponse> brands() {
        return adminCatalogService.listBrands();
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse createBrand(@Valid @RequestBody BrandRequest request) {
        return adminCatalogService.createBrand(request);
    }

    @PutMapping("/brands/{id}")
    public BrandResponse updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return adminCatalogService.updateBrand(id, request);
    }

    @DeleteMapping("/brands/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(@PathVariable Long id) {
        adminCatalogService.deleteBrand(id);
    }
}
