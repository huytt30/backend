package com.da.billiards.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.da.billiards.ApiTestSupport;
import com.da.billiards.catalog.Category;
import com.da.billiards.catalog.Product;

class AdminApiTest extends ApiTestSupport {

    @Test
    void searchUsersByNameEmailOrPhone() throws Exception {
        registerCustomer("tim.kiem@test.local");
        String admin = adminToken();
        mockMvc.perform(get("/api/v1/admin/users").param("q", "tim.kiem").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].role").value("CUSTOMER"));
        mockMvc.perform(get("/api/v1/admin/users").param("q", "0912345678").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void lockedUserCannotLoginAndOldTokenStopsWorking() throws Exception {
        String customer = registerCustomer("khoa@test.local");
        String admin = adminToken();
        long userId = readJson(mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        setEnabled(admin, userId, false).andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "khoa@test.local", "password", "secret123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khoá"));

        setEnabled(admin, userId, true).andExpect(status().isOk());
        login("khoa@test.local", "secret123");
    }

    @Test
    void adminCannotLockThemself() throws Exception {
        String admin = adminToken();
        long adminId = readJson(mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
        setEnabled(admin, adminId, false).andExpect(status().isBadRequest());
    }

    @Test
    void dashboardReflectsCompletedOrders() throws Exception {
        Category category = data.category("Cơ lỗ", null);
        Product product = data.product("Cơ thống kê", category, null, 500_000, null, 10);
        data.product("Phấn sắp hết", category, null, 50_000, null, 2);
        String customer = registerCustomer("thongke@test.local");
        mockMvc.perform(post("/api/v1/cart/items").header(HttpHeaders.AUTHORIZATION, bearer(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("variantId", data.firstVariantId(product), "quantity", 2))));
        long orderId = readJson(mockMvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("recipientName", "A", "phone", "0912345678", "address", "HN",
                                "paymentMethod", "COD"))))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        String admin = adminToken();
        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.pendingOrders").value(1))
                .andExpect(jsonPath("$.ordersToday").value(1))
                .andExpect(jsonPath("$.totalRevenue").value(0))
                .andExpect(jsonPath("$.totalCustomers").value(1))
                .andExpect(jsonPath("$.totalProducts").value(2))
                .andExpect(jsonPath("$.lowStock[0].productName").value("Phấn sắp hết"))
                .andExpect(jsonPath("$.lowStock[0].thumbnail").hasJsonPath())
                .andExpect(jsonPath("$.revenueByDay[6].orders").value(1));

        for (String status : new String[]{"CONFIRMED", "SHIPPING", "COMPLETED"}) {
            mockMvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status")
                    .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                    .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", status))))
                    .andExpect(status().isOk());
        }

        long expected = 1_000_000 + 30_000;
        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalRevenue").value(expected))
                .andExpect(jsonPath("$.pendingOrders").value(0))
                .andExpect(jsonPath("$.ordersByStatus.COMPLETED").value(1))
                .andExpect(jsonPath("$.revenueByDay[6].date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.revenueByDay[6].revenue").value(expected))
                .andExpect(jsonPath("$.revenueByDay[6].orders").value(1))
                .andExpect(jsonPath("$.revenueByDay[5].orders").value(0));
    }

    private ResultActions setEnabled(String admin, long userId, boolean enabled)
            throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/enabled")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("enabled", enabled))));
    }
}
