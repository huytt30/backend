package com.da.billiards.catalog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 60)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String thumbnail;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "url", nullable = false, length = 500)
    private List<String> images = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_specs", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "sort_order")
    private List<ProductSpec> specs = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<ProductVariant> variants = new ArrayList<>();

    /*
     * Ba cột giá dưới đây được tính lại từ biến thể rẻ nhất mỗi khi lưu sản phẩm,
     * để lọc và sắp xếp theo giá ngay trong câu truy vấn.
     */
    @Column(nullable = false)
    private long price;

    private Long salePrice;

    @Column(nullable = false)
    private long effectivePrice;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public List<ProductVariant> getActiveVariants() {
        return variants.stream().filter(ProductVariant::isActive).toList();
    }

    public int getTotalStock() {
        return getActiveVariants().stream().mapToInt(ProductVariant::getStock).sum();
    }

    public void refreshPricing() {
        getActiveVariants().stream()
                .min(Comparator.comparingLong(ProductVariant::effectivePrice))
                .ifPresent(cheapest -> {
                    price = cheapest.getPrice();
                    salePrice = cheapest.getSalePrice();
                    effectivePrice = cheapest.effectivePrice();
                });
    }
}
