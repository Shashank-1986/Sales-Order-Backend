package com.cognizant.sales.sales_order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderRequestDto {
    @NotBlank(message = "Customer name cannot be blank")
    private String customerName;

    @NotEmpty(message = "Order must contain at least one item")
    private List<@Valid OrderItemRequestDto> items;

    @Data
    public static class OrderItemRequestDto {
        @NotBlank(message = "Product name cannot be blank")
        private String productName;
        @NotNull(message = "Quantity cannot be null")
        @Positive(message = "Quantity must be a positive number")
        private int quantity;
    }
}