package com.example.bulkupdate;

import com.example.bulkupdate.product.Product;
import com.example.bulkupdate.product.ProductRepository;
import com.example.bulkupdate.product.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductSuspensionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        Product product = productRepository.saveAndFlush(
                new Product("SKU-001", ProductStatus.ACTIVE, "登録直後")
        );
        productId = product.getId();
    }

    @Test
    void 停止APIが204を返しても商品は停止済みかつ理由が保存される() throws Exception {
        mockMvc.perform(post("/products/{productId}/suspension", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"品質確認のため停止"}
                                """))
                .andExpect(status().isNoContent());

        Product persistedProduct = productRepository.findById(productId).orElseThrow();

        assertAll(
                () -> assertThat(persistedProduct.getStatus()).isEqualTo(ProductStatus.SUSPENDED),
                () -> assertThat(persistedProduct.getNote()).isEqualTo("品質確認のため停止"),
                () -> assertThat(persistedProduct.getSku()).isEqualTo("SKU-001")
        );
    }
}
