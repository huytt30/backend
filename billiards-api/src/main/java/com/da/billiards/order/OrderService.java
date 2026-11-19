package com.da.billiards.order;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.cart.CartItem;
import com.da.billiards.cart.CartItemRepository;
import com.da.billiards.catalog.Product;
import com.da.billiards.catalog.ProductVariant;
import com.da.billiards.catalog.ProductVariantRepository;
import com.da.billiards.common.ApiException;
import com.da.billiards.common.PageResponse;
import com.da.billiards.common.Pages;
import com.da.billiards.order.OrderDtos.CheckoutRequest;
import com.da.billiards.order.OrderDtos.OrderResponse;
import com.da.billiards.order.OrderDtos.OrderSummary;
import com.da.billiards.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    static final long SHIPPING_FEE = 30_000;
    static final long FREE_SHIPPING_THRESHOLD = 2_000_000;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final SecureRandom random = new SecureRandom();
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    /**
     * Tạo đơn từ giỏ hàng hiện tại. Giá luôn lấy từ dữ liệu sản phẩm trong DB, không tin giá từ client.
     * Nếu một sản phẩm không đủ hàng thì cả giao dịch bị rollback, tồn kho các sản phẩm khác không bị trừ.
     */
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithProduct(userId);
        if (cartItems.isEmpty()) {
            throw ApiException.badRequest("Giỏ hàng đang trống");
        }

        Order order = new Order();
        order.setCode(generateCode());
        order.setUser(userRepository.getReferenceById(userId));
        order.setRecipientName(request.recipientName().trim());
        order.setPhone(request.phone().trim());
        order.setAddress(request.address().trim());
        order.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        order.setPaymentMethod(request.paymentMethod());

        long subtotal = 0;
        for (CartItem cartItem : cartItems) {
            ProductVariant variant = cartItem.getVariant();
            Product product = variant.getProduct();
            String displayName = product.getName() + " - " + variant.getName();
            if (!variant.isPurchasable()) {
                throw ApiException.conflict("Sản phẩm \"" + displayName + "\" đã ngừng bán, vui lòng xoá khỏi giỏ");
            }
            if (variantRepository.decreaseStock(variant.getId(), cartItem.getQuantity()) == 0) {
                throw ApiException.conflict("Sản phẩm \"" + displayName + "\" không đủ số lượng trong kho");
            }

            OrderItem item = new OrderItem();
            item.setVariant(variant);
            item.setProductName(product.getName());
            item.setProductSlug(product.getSlug());
            item.setVariantName(variant.getName());
            item.setThumbnail(product.getThumbnail());
            item.setUnitPrice(variant.effectivePrice());
            item.setQuantity(cartItem.getQuantity());
            item.setLineTotal(variant.effectivePrice() * cartItem.getQuantity());
            order.addItem(item);
            subtotal += item.getLineTotal();
        }

        long shippingFee = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
        order.setSubtotal(subtotal);
        order.setShippingFee(shippingFee);
        order.setTotal(subtotal + shippingFee);
        orderRepository.save(order);
        cartItemRepository.deleteAllByUserId(userId);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> myOrders(Long userId, int page, int size) {
        return PageResponse.of(orderRepository.findByUserId(userId, Pages.of(page, size, NEWEST_FIRST)),
                OrderSummary::forCustomer);
    }

    @Transactional(readOnly = true)
    public OrderResponse myOrder(Long userId, String code) {
        return OrderResponse.from(findOwnOrder(userId, code));
    }

    public OrderResponse cancelByCustomer(Long userId, String code) {
        Order order = findOwnOrder(userId, code);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw ApiException.conflict("Chỉ hủy được đơn hàng đang chờ xác nhận");
        }
        changeStatus(order, OrderStatus.CANCELLED);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> searchOrders(OrderStatus status, String q, int page, int size) {
        String keyword = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(orderRepository.search(status, keyword, Pages.of(page, size, NEWEST_FIRST)),
                OrderSummary::forAdmin);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return OrderResponse.from(findOrder(id));
    }

    public OrderResponse updateStatus(Long id, OrderStatus target) {
        Order order = findOrder(id);
        changeStatus(order, target);
        return OrderResponse.from(order);
    }

    private void changeStatus(Order order, OrderStatus target) {
        if (!order.getStatus().canMoveTo(target)) {
            throw ApiException.conflict("Không thể chuyển đơn từ \"" + order.getStatus().label()
                    + "\" sang \"" + target.label() + "\"");
        }
        if (target == OrderStatus.CANCELLED) {
            order.getItems().stream()
                    .filter(item -> item.getVariant() != null)
                    .forEach(item -> variantRepository.increaseStock(item.getVariant().getId(), item.getQuantity()));
        }
        order.setStatus(target);
    }

    private Order findOwnOrder(Long userId, String code) {
        return orderRepository.findByCodeAndUserId(code, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
    }

    private Order findOrder(Long id) {
        return orderRepository.findWithItemsById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
    }

    private String generateCode() {
        String prefix = "HT" + LocalDate.now().format(CODE_DATE);
        String code;
        do {
            StringBuilder builder = new StringBuilder(prefix);
            for (int i = 0; i < 5; i++) {
                builder.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
            code = builder.toString();
        } while (orderRepository.existsByCode(code));
        return code;
    }
}
