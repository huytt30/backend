package com.da.billiards.cart;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.da.billiards.cart.CartDtos.AddItemRequest;
import com.da.billiards.cart.CartDtos.CartResponse;
import com.da.billiards.cart.CartDtos.UpdateItemRequest;
import com.da.billiards.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse cart(@AuthenticationPrincipal AuthUser user) {
        return cartService.getCart(user.id());
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody AddItemRequest request) {
        return cartService.addItem(user.id(), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateItem(@AuthenticationPrincipal AuthUser user, @PathVariable Long itemId,
                                   @Valid @RequestBody UpdateItemRequest request) {
        return cartService.updateItem(user.id(), itemId, request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@AuthenticationPrincipal AuthUser user, @PathVariable Long itemId) {
        return cartService.removeItem(user.id(), itemId);
    }
}
