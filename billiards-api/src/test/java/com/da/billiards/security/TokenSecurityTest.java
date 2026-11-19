package com.da.billiards.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;

import com.da.billiards.ApiTestSupport;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class TokenSecurityTest extends ApiTestSupport {

    @Value("${app.jwt.secret}")
    private String secret;

    @Test
    void expiredTokenIsRejected() throws Exception {
        String token = registerCustomer("het.han@test.local");
        String userId = readJson(mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String expired = Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenSignedWithOtherKeyIsRejected() throws Exception {
        String forged = Jwts.builder()
                .subject("1")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor("another-secret-key-that-is-long-enough-123".getBytes(StandardCharsets.UTF_8)))
                .compact();
        mockMvc.perform(get("/api/v1/admin/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedHeadersAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46YWRtaW4="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicCatalogNeedsNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/articles")).andExpect(status().isOk());
    }
}
