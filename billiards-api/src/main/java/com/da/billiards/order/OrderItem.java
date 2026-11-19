package com.da.billiards.order;

import com.da.billiards.catalog.ProductVariant;

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

/**
 * Lưu lại tên và giá tại thời điểm đặt hàng, để sau này sửa sản phẩm không làm thay đổi đơn cũ.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = false, length = 200)
    private String productName;

    @Column(length = 220)
    private String productSlug;

    @Column(nullable = false, length = 100)
    private String variantName;

    @Column(length = 500)
    private String thumbnail;

    @Column(nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long lineTotal;
}
