package com.cognizant.sales.sales_order.service;

import com.cognizant.sales.sales_order.dto.SalesOrderRequestDto;
import com.cognizant.sales.sales_order.entity.OrderStatus;
import com.cognizant.sales.sales_order.entity.Product;
import com.cognizant.sales.sales_order.entity.SalesOrder;
import com.cognizant.sales.sales_order.repository.ProductRepository;
import com.cognizant.sales.sales_order.repository.SalesOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SalesOrderService salesOrderService;

    // Common objects for tests
    private SalesOrderRequestDto requestDto;
    private Product product;
    private SalesOrder salesOrder;

    @BeforeEach
    void setUp() {
        // This method, annotated with @BeforeEach, runs before every single test.
        // It sets up a clean state for each test run.
        requestDto = new SalesOrderRequestDto();
        requestDto.setCustomerName("Test Customer");

        SalesOrderRequestDto.OrderItemRequestDto itemDto = new SalesOrderRequestDto.OrderItemRequestDto();
        itemDto.setProductName("Laptop");
        itemDto.setQuantity(1);
        requestDto.setItems(singletonList(itemDto));

        product = new Product(1L, null, "Laptop", new BigDecimal("1200.00"));

        salesOrder = new SalesOrder();
        salesOrder.setId(1L);
        salesOrder.setCustomerName("Existing Customer");
        salesOrder.setStatus(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("Should cancel an existing order successfully")
    void testCancelSalesOrder_Success() {
        // 1. Arrange
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenReturn(salesOrder);

        // 2. Act
        SalesOrder result = salesOrderService.cancelSalesOrder(1L);

        // 3. Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(result.getCancellationDate()).isNotNull();
        verify(salesOrderRepository, times(1)).findById(1L);
        verify(salesOrderRepository, times(1)).save(salesOrder);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when cancelling an already cancelled order")
    void testCancelSalesOrder_AlreadyCancelled() {
        // 1. Arrange
        salesOrder.setStatus(OrderStatus.CANCELLED);
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        // 2. Act & 3. Assert
        assertThrows(IllegalStateException.class, () -> salesOrderService.cancelSalesOrder(1L));

        verify(salesOrderRepository, times(1)).findById(1L);
        verify(salesOrderRepository, never()).save(any(SalesOrder.class));
    }

    @Test
    @DisplayName("Should create order successfully and calculate totals")
    void testCreateSalesOrder_Success() {
        // 1. Arrange: Define the behavior of our mocks.
        // "When productRepository.findByNameIgnoreCase is called with 'Laptop', then return our sample product."
        when(productRepository.findByNameIgnoreCase("Laptop")).thenReturn(Optional.of(product));
        when(salesOrderRepository.save(any(SalesOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        SalesOrder result = salesOrderService.createSalesOrder(requestDto);

        // 3. Assert: Verify that the outcome is what we expect.
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getCustomerName()).isEqualTo("Test Customer");

        // Verify the calculations (Subtotal = 1 * 1200.00)
        assertThat(result.getSubtotal()).isEqualByComparingTo("1200.00");
        // VAT = 1200.00 * 0.20 = 240.00
        assertThat(result.getVat()).isEqualByComparingTo("240.00");
        // Total = 1200.00 + 240.00 = 1440.00
        assertThat(result.getTotal()).isEqualByComparingTo("1440.00");

        // Verify mock interactions
        verify(productRepository, times(1)).findByNameIgnoreCase("Laptop");
        verify(salesOrderRepository, times(1)).save(any(SalesOrder.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when product does not exist")
    void testCreateSalesOrder_ProductNotFound() {
        // Arrange
        when(productRepository.findByNameIgnoreCase("Laptop")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> salesOrderService.createSalesOrder(requestDto));

        // Verify save was not called
        verify(salesOrderRepository, never()).save(any(SalesOrder.class));
    }

    @Test
    @DisplayName("Should return a page of sales orders")
    void testGetAllSalesOrders() {
        // 1. Arrange
        Page<SalesOrder> expectedPage = new PageImpl<>(singletonList(salesOrder));
        when(salesOrderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(expectedPage);

        // 2. Act
        Page<SalesOrder> result = salesOrderService.getAllSalesOrders(null, null, null, null, Pageable.unpaged());

        // 3. Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCustomerName()).isEqualTo("Existing Customer");
        verify(salesOrderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return a sales order by its ID")
    void testGetSalesOrderById() {
        // 1. Arrange
        when(salesOrderRepository.findById(1L)).thenReturn(Optional.of(salesOrder));

        // 2. Act
        SalesOrder result = salesOrderService.getSalesOrderById(1L);

        // 3. Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when order ID does not exist")
    void testGetSalesOrderById_NotFound() {
        // 1. Arrange
        when(salesOrderRepository.findById(anyLong())).thenReturn(Optional.empty());

        // 2. Act & 3. Assert
        assertThrows(EntityNotFoundException.class, () -> salesOrderService.getSalesOrderById(99L));
    }
}
