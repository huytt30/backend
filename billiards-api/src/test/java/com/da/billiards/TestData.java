package com.da.billiards;

import org.springframework.stereotype.Component;

import com.da.billiards.catalog.Brand;
import com.da.billiards.catalog.BrandRepository;
import com.da.billiards.catalog.Category;
import com.da.billiards.catalog.CategoryRepository;
import com.da.billiards.catalog.Product;
import com.da.billiards.catalog.ProductRepository;
import com.da.billiards.catalog.ProductVariant;
import com.da.billiards.common.SlugUtil;

/**
 * Tạo nhanh danh mục, thương hiệu, sản phẩm trực tiếp qua repository cho các test.
 */
@Component
public class TestData {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public TestData(CategoryRepository categoryRepository, BrandRepository brandRepository,
                    ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    public Category category(String name, Category parent) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(SlugUtil.unique(name, categoryRepository::existsBySlug));
        category.setParent(parent);
        return categoryRepository.save(category);
    }

    public Brand brand(String name) {
        Brand brand = new Brand();
        brand.setName(name);
        brand.setSlug(SlugUtil.unique(name, brandRepository::existsBySlug));
        return brandRepository.save(brand);
    }

    public Product product(String name, Category category, Brand brand, long price, Long salePrice, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(SlugUtil.unique(name, productRepository::existsBySlug));
        product.setCategory(category);
        product.setBrand(brand);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName("Tiêu chuẩn");
        variant.setPrice(price);
        variant.setSalePrice(salePrice);
        variant.setStock(stock);
        product.getVariants().add(variant);
        product.refreshPricing();
        return productRepository.save(product);
    }

    public Long firstVariantId(Product product) {
        return product.getVariants().get(0).getId();
    }
}
