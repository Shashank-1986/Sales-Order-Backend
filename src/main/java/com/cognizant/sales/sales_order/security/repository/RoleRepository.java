package com.cognizant.sales.sales_order.security.repository;

import com.cognizant.sales.sales_order.security.model.ERole;
import com.cognizant.sales.sales_order.security.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}