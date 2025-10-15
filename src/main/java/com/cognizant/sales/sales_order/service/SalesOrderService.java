package com.cognizant.sales.sales_order.service;

import com.cognizant.sales.sales_order.entity.OrderItem;
import com.cognizant.sales.sales_order.entity.OrderStatus;
import com.cognizant.sales.sales_order.entity.SalesOrder;
import com.cognizant.sales.sales_order.dto.SalesOrderRequestDto;
import com.cognizant.sales.sales_order.repository.ProductRepository;
import com.cognizant.sales.sales_order.repository.SalesOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for handling sales order business logic.
 * This includes creation, retrieval, cancellation, and calculation of totals.
 */
@Service
public class SalesOrderService {

    private static final Logger logger = LoggerFactory.getLogger(SalesOrderService.class);

    private static final BigDecimal VAT_RATE = new BigDecimal("0.20"); // Assuming a 20% VAT rate

    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;

    @Autowired
    public SalesOrderService(SalesOrderRepository salesOrderRepository, ProductRepository productRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates a new sales order based on the provided request data.
     * It fetches current product prices, calculates totals, and persists the order.
     * This operation is transactional.
     * @param requestDto DTO containing the customer name and items to be purchased.
     * @return The newly created and saved SalesOrder entity.
     */
    @Transactional
    public SalesOrder createSalesOrder(SalesOrderRequestDto requestDto) {
        logger.info("Creating new sales order for customer: {}", requestDto.getCustomerName());
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerName(requestDto.getCustomerName());
        salesOrder.setStatus(OrderStatus.CREATED);

        List<OrderItem> orderItems = requestDto.getItems().stream()
                .map(itemDto -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductName(itemDto.getProductName());
                    orderItem.setQuantity(itemDto.getQuantity());
                    BigDecimal price = getPriceForProduct(itemDto.getProductName()); // Fetches price from the new catalog
                    orderItem.setPrice(price);
                    orderItem.setSalesOrder(salesOrder);
                    return orderItem;
                }).collect(Collectors.toList());

        salesOrder.setOrderItems(orderItems);

        calculateTotals(salesOrder);

        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);
        logger.info("Successfully created sales order with ID: {}", savedOrder.getId());
        return savedOrder;
    }

    /**
     * Retrieves a paginated list of sales orders, with optional filtering.
     * Uses JPA Specifications to build a dynamic query based on the provided filters.
     * @param createdStart The start of the creation date range.
     * @param createdEnd The end of the creation date range.
     * @param cancelledStart The start of the cancellation date range.
     * @param cancelledEnd The end of the cancellation date range.
     * @param pageable Pagination and sorting information.
     * @return A Page of SalesOrder entities.
     */
    public Page<SalesOrder> getAllSalesOrders(LocalDateTime createdStart, LocalDateTime createdEnd, LocalDateTime cancelledStart, LocalDateTime cancelledEnd, Pageable pageable) {
        return salesOrderRepository.findAll((Specification<SalesOrder>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (createdStart != null && createdEnd != null) {
                predicates.add(criteriaBuilder.between(root.get("orderDate"), createdStart, createdEnd));
            }

            if (cancelledStart != null && cancelledEnd != null) {
                predicates.add(criteriaBuilder.between(root.get("cancellationDate"), cancelledStart, cancelledEnd));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    /**
     * Finds a single sales order by its unique ID.
     * @param id The ID of the sales order to find.
     * @return The found SalesOrder.
     * @throws EntityNotFoundException if no order with the given ID exists.
     */
    public SalesOrder getSalesOrderById(Long id) {
        logger.debug("Attempting to find sales order with ID: {}", id);
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("SalesOrder not found with id: {}", id);
                    return new EntityNotFoundException("SalesOrder not found with id: " + id);
                });
    }

    /**
     * Cancels an existing sales order.
     * This operation is transactional.
     * @param id The ID of the sales order to cancel.
     * @return The updated SalesOrder with a 'CANCELLED' status.
     * @throws IllegalStateException if the order is already cancelled.
     */
    @Transactional
    public SalesOrder cancelSalesOrder(Long id) {
        SalesOrder salesOrder = getSalesOrderById(id);
        logger.info("Attempting to cancel sales order with ID: {}", id);
        if (salesOrder.getStatus() == OrderStatus.CANCELLED) {
            logger.warn("Attempted to cancel an already cancelled order with ID: {}", id);
            throw new IllegalStateException("Order is already cancelled.");
        }
        salesOrder.setStatus(OrderStatus.CANCELLED);
        salesOrder.setCancellationDate(LocalDateTime.now());
        SalesOrder cancelledOrder = salesOrderRepository.save(salesOrder);
        logger.info("Successfully cancelled sales order with ID: {}", cancelledOrder.getId());
        return cancelledOrder;
    }

    /**
     * Calculates the subtotal, VAT, and total for a given sales order based on its items.
     * @param salesOrder The sales order to calculate totals for.
     */
    private void calculateTotals(SalesOrder salesOrder) {
        BigDecimal subtotal = salesOrder.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal vat = subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(vat);

        salesOrder.setSubtotal(subtotal);
        salesOrder.setVat(vat);
        salesOrder.setTotal(total);
    }

    /**
     * Fetches the price for a given product name from the product repository.
     * @param productName The name of the product.
     * @return The price of the product.
     * @throws EntityNotFoundException if the product name does not exist in the catalog.
     */
    private BigDecimal getPriceForProduct(String productName) {
        return productRepository.findByNameIgnoreCase(productName)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with name: " + productName))
                .getPrice();
    }
}