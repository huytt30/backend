package com.da.billiards.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.da.billiards.ApiTestSupport;

class ProfileApiTest extends ApiTestSupport {

    @Test
    void updateProfile() throws Exception {
        String token = registerCustomer("ho.so@test.local");
        mockMvc.perform(put("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Tên Mới", "phone", "+84912345678",
                                "address", "  99 Nguyễn Trãi  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Tên Mới"))
                .andExpect(jsonPath("$.address").value("99 Nguyễn Trãi"))
                .andExpect(jsonPath("$.email").value("ho.so@test.local"));
        mockMvc.perform(put("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "", "phone", "abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        String token = registerCustomer("doi.mk@test.local");
        changePassword(token, "sai-mat-khau", "matkhaumoi").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu hiện tại không đúng"));
        changePassword(token, "secret123", "123").andExpect(status().isBadRequest());
        changePassword(token, "secret123", "matkhaumoi").andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "doi.mk@test.local", "password", "secret123"))))
                .andExpect(status().isUnauthorized());
        login("doi.mk@test.local", "matkhaumoi");
    }

    @Test
    void loginEmailIsCaseInsensitive() throws Exception {
        registerCustomer("Hoa.Thuong@Test.Local");
        login("hoa.thuong@test.local", "secret123");
        login("  HOA.THUONG@TEST.LOCAL ", "secret123");
    }

    private ResultActions changePassword(String token, String current, String next)
            throws Exception {
        return mockMvc.perform(put("/api/v1/auth/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", current, "newPassword", next))));
    }
}
