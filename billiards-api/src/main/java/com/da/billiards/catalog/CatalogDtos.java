package com.da.billiards.catalog;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryResponse(Long id, String name, String slug, String description, String imageUrl,
                                   Long parentId, int sortOrder, long productCount,
                                   List<CategoryResponse> children) {
    }

    public record BrandResponse(Long id, String name, String slug, String logoUrl, String description) {

        static BrandResponse from(Brand brand) {
            return new BrandResponse(brand.getId(), brand.getName(), brand.getSlug(), brand.getLogoUrl(),
                    brand.getDescription());
        }
    }

    public record RefResponse(Long id, String name, String slug) {
    }

    public record SpecDto(
            @NotBlank(message = "Tên thông số không được trống") @Size(max = 100) String name,
            @NotBlank(message = "Giá trị thông số không được trống") @Size(max = 255) String value) {
    }

    public record VariantResponse(Long id, String name, String sku, long price, Long salePrice, int stock) {

        static VariantResponse from(ProductVariant variant) {
            return new VariantResponse(variant.getId(), variant.getName(), variant.getSku(), variant.getPrice(),
                    variant.getSalePrice(), variant.getStock());
        }
    }

    public record ProductSummary(Long id, String name, String slug, String thumbnail, long price, Long salePrice,
                                 String categoryName, String brandName, boolean inStock, boolean featured) {

        static ProductSummary from(Product product) {
            return new ProductSummary(product.getId(), product.getName(), product.getSlug(), product.getThumbnail(),
                    product.getPrice(), product.getSalePrice(), product.getCategory().getName(),
                    product.getBrand() == null ? null : product.getBrand().getName(),
                    product.getTotalStock() > 0, product.isFeatured());
        }
    }

    public record ProductDetail(Long id, String name, String slug, String sku, String shortDescription,
                                String description, long price, Long salePrice, String thumbnail,
                                List<String> images, List<SpecDto> specs, RefResponse category, RefResponse brand,
                                List<VariantResponse> variants, boolean featured, boolean active,
                                LocalDateTime createdAt) {

        static ProductDetail from(Product product) {
            Category category = product.getCategory();
            Brand brand = product.getBrand();
            return new ProductDetail(product.getId(), product.getName(), product.getSlug(), product.getSku(),
                    product.getShortDescription(), product.getDescription(), product.getPrice(),
                    product.getSalePrice(), product.getThumbnail(), List.copyOf(product.getImages()),
                    product.getSpecs().stream().map(spec -> new SpecDto(spec.getName(), spec.getValue())).toList(),
                    new RefResponse(category.getId(), category.getName(), category.getSlug()),
                    brand == null ? null : new RefResponse(brand.getId(), brand.getName(), brand.getSlug()),
                    product.getActiveVariants().stream().map(VariantResponse::from).toList(),
                    product.isFeatured(), product.isActive(), product.getCreatedAt());
        }
    }

    public record AdminProductRow(Long id, String name, String slug, String thumbnail, String categoryName,
                                  String brandName, long price, Long salePrice, int totalStock, boolean active,
                                  boolean featured) {

        static AdminProductRow from(Product product) {
            return new AdminProductRow(product.getId(), product.getName(), product.getSlug(), product.getThumbnail(),
                    product.getCategory().getName(),
                    product.getBrand() == null ? null : product.getBrand().getName(),
                    product.getPrice(), product.getSalePrice(), product.getTotalStock(), product.isActive(),
                    product.isFeatured());
        }
    }

    public record CategoryRequest(
            @NotBlank(message = "Vui lòng nhập tên danh mục") @Size(max = 100) String name,
            @Size(max = 500) String description,
            @Size(max = 500) String imageUrl,
            Long parentId,
            int sortOrder) {
    }

    public record BrandRequest(
            @NotBlank(message = "Vui lòng nhập tên thương hiệu") @Size(max = 100) String name,
            @Size(max = 500) String logoUrl,
            @Size(max = 1000) String description) {
    }

    public record VariantRequest(
            Long id,
            @NotBlank(message = "Vui lòng nhập tên phiên bản") @Size(max = 100) String name,
            @Size(max = 60) String sku,
            @Positive(message = "Giá phải lớn hơn 0") long price,
            @Positive(message = "Giá khuyến mãi phải lớn hơn 0") Long salePrice,
            @PositiveOrZero(message = "Tồn kho không được âm") int stock) {
    }

    public record ProductRequest(
            @NotBlank(message = "Vui lòng nhập tên sản phẩm") @Size(max = 200) String name,
            @Size(max = 60) String sku,
            @NotNull(message = "Vui lòng chọn danh mục") Long categoryId,
            Long brandId,
            @Size(max = 500) String shortDescription,
            @Size(max = 20000) String description,
            @Size(max = 500) String thumbnail,
            @Size(max = 10, message = "Tối đa 10 ảnh") List<@NotBlank @Size(max = 500) String> images,
            @Size(max = 30) List<@Valid SpecDto> specs,
            boolean featured,
            boolean active,
            @NotEmpty(message = "Sản phẩm cần ít nhất một phiên bản") @Size(max = 30)
            List<@Valid VariantRequest> variants) {
    }
}
