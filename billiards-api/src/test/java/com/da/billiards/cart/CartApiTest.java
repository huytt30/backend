package com.da.billiards.cart;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.da.billiards.ApiTestSupport;
import com.da.billiards.catalog.Category;
import com.da.billiards.catalog.Product;

class CartApiTest extends ApiTestSupport {

    private Long variantId;

    @BeforeEach
    void setUp() {
        Category category = data.category("Phấn", null);
        Product product = data.product("Phấn xanh", category, null, 100_000, 90_000L, 5);
        variantId = data.firstVariantId(product);
    }

    @Test
    void cartRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/cart/items").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("variantId", variantId, "quantity", 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addingSameVariantTwiceMergesQuantity() throws Exception {
        String token = registerCustomer("gop@test.local");
        add(token, 2);
        add(token, 1)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].unitPrice").value(90_000))
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.subtotal").value(270_000));
    }

    @Test
    void quantityCannotExceedStock() throws Exception {
        String token = registerCustomer("vuot@test.local");
        add(token, 5).andExpect(status().isOk());
        add(token, 1).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("chỉ còn 5")));
        long itemId = itemId(token);
        mockMvc.perform(patch("/api/v1/cart/items/" + itemId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("quantity", 6))))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/cart/items/" + itemId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("quantity", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndRemoveOwnItem() throws Exception {
        String token = registerCustomer("sua@test.local");
        add(token, 1);
        long itemId = itemId(token);
        mockMvc.perform(patch("/api/v1/cart/items/" + itemId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("quantity", 4))))
                .andExpect(jsonPath("$.items[0].quantity").value(4));
        mockMvc.perform(delete("/api/v1/cart/items/" + itemId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void cannotTouchAnotherUsersCartItem() throws Exception {
        String owner = registerCustomer("chu.gio@test.local");
        String other = registerCustomer("ke.gian@test.local");
        add(owner, 1);
        long itemId = itemId(owner);
        mockMvc.perform(patch("/api/v1/cart/items/" + itemId).header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("quantity", 2))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/cart/items/" + itemId).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void unknownVariantReturns404() throws Exception {
        String token = registerCustomer("ma@test.local");
        mockMvc.perform(post("/api/v1/cart/items").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("variantId", 999999, "quantity", 1))))
                .andExpect(status().isNotFound());
    }

    private ResultActions add(String token, int quantity) throws Exception {
        return mockMvc.perform(post("/api/v1/cart/items").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("variantId", variantId, "quantity", quantity))));
    }

    private long itemId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getContentAsString();
        return readJson(body).get("items").get(0).get("id").asLong();
    }
}
