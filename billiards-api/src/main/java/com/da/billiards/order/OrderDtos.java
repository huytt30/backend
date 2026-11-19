package com.da.billiards.order;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.da.billiards.user.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CheckoutRequest(
            @NotBlank(message = "Vui lòng nhập tên người nhận") @Size(max = 100) String recipientName,
            @NotBlank(message = "Vui lòng nhập số điện thoại")
            @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ") String phone,
            @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng") @Size(max = 255) String address,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note,
            @NotNull(message = "Vui lòng chọn phương thức thanh toán") PaymentMethod paymentMethod) {
    }

    public record UpdateStatusRequest(@NotNull(message = "Vui lòng chọn trạng thái") OrderStatus status) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderSummary(Long id, String code, OrderStatus status, PaymentMethod paymentMethod, long total,
                               int itemCount, LocalDateTime createdAt, String recipientName, String phone) {

        static OrderSummary forCustomer(Order order) {
            return new OrderSummary(order.getId(), order.getCode(), order.getStatus(), order.getPaymentMethod(),
                    order.getTotal(), itemCount(order), order.getCreatedAt(), null, null);
        }

        static OrderSummary forAdmin(Order order) {
            return new OrderSummary(order.getId(), order.getCode(), order.getStatus(), order.getPaymentMethod(),
                    order.getTotal(), itemCount(order), order.getCreatedAt(), order.getRecipientName(),
                    order.getPhone());
        }

        private static int itemCount(Order order) {
            return order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        }
    }

    public record OrderItemResponse(String productName, String productSlug, String variantName, String thumbnail,
                                    long unitPrice, int quantity, long lineTotal) {

        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getProductName(), item.getProductSlug(), item.getVariantName(),
                    item.getThumbnail(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal());
        }
    }

    public record CustomerRef(Long id, String fullName, String email) {
    }

    public record OrderResponse(Long id, String code, OrderStatus status, PaymentMethod paymentMethod,
                                String recipientName, String phone, String address, String note, long subtotal,
                                long shippingFee, long total, LocalDateTime createdAt, LocalDateTime updatedAt,
                                List<OrderItemResponse> items, CustomerRef customer) {

        static OrderResponse from(Order order) {
            User user = order.getUser();
            return new OrderResponse(order.getId(), order.getCode(), order.getStatus(), order.getPaymentMethod(),
                    order.getRecipientName(), order.getPhone(), order.getAddress(), order.getNote(),
                    order.getSubtotal(), order.getShippingFee(), order.getTotal(), order.getCreatedAt(),
                    order.getUpdatedAt(), order.getItems().stream().map(OrderItemResponse::from).toList(),
                    new CustomerRef(user.getId(), user.getFullName(), user.getEmail()));
        }
    }
}
