package com.cognizant.sales.sales_order.controller;

import jakarta.validation.Valid;
import com.cognizant.sales.sales_order.entity.SalesOrder;
import com.cognizant.sales.sales_order.dto.SalesOrderRequestDto;
import com.cognizant.sales.sales_order.dto.SalesOrderResponseDto;
import com.cognizant.sales.sales_order.service.SalesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * REST controller for managing sales orders.
 * Provides endpoints for creating, retrieving, listing, and canceling sales orders.
 */
@RestController
@RequestMapping("/api/sales-orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @Autowired
    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    /**
     * Creates a new sales order.
     * @param requestDto The request body containing customer and item details.
     * @return The created sales order.
     */
    @PostMapping
    public ResponseEntity<SalesOrderResponseDto> createSalesOrder(@Valid @RequestBody SalesOrderRequestDto requestDto) {
        SalesOrder salesOrder = salesOrderService.createSalesOrder(requestDto);
        return new ResponseEntity<>(convertToDto(salesOrder), HttpStatus.CREATED);
    }

    /**
     * Retrieves a single sales order by its ID.
     * @param id The ID of the sales order.
     * @return The sales order details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponseDto> getSalesOrderById(@PathVariable Long id) {
        SalesOrder salesOrder = salesOrderService.getSalesOrderById(id);
        return ResponseEntity.ok(convertToDto(salesOrder));
    }

    /**
     * Lists all sales orders with support for filtering, pagination, and sorting.
     * @param createdStart Start of the creation date range filter.
     * @param createdEnd End of the creation date range filter.
     * @param cancelledStart Start of the cancellation date range filter.
     * @param cancelledEnd End of the cancellation date range filter.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of sales orders.
     */
    @GetMapping
    public ResponseEntity<Page<SalesOrderResponseDto>> getAllSalesOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cancelledStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cancelledEnd,
            Pageable pageable) {
        Page<SalesOrder> page = salesOrderService.getAllSalesOrders(createdStart, createdEnd, cancelledStart, cancelledEnd, pageable);
        return ResponseEntity.ok(page.map(this::convertToDto));
    }

    /**
     * Cancels a sales order by its ID.
     * @param id The ID of the sales order to cancel.
     * @return The updated sales order with a 'CANCELLED' status.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<SalesOrderResponseDto> cancelSalesOrder(@PathVariable Long id) {
        SalesOrder salesOrder = salesOrderService.cancelSalesOrder(id);
        return ResponseEntity.ok(convertToDto(salesOrder));
    }

    /**
     * Converts a SalesOrder entity to a SalesOrderResponseDto.
     * @param salesOrder The entity to convert.
     * @return The DTO.
     */
    private SalesOrderResponseDto convertToDto(SalesOrder salesOrder) {
        SalesOrderResponseDto dto = new SalesOrderResponseDto();
        dto.setId(salesOrder.getId());
        dto.setOrderDate(salesOrder.getOrderDate());
        dto.setCancellationDate(salesOrder.getCancellationDate());
        dto.setCustomerName(salesOrder.getCustomerName());
        dto.setSubtotal(salesOrder.getSubtotal());
        dto.setVat(salesOrder.getVat());
        dto.setTotal(salesOrder.getTotal());
        dto.setStatus(salesOrder.getStatus());

        dto.setPurchasedItems(salesOrder.getOrderItems().stream()
                .map(this::convertOrderItemToDto)
                .collect(Collectors.toList()));

        return dto;
    }

    /**
     * Converts an OrderItem entity to an OrderItemResponseDto.
     * @param orderItem The entity to convert.
     * @return The DTO.
     */
    private SalesOrderResponseDto.OrderItemResponseDto convertOrderItemToDto(com.cognizant.sales.sales_order.entity.OrderItem orderItem) {
        SalesOrderResponseDto.OrderItemResponseDto dto = new SalesOrderResponseDto.OrderItemResponseDto();
        dto.setProductName(orderItem.getProductName());
        dto.setPrice(orderItem.getPrice());
        dto.setQuantity(orderItem.getQuantity());
        return dto;
    }
}