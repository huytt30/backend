package com.da.billiards.article;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.da.billiards.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;

class ArticleApiTest extends ApiTestSupport {

    @Test
    void draftIsHiddenUntilPublished() throws Exception {
        String admin = adminToken();
        JsonNode draft = readJson(mockMvc.perform(post("/api/v1/admin/articles")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Mẹo đánh bi xoáy", "summary", "Tóm tắt",
                                "content", "Đoạn 1\n\nĐoạn 2", "published", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("meo-danh-bi-xoay"))
                .andExpect(jsonPath("$.publishedAt").isEmpty())
                .andReturn().getResponse().getContentAsString());
        long id = draft.get("id").asLong();

        mockMvc.perform(get("/api/v1/articles/meo-danh-bi-xoay")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/articles")).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/articles").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(put("/api/v1/admin/articles/" + id).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Mẹo đánh bi xoáy", "content", "Nội dung", "published", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/articles/meo-danh-bi-xoay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Nội dung"));
        mockMvc.perform(get("/api/v1/articles")).andExpect(jsonPath("$.content[0].title").value("Mẹo đánh bi xoáy"));

        mockMvc.perform(delete("/api/v1/admin/articles/" + id).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/articles/meo-danh-bi-xoay")).andExpect(status().isNotFound());
    }

    @Test
    void titleAndContentAreRequired() throws Exception {
        mockMvc.perform(post("/api/v1/admin/articles").header(HttpHeaders.AUTHORIZATION, bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "", "content", "", "published", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.content").exists());
    }
}
