package com.da.billiards.catalog;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.da.billiards.ApiTestSupport;

class CatalogApiTest extends ApiTestSupport {

    private Product cheapPoolCue;

    @BeforeEach
    void seedCatalog() {
        Category cues = data.category("Cơ bida", null);
        Category pool = data.category("Cơ lỗ", cues);
        Category carom = data.category("Cơ carom", cues);
        Category tables = data.category("Bàn bida", null);
        Brand falcon = data.brand("Falcon");
        Brand aria = data.brand("Aria");

        cheapPoolCue = data.product("Cơ lỗ giá rẻ", pool, falcon, 1_000_000, null, 5);
        data.product("Cơ lỗ cao cấp", pool, aria, 9_000_000, 8_000_000L, 2);
        data.product("Cơ carom thi đấu", carom, aria, 4_000_000, null, 1);
        data.product("Bàn bida 9 feet", tables, falcon, 60_000_000, null, 1);
        Product hidden = data.product("Cơ đã ngừng bán", pool, falcon, 2_000_000, null, 3);
        hidden.setActive(false);
    }

    @Test
    void categoryTreeCountsIncludeChildrenAndIgnoreHiddenProducts() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.slug=='co-bida')].productCount").value(3))
                .andExpect(jsonPath("$[?(@.slug=='co-bida')].children[*].slug",
                        hasSize(2)))
                .andExpect(jsonPath("$[?(@.slug=='ban-bida')].productCount").value(1));
    }

    @Test
    void filterByParentCategoryIncludesDescendants() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("category", "co-bida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].name",
                        containsInAnyOrder("Cơ lỗ giá rẻ", "Cơ lỗ cao cấp", "Cơ carom thi đấu")));
    }

    @Test
    void unknownCategoryReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("category", "khong-ton-tai"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filterByBrandListAndPriceRange() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("brand", "aria"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/products").param("brand", "aria,falcon"))
                .andExpect(jsonPath("$.totalElements").value(4));
        // Lọc theo giá bán thực tế: cơ cao cấp đang giảm còn 8.000.000đ
        mockMvc.perform(get("/api/v1/products").param("minPrice", "3000000").param("maxPrice", "8000000"))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Cơ lỗ cao cấp", "Cơ carom thi đấu")));
    }

    @Test
    void searchByNameIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("q", "CAROM"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Cơ carom thi đấu"));
    }

    @Test
    void sortByPriceBothDirections() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("sort", "price_asc"))
                .andExpect(jsonPath("$.content[0].name").value("Cơ lỗ giá rẻ"))
                .andExpect(jsonPath("$.content[3].name").value("Bàn bida 9 feet"));
        mockMvc.perform(get("/api/v1/products").param("sort", "price_desc"))
                .andExpect(jsonPath("$.content[0].name").value("Bàn bida 9 feet"));
    }

    @Test
    void paginationAndSizeCap() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/api/v1/products").param("size", "1000"))
                .andExpect(jsonPath("$.size").value(48));
    }

    @Test
    void hiddenProductIsNotPublic() throws Exception {
        mockMvc.perform(get("/api/v1/products").param("q", "ngừng bán"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/products/co-da-ngung-ban")).andExpect(status().isNotFound());
    }

    @Test
    void productDetailAndRelated() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + cheapPoolCue.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.slug").value("co-lo"))
                .andExpect(jsonPath("$.brand.slug").value("falcon"))
                .andExpect(jsonPath("$.variants.length()").value(1))
                .andExpect(jsonPath("$.variants[0].stock").value(5));
        mockMvc.perform(get("/api/v1/products/" + cheapPoolCue.getSlug() + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Cơ lỗ cao cấp")));
    }

    @Test
    void brandsAreSortedByName() throws Exception {
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(jsonPath("$[0].name").value("Aria"))
                .andExpect(jsonPath("$[1].name").value("Falcon"));
    }
}
