package com.cognizant.sales.sales_order.service;

import com.cognizant.sales.sales_order.entity.Product;
import com.cognizant.sales.sales_order.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product(1L, 1L, "Test Product", new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("Should save a product and return it")
    void testCreateOrUpdateProduct() {
        // Arrange
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // Act
        Product savedProduct = productService.createOrUpdateProduct(product);

        // Assert
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Test Product");
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Should return a list of all products")
    void testGetAllProducts() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of(product));

        // Act
        List<Product> products = productService.getAllProducts();

        // Assert
        assertThat(products).isNotNull();
        assertThat(products).hasSize(1);
        assertThat(products.get(0)).isEqualTo(product);
    }

    @Test
    @DisplayName("Should return a product by its ID")
    void testGetProductById() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // Act
        Product foundProduct = productService.getProductById(1L);

        // Assert
        assertThat(foundProduct).isNotNull();
        assertThat(foundProduct.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when product ID does not exist")
    void testGetProductById_NotFound() {
        // Arrange
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> productService.getProductById(99L));
    }
}
