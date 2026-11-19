package com.da.billiards.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 60)
    private String sku;

    /** Giá niêm yết (VNĐ). */
    @Column(nullable = false)
    private long price;

    /** Giá khuyến mãi, null nếu không giảm giá. */
    private Long salePrice;

    @Column(nullable = false)
    private int stock;

    /** Biến thể bị gỡ khỏi sản phẩm sẽ chuyển sang false thay vì xoá, để giữ liên kết với đơn hàng cũ. */
    @Column(nullable = false)
    private boolean active = true;

    public long effectivePrice() {
        return salePrice != null ? salePrice : price;
    }

    public boolean isPurchasable() {
        return active && product.isActive();
    }
}
