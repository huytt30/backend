package com.da.billiards.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

import com.da.billiards.ApiTestSupport;

class UploadApiTest extends ApiTestSupport {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n', 0, 0, 0, 0};

    @Test
    void adminUploadsPngAndItIsServed() throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(new MockMultipartFile("file", "anh.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("/uploads/")))
                .andReturn().getResponse().getContentAsString();
        String url = readJson(body).get("url").asText();
        assertThat(url).endsWith(".png");
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    void fakeImageIsRejectedEvenWithImageExtension() throws Exception {
        byte[] svg = "<svg onload=alert(1)></svg>".getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(new MockMultipartFile("file", "anh.png", "image/png", svg))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(new MockMultipartFile("file", "rong.png", "image/png", new byte[0]))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerCannotUpload() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/uploads")
                        .file(new MockMultipartFile("file", "anh.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, bearer(registerCustomer("up@test.local"))))
                .andExpect(status().isForbidden());
    }
}
