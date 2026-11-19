package com.da.billiards.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.catalog.CatalogDtos.AdminProductRow;
import com.da.billiards.catalog.CatalogDtos.BrandRequest;
import com.da.billiards.catalog.CatalogDtos.BrandResponse;
import com.da.billiards.catalog.CatalogDtos.CategoryRequest;
import com.da.billiards.catalog.CatalogDtos.CategoryResponse;
import com.da.billiards.catalog.CatalogDtos.ProductDetail;
import com.da.billiards.catalog.CatalogDtos.ProductRequest;
import com.da.billiards.catalog.CatalogDtos.VariantRequest;
import com.da.billiards.common.ApiException;
import com.da.billiards.common.PageResponse;
import com.da.billiards.common.Pages;
import com.da.billiards.common.SlugUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCatalogService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final CatalogService catalogService;

    // ---------- Sản phẩm ----------

    @Transactional(readOnly = true)
    public PageResponse<AdminProductRow> listProducts(String q, String category, int page, int size) {
        Specification<Product> spec = ProductSpecifications.all().and(ProductSpecifications.nameContains(q));
        if (category != null && !category.isBlank()) {
            Category found = categoryRepository.findBySlug(category)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục"));
            spec = spec.and(ProductSpecifications.inCategories(catalogService.descendantIds(found.getId())));
        }
        var pageable = Pages.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.of(productRepository.findAll(spec, pageable), AdminProductRow::from);
    }

    @Transactional(readOnly = true)
    public ProductDetail getProduct(Long id) {
        return ProductDetail.from(findProduct(id));
    }

    public ProductDetail createProduct(ProductRequest request) {
        Product product = new Product();
        product.setSlug(SlugUtil.unique(request.name(), productRepository::existsBySlug));
        applyProduct(product, request);
        return ProductDetail.from(productRepository.save(product));
    }

    public ProductDetail updateProduct(Long id, ProductRequest request) {
        Product product = findProduct(id);
        if (!product.getName().equals(request.name().trim())) {
            product.setSlug(SlugUtil.unique(request.name(),
                    slug -> productRepository.existsBySlugAndIdNot(slug, id)));
        }
        applyProduct(product, request);
        return ProductDetail.from(product);
    }

    /**
     * Không xoá hẳn vì sản phẩm có thể đã nằm trong đơn hàng; chỉ ẩn khỏi cửa hàng.
     */
    public void hideProduct(Long id) {
        findProduct(id).setActive(false);
    }

    private void applyProduct(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setSku(trimToNull(request.sku()));
        product.setCategory(categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ApiException.badRequest("Danh mục không tồn tại")));
        product.setBrand(request.brandId() == null ? null : brandRepository.findById(request.brandId())
                .orElseThrow(() -> ApiException.badRequest("Thương hiệu không tồn tại")));
        product.setShortDescription(trimToNull(request.shortDescription()));
        product.setDescription(trimToNull(request.description()));
        product.setThumbnail(trimToNull(request.thumbnail()));
        product.getImages().clear();
        if (request.images() != null) {
            product.getImages().addAll(request.images());
        }
        product.getSpecs().clear();
        if (request.specs() != null) {
            request.specs().forEach(spec -> product.getSpecs().add(new ProductSpec(spec.name().trim(), spec.value().trim())));
        }
        product.setFeatured(request.featured());
        product.setActive(request.active());
        applyVariants(product, request.variants());
        product.refreshPricing();
    }

    private void applyVariants(Product product, List<VariantRequest> requests) {
        Map<Long, ProductVariant> existing = new HashMap<>();
        product.getVariants().forEach(variant -> existing.put(variant.getId(), variant));
        List<ProductVariant> kept = new ArrayList<>();

        for (VariantRequest request : requests) {
            if (request.salePrice() != null && request.salePrice() >= request.price()) {
                throw ApiException.badRequest("Giá khuyến mãi của \"" + request.name() + "\" phải nhỏ hơn giá gốc");
            }
            ProductVariant variant;
            if (request.id() != null) {
                variant = existing.get(request.id());
                if (variant == null) {
                    throw ApiException.badRequest("Phiên bản không thuộc sản phẩm này");
                }
            } else {
                variant = new ProductVariant();
                variant.setProduct(product);
                product.getVariants().add(variant);
            }
            variant.setName(request.name().trim());
            variant.setSku(trimToNull(request.sku()));
            variant.setPrice(request.price());
            variant.setSalePrice(request.salePrice());
            variant.setStock(request.stock());
            variant.setActive(true);
            kept.add(variant);
        }
        // Phiên bản bị bỏ khỏi form: ẩn đi, vẫn giữ bản ghi cho các đơn hàng đã đặt
        product.getVariants().stream().filter(variant -> !kept.contains(variant))
                .forEach(variant -> variant.setActive(false));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm"));
    }

    // ---------- Danh mục ----------

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : categoryRepository.countActiveProductsByCategory()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                        category.getDescription(), category.getImageUrl(),
                        category.getParent() == null ? null : category.getParent().getId(),
                        category.getSortOrder(), counts.getOrDefault(category.getId(), 0L), List.of()))
                .toList();
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setSlug(SlugUtil.unique(request.name(), categoryRepository::existsBySlug));
        applyCategory(category, request);
        categoryRepository.save(category);
        return toFlat(category);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        if (request.parentId() != null && catalogService.descendantIds(id).contains(request.parentId())) {
            throw ApiException.badRequest("Không thể chọn danh mục con làm danh mục cha");
        }
        if (!category.getName().equals(request.name().trim())) {
            category.setSlug(SlugUtil.unique(request.name(),
                    slug -> !slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)));
        }
        applyCategory(category, request);
        return toFlat(category);
    }

    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        if (categoryRepository.existsByParentId(id)) {
            throw ApiException.conflict("Danh mục còn danh mục con, hãy xoá hoặc chuyển danh mục con trước");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw ApiException.conflict("Danh mục đang có sản phẩm, không thể xoá");
        }
        categoryRepository.delete(category);
    }

    private void applyCategory(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(trimToNull(request.description()));
        category.setImageUrl(trimToNull(request.imageUrl()));
        category.setSortOrder(request.sortOrder());
        category.setParent(request.parentId() == null ? null : findCategory(request.parentId()));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy danh mục"));
    }

    private static CategoryResponse toFlat(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), category.getImageUrl(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getSortOrder(), 0, List.of());
    }

    // ---------- Thương hiệu ----------

    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands() {
        return catalogService.brands();
    }

    public BrandResponse createBrand(BrandRequest request) {
        Brand brand = new Brand();
        brand.setSlug(SlugUtil.unique(request.name(), brandRepository::existsBySlug));
        applyBrand(brand, request);
        return BrandResponse.from(brandRepository.save(brand));
    }

    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Brand brand = findBrand(id);
        if (!brand.getName().equals(request.name().trim())) {
            brand.setSlug(SlugUtil.unique(request.name(),
                    slug -> !slug.equals(brand.getSlug()) && brandRepository.existsBySlug(slug)));
        }
        applyBrand(brand, request);
        return BrandResponse.from(brand);
    }

    public void deleteBrand(Long id) {
        Brand brand = findBrand(id);
        if (productRepository.existsByBrandId(id)) {
            throw ApiException.conflict("Thương hiệu đang có sản phẩm, không thể xoá");
        }
        brandRepository.delete(brand);
    }

    private void applyBrand(Brand brand, BrandRequest request) {
        brand.setName(request.name().trim());
        brand.setLogoUrl(trimToNull(request.logoUrl()));
        brand.setDescription(trimToNull(request.description()));
    }

    private Brand findBrand(Long id) {
        return brandRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy thương hiệu"));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
