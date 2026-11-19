package com.da.billiards.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.da.billiards.common.PageResponse;
import com.da.billiards.order.OrderDtos.OrderResponse;
import com.da.billiards.order.OrderDtos.OrderSummary;
import com.da.billiards.order.OrderDtos.UpdateStatusRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public PageResponse<OrderSummary> orders(@RequestParam(required = false) OrderStatus status,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return orderService.searchOrders(status, q, page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse order(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }
}
