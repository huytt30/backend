package com.da.billiards.cart;

import java.util.List;

import com.da.billiards.catalog.Product;
import com.da.billiards.catalog.ProductVariant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {

    static final int MAX_QUANTITY = 99;

    private CartDtos() {
    }

    public record AddItemRequest(
            @NotNull(message = "Vui lòng chọn phiên bản sản phẩm") Long variantId,
            @Min(value = 1, message = "Số lượng tối thiểu là 1")
            @Max(value = MAX_QUANTITY, message = "Số lượng tối đa là 99") int quantity) {
    }

    public record UpdateItemRequest(
            @Min(value = 1, message = "Số lượng tối thiểu là 1")
            @Max(value = MAX_QUANTITY, message = "Số lượng tối đa là 99") int quantity) {
    }

    public record CartItemResponse(Long id, Long variantId, Long productId, String productName, String productSlug,
                                   String variantName, String thumbnail, long unitPrice, int quantity,
                                   long lineTotal, int stock) {

        static CartItemResponse from(CartItem item) {
            ProductVariant variant = item.getVariant();
            Product product = variant.getProduct();
            long unitPrice = variant.effectivePrice();
            return new CartItemResponse(item.getId(), variant.getId(), product.getId(), product.getName(),
                    product.getSlug(), variant.getName(), product.getThumbnail(), unitPrice, item.getQuantity(),
                    unitPrice * item.getQuantity(), variant.getStock());
        }
    }

    public record CartResponse(List<CartItemResponse> items, int totalQuantity, long subtotal) {

        static CartResponse of(List<CartItemResponse> items) {
            return new CartResponse(items,
                    items.stream().mapToInt(CartItemResponse::quantity).sum(),
                    items.stream().mapToLong(CartItemResponse::lineTotal).sum());
        }
    }
}
