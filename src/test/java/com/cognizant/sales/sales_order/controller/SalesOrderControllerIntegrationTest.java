package com.cognizant.sales.sales_order.controller;

import com.cognizant.sales.sales_order.entity.Product;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Deactivate default in-memory db
@Testcontainers
class SalesOrderControllerIntegrationTest {

    @Container
    @ServiceConnection // This annotation handles the dynamic properties for us
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.26");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @WithMockUser(roles = "ADMIN") // Use an admin user to set up the product catalog
    void setUp() throws Exception {
        // Before each test, ensure the product catalog has the necessary item.
        Product product = new Product();
        product.setName("Integration Test Laptop");
        product.setPrice(new BigDecimal("1500.00"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER") // A regular user can create an order
    void shouldCreateSalesOrderSuccessfully() throws Exception {
        // Arrange: Prepare the request body for creating a sales order
        String salesOrderRequestJson = """
                {
                  "customerName": "Integration Test Customer",
                  "items": [
                    {
                      "productName": "Integration Test Laptop",
                      "quantity": 2
                    }
                  ]
                }
                """;

        // Act & Assert: Perform the POST request and verify the response
        mockMvc.perform(post("/api/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(salesOrderRequestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Integration Test Customer"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.purchasedItems[0].productName").value("Integration Test Laptop"))
                .andExpect(jsonPath("$.purchasedItems[0].quantity").value(2))
                .andExpect(jsonPath("$.purchasedItems[0].price").value(1500.00))
                .andExpect(jsonPath("$.subtotal").value(3000.00)) // 2 * 1500
                .andExpect(jsonPath("$.vat").value(600.00))      // 3000 * 0.20
                .andExpect(jsonPath("$.total").value(3600.00));   // 3000 + 600
    }
}
