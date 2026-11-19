package com.da.billiards.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.da.billiards.ApiTestSupport;

class AuthFlowTest extends ApiTestSupport {

    @Test
    void registerAlwaysCreatesCustomerEvenIfClientAsksForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Người Lạ", "email", "la@test.local",
                                "phone", "0987654321", "password", "secret123",
                                "role", "ADMIN", "roles", new String[]{"admin"}))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        registerCustomer("trung@test.local");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "A", "email", "TRUNG@test.local",
                                "phone", "0912345678", "password", "secret123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegisterReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "", "email", "khong-phai-email",
                                "phone", "123", "password", "1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        registerCustomer("sai@test.local");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "sai@test.local", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresValidToken() throws Exception {
        String token = registerCustomer("me@test.local");
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@test.local"));
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token + "x")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpointsNeedAdminRole() throws Exception {
        String customer = registerCustomer("khach@test.local");
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenueByDay.length()").value(7));
    }
}
