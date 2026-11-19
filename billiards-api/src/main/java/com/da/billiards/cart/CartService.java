package com.da.billiards.cart;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.cart.CartDtos.AddItemRequest;
import com.da.billiards.cart.CartDtos.CartItemResponse;
import com.da.billiards.cart.CartDtos.CartResponse;
import com.da.billiards.catalog.ProductVariant;
import com.da.billiards.catalog.ProductVariantRepository;
import com.da.billiards.common.ApiException;
import com.da.billiards.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    /**
     * Trả về giỏ hàng, đồng thời bỏ các dòng có sản phẩm đã ngừng bán.
     */
    public CartResponse getCart(Long userId) {
        List<CartItemResponse> items = new ArrayList<>();
        for (CartItem item : cartItemRepository.findByUserIdWithProduct(userId)) {
            if (item.getVariant().isPurchasable()) {
                items.add(CartItemResponse.from(item));
            } else {
                cartItemRepository.delete(item);
            }
        }
        return CartResponse.of(items);
    }

    public CartResponse addItem(Long userId, AddItemRequest request) {
        ProductVariant variant = variantRepository.findById(request.variantId())
                .filter(ProductVariant::isPurchasable)
                .orElseThrow(() -> ApiException.notFound("Sản phẩm không tồn tại hoặc đã ngừng bán"));

        CartItem item = cartItemRepository.findByUserIdAndVariantId(userId, variant.getId()).orElseGet(() -> {
            CartItem created = new CartItem();
            created.setUser(userRepository.getReferenceById(userId));
            created.setVariant(variant);
            return created;
        });
        int newQuantity = item.getQuantity() + request.quantity();
        ensureQuantity(variant, newQuantity);
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        return getCart(userId);
    }

    public CartResponse updateItem(Long userId, Long itemId, int quantity) {
        CartItem item = findItem(userId, itemId);
        ensureQuantity(item.getVariant(), quantity);
        item.setQuantity(quantity);
        return getCart(userId);
    }

    public CartResponse removeItem(Long userId, Long itemId) {
        cartItemRepository.delete(findItem(userId, itemId));
        return getCart(userId);
    }

    private CartItem findItem(Long userId, Long itemId) {
        // Lọc theo cả userId để người này không sửa được giỏ hàng của người khác
        return cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sản phẩm trong giỏ"));
    }

    private static void ensureQuantity(ProductVariant variant, int quantity) {
        if (quantity > CartDtos.MAX_QUANTITY) {
            throw ApiException.badRequest("Số lượng tối đa cho mỗi sản phẩm là " + CartDtos.MAX_QUANTITY);
        }
        if (quantity > variant.getStock()) {
            throw ApiException.conflict("Sản phẩm \"" + variant.getProduct().getName() + " - " + variant.getName()
                    + "\" chỉ còn " + variant.getStock() + " sản phẩm");
        }
    }
}
