package com.da.billiards.order;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.da.billiards.common.PageResponse;
import com.da.billiards.order.OrderDtos.CheckoutRequest;
import com.da.billiards.order.OrderDtos.OrderResponse;
import com.da.billiards.order.OrderDtos.OrderSummary;
import com.da.billiards.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(user.id(), request);
    }

    @GetMapping("/me")
    public PageResponse<OrderSummary> myOrders(@AuthenticationPrincipal AuthUser user,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return orderService.myOrders(user.id(), page, size);
    }

    @GetMapping("/me/{code}")
    public OrderResponse myOrder(@AuthenticationPrincipal AuthUser user, @PathVariable String code) {
        return orderService.myOrder(user.id(), code);
    }

    @PostMapping("/me/{code}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal AuthUser user, @PathVariable String code) {
        return orderService.cancelByCustomer(user.id(), code);
    }
}
