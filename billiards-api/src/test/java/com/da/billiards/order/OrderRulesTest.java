package com.da.billiards.order;

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
import com.fasterxml.jackson.databind.JsonNode;

class OrderRulesTest extends ApiTestSupport {

    private Long variantId;

    @BeforeEach
    void setUp() {
        Category category = data.category("Găng tay", null);
        Product product = data.product("Găng tay 3 ngón", category, null, 150_000, null, 20);
        variantId = data.firstVariantId(product);
    }

    @Test
    void emptyCartCannotCheckout() throws Exception {
        String token = registerCustomer("rong@test.local");
        checkout(token, "0912345678").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Giỏ hàng đang trống"));
    }

    @Test
    void checkoutValidatesRecipientInfo() throws Exception {
        String token = registerCustomer("sai.sdt@test.local");
        addToCart(token);
        checkout(token, "12345").andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.phone").exists());
        mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("recipientName", "A", "phone", "0912345678", "address", "HN",
                                "paymentMethod", "BITCOIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerCanOnlyCancelPendingOrders() throws Exception {
        String token = registerCustomer("huy.don@test.local");
        addToCart(token);
        JsonNode order = readJson(checkout(token, "0912345678").andReturn().getResponse().getContentAsString());
        String admin = adminToken();
        mockMvc.perform(patch("/api/v1/admin/orders/" + order.get("id").asLong() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/me/" + order.get("code").asText() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void myOrdersListsOnlyOwnOrdersNewestFirst() throws Exception {
        String token = registerCustomer("lich.su@test.local");
        addToCart(token);
        checkout(token, "0912345678");
        addToCart(token);
        String second = readJson(checkout(token, "0912345678").andReturn().getResponse().getContentAsString())
                .get("code").asText();
        String other = registerCustomer("nguoi.khac@test.local");

        mockMvc.perform(get("/api/v1/orders/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].code").value(second))
                .andExpect(jsonPath("$.content[0].recipientName").doesNotExist());
        mockMvc.perform(get("/api/v1/orders/me").header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void adminSearchesOrdersByStatusAndKeyword() throws Exception {
        String token = registerCustomer("tim.don@test.local");
        addToCart(token);
        String code = readJson(checkout(token, "0987000111").andReturn().getResponse().getContentAsString())
                .get("code").asText();
        String admin = adminToken();

        mockMvc.perform(get("/api/v1/admin/orders").param("status", "PENDING").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].phone").value("0987000111"));
        mockMvc.perform(get("/api/v1/admin/orders").param("status", "SHIPPING").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/orders").param("q", code.substring(2, 8).toLowerCase())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/admin/orders").param("q", "0987000").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/admin/orders").param("status", "KHONG_CO").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    private void addToCart(String token) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("variantId", variantId, "quantity", 1))))
                .andExpect(status().isOk());
    }

    private ResultActions checkout(String token, String phone) throws Exception {
        return mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("recipientName", "Lê C", "phone", phone, "address", "5 Hai Bà Trưng",
                        "paymentMethod", "COD"))));
    }
}
