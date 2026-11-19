package com.da.billiards.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.da.billiards.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;

class AdminCatalogApiTest extends ApiTestSupport {

    @Autowired
    private CategoryRepository categoryRepository;

    private String admin;
    private Category category;
    private Brand brand;

    @BeforeEach
    void setUp() throws Exception {
        admin = adminToken();
        category = data.category("Cơ lỗ", null);
        brand = data.brand("Falcon");
    }

    @Test
    void createProductWithVariantsAndUniqueSlug() throws Exception {
        String first = createProduct("Cơ Falcon F-20", List.of(variant(null, "19oz", 3_000_000, null, 4),
                variant(null, "20oz", 3_200_000, 2_900_000L, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("co-falcon-f-20"))
                .andExpect(jsonPath("$.variants.length()").value(2))
                // Giá hiển thị lấy theo phiên bản có giá bán thấp nhất
                .andExpect(jsonPath("$.price").value(3_200_000))
                .andExpect(jsonPath("$.salePrice").value(2_900_000))
                .andReturn().getResponse().getContentAsString();

        createProduct("Cơ Falcon F-20", List.of(variant(null, "19oz", 1_000_000, null, 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("co-falcon-f-20-2"));

        mockMvc.perform(get("/api/v1/products/" + readJson(first).get("slug").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void salePriceMustBeLowerThanPrice() throws Exception {
        createProduct("Cơ lỗi giá", List.of(variant(null, "19oz", 1_000_000, 1_000_000L, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Giá khuyến mãi")));
    }

    @Test
    void productRequiresAtLeastOneVariantAndCategory() throws Exception {
        Map<String, Object> body = productBody("Thiếu dữ liệu", List.of());
        body.remove("categoryId");
        mockMvc.perform(post("/api/v1/admin/products").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.variants").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }

    @Test
    void updateKeepsVariantIdsAndHidesRemovedVariant() throws Exception {
        JsonNode created = readJson(createProduct("Cơ hai phiên bản",
                List.of(variant(null, "19oz", 2_000_000, null, 5), variant(null, "20oz", 2_000_000, null, 5)))
                .andReturn().getResponse().getContentAsString());
        long productId = created.get("id").asLong();
        long keptId = created.get("variants").get(0).get("id").asLong();
        long removedId = created.get("variants").get(1).get("id").asLong();

        String customer = registerCustomer("gio@test.local");
        mockMvc.perform(post("/api/v1/cart/items").header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("variantId", removedId, "quantity", 1))))
                .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(put("/api/v1/admin/products/" + productId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productBody("Cơ hai phiên bản",
                                List.of(variant(keptId, "19oz mới", 2_100_000, null, 7),
                                        variant(null, "21oz", 2_300_000, null, 1))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants.length()").value(2))
                .andExpect(jsonPath("$.variants[0].id").value(keptId))
                .andExpect(jsonPath("$.variants[0].name").value("19oz mới"))
                .andExpect(jsonPath("$.variants[1].name").value("21oz"));

        // Phiên bản bị gỡ không còn mua được nên tự rời khỏi giỏ hàng
        mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void variantFromAnotherProductIsRejected() throws Exception {
        Product other = data.product("Sản phẩm khác", category, brand, 1_000_000, null, 1);
        JsonNode created = readJson(createProduct("Cơ A", List.of(variant(null, "19oz", 1_000_000, null, 1)))
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(put("/api/v1/admin/products/" + created.get("id").asLong())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productBody("Cơ A",
                                List.of(variant(data.firstVariantId(other), "lấy trộm", 1, null, 1))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hideProductRemovesItFromShopButKeepsItInAdmin() throws Exception {
        Product product = data.product("Cơ sắp ẩn", category, brand, 1_000_000, null, 1);
        mockMvc.perform(delete("/api/v1/admin/products/" + product.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/products/" + product.getSlug())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/admin/products").param("q", "sắp ẩn")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.content[0].active").value(false));
    }

    @Test
    void adminProductListFiltersByCategoryAndSearch() throws Exception {
        Category tables = data.category("Bàn bida", null);
        data.product("Cơ X", category, brand, 1_000_000, null, 1);
        data.product("Bàn Y", tables, brand, 1_000_000, null, 1);
        mockMvc.perform(get("/api/v1/admin/products").param("category", tables.getSlug())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Bàn Y"));
    }

    @Test
    void categoryCrudAndGuards() throws Exception {
        String parentJson = mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Phụ kiện", "sortOrder", 3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("phu-kien"))
                .andReturn().getResponse().getContentAsString();
        long parentId = readJson(parentJson).get("id").asLong();

        String childJson = mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Phấn", "parentId", parentId, "sortOrder", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(parentId))
                .andReturn().getResponse().getContentAsString();
        long childId = readJson(childJson).get("id").asLong();

        // Chọn con làm cha tạo vòng lặp
        mockMvc.perform(put("/api/v1/admin/categories/" + parentId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Phụ kiện", "parentId", childId, "sortOrder", 3))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/admin/categories/" + parentId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Phụ kiện bida", "sortOrder", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("phu-kien-bida"));

        mockMvc.perform(delete("/api/v1/admin/categories/" + parentId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/admin/categories/" + category.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        data.product("Phấn xanh", categoryRef(childId), brand, 100_000, null, 1);
        mockMvc.perform(delete("/api/v1/admin/categories/" + childId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/admin/categories").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$[?(@.id==" + childId + ")].productCount").value(1));
    }

    @Test
    void brandCrudAndDeleteGuard() throws Exception {
        String created = mockMvc.perform(post("/api/v1/admin/brands").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Kaiser Cue", "description", "Hãng mới"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("kaiser-cue"))
                .andReturn().getResponse().getContentAsString();
        long brandId = readJson(created).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/brands/" + brandId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "Kaiser"))))
                .andExpect(jsonPath("$.slug").value("kaiser"));
        mockMvc.perform(delete("/api/v1/admin/brands/" + brandId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());

        data.product("Cơ có hãng", category, brand, 1_000_000, null, 1);
        mockMvc.perform(delete("/api/v1/admin/brands/" + brand.getId()).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void customerCannotUseAdminCatalog() throws Exception {
        String customer = registerCustomer("tomo@test.local");
        mockMvc.perform(post("/api/v1/admin/brands").header(HttpHeaders.AUTHORIZATION, bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("name", "Hack"))))
                .andExpect(status().isForbidden());
    }

    private Category categoryRef(long id) {
        return categoryRepository.getReferenceById(id);
    }

    private ResultActions createProduct(String name, List<Map<String, Object>> variants) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/products").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content(json(productBody(name, variants))));
    }

    private Map<String, Object> productBody(String name, List<Map<String, Object>> variants) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("categoryId", category.getId());
        body.put("brandId", brand.getId());
        body.put("shortDescription", "Mô tả ngắn");
        body.put("images", List.of());
        body.put("specs", List.of(Map.of("name", "Chiều dài", "value", "147 cm")));
        body.put("featured", false);
        body.put("active", true);
        body.put("variants", variants);
        return body;
    }

    private static Map<String, Object> variant(Long id, String name, long price, Long salePrice, int stock) {
        Map<String, Object> variant = new HashMap<>();
        variant.put("id", id);
        variant.put("name", name);
        variant.put("price", price);
        variant.put("salePrice", salePrice);
        variant.put("stock", stock);
        return variant;
    }
}
