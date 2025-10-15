package com.cognizant.sales.sales_order.dto;

import com.cognizant.sales.sales_order.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SalesOrderResponseDto {
    private Long id;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDateTime orderDate;

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDateTime cancellationDate;

    private String customerName;
    private List<OrderItemResponseDto> purchasedItems;
    private BigDecimal subtotal;
    private BigDecimal vat;
    private BigDecimal total;
    private OrderStatus status;

    @Data
    public static class OrderItemResponseDto {
        private String productName;
        private BigDecimal price;
        private int quantity;
    }
}