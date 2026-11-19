package com.da.billiards.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import com.da.billiards.ApiTestSupport;
import com.da.billiards.catalog.Category;
import com.da.billiards.catalog.CategoryRepository;
import com.da.billiards.catalog.Product;
import com.da.billiards.catalog.ProductRepository;
import com.da.billiards.catalog.ProductVariant;

class OrderFlowTest extends ApiTestSupport {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long variantId;

    @BeforeEach
    void createProduct() {
        Category category = new Category();
        category.setName("Cơ lỗ");
        category.setSlug("co-lo-test");
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Cơ test");
        product.setSlug("co-test");
        product.setCategory(category);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName("19oz");
        variant.setPrice(1_200_000);
        variant.setSalePrice(1_000_000L);
        variant.setStock(3);
        product.getVariants().add(variant);
        product.refreshPricing();
        productRepository.save(product);
        variantId = variant.getId();
    }

    @Test
    void checkoutUsesServerPricesDeductsStockAndClearsCart() throws Exception {
        String token = registerCustomer("mua@test.local");
        addToCart(token, 2).andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value(2_000_000));

        String body = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("recipientName", "Nguyễn Văn A", "phone", "0912345678",
                                "address", "12 Lê Lợi, Hà Nội", "paymentMethod", "COD",
                                // Trường lạ do client tự thêm phải bị bỏ qua
                                "total", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.subtotal").value(2_000_000))
                .andExpect(jsonPath("$.shippingFee").value(0))
                .andExpect(jsonPath("$.total").value(2_000_000))
                .andExpect(jsonPath("$.items[0].unitPrice").value(1_000_000))
                .andReturn().getResponse().getContentAsString();

        assertThat(stock()).isEqualTo(1);
        mockMvc.perform(get("/api/v1/cart").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(0));

        String code = readJson(body).get("code").asText();
        mockMvc.perform(post("/api/v1/orders/me/" + code + "/cancel").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(stock()).isEqualTo(3);
    }

    @Test
    void smallOrderPaysShippingFee() throws Exception {
        String token = registerCustomer("le@test.local");
        addToCart(token, 1);
        checkout(token)
                .andExpect(jsonPath("$.shippingFee").value(OrderService.SHIPPING_FEE))
                .andExpect(jsonPath("$.total").value(1_000_000 + OrderService.SHIPPING_FEE));
    }

    @Test
    void cannotAddMoreThanStock() throws Exception {
        String token = registerCustomer("tham@test.local");
        addToCart(token, 4).andExpect(status().isConflict());
    }

    @Test
    void customersCannotSeeEachOthersOrders() throws Exception {
        String owner = registerCustomer("chu@test.local");
        String other = registerCustomer("khac@test.local");
        addToCart(owner, 1);
        String code = readJson(checkout(owner).andReturn().getResponse().getContentAsString()).get("code").asText();

        mockMvc.perform(get("/api/v1/orders/me/" + code).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminMustFollowStatusFlow() throws Exception {
        String customer = registerCustomer("luong@test.local");
        addToCart(customer, 1);
        long orderId = readJson(checkout(customer).andReturn().getResponse().getContentAsString()).get("id").asLong();
        String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        updateStatus(admin, orderId, "COMPLETED").andExpect(status().isConflict());
        updateStatus(admin, orderId, "CONFIRMED").andExpect(status().isOk());
        updateStatus(admin, orderId, "SHIPPING").andExpect(status().isOk());
        updateStatus(admin, orderId, "CANCELLED").andExpect(status().isConflict());
        updateStatus(admin, orderId, "COMPLETED").andExpect(status().isOk());
    }

    private ResultActions addToCart(String token, int quantity) throws Exception {
        return mockMvc.perform(post("/api/v1/cart/items")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("variantId", variantId, "quantity", quantity))));
    }

    private ResultActions checkout(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("recipientName", "Trần B", "phone", "0912345678",
                                "address", "1 Trần Phú", "paymentMethod", "BANK_TRANSFER"))))
                .andExpect(status().isCreated());
    }

    private ResultActions updateStatus(String token, long orderId, String status)
            throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", status))));
    }

    private int stock() {
        return jdbcTemplate.queryForObject("select stock from product_variants where id = ?", Integer.class, variantId);
    }
}
