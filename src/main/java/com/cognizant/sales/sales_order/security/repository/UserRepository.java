package com.cognizant.sales.sales_order.security.repository;

import com.cognizant.sales.sales_order.security.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}