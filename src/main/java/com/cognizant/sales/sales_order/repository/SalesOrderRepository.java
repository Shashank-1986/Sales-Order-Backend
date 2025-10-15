package com.cognizant.sales.sales_order.repository;

import com.cognizant.sales.sales_order.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    @Override
    @EntityGraph(value = "SalesOrder.withOrderItems")
    Page<SalesOrder> findAll(Pageable pageable);

    Page<SalesOrder> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<SalesOrder> findByCancellationDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}