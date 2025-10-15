package com.cognizant.sales.sales_order;

import com.cognizant.sales.sales_order.security.model.ERole;
import com.cognizant.sales.sales_order.security.model.Role;
import com.cognizant.sales.sales_order.security.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class SalesOrderApplication {

	@Bean
	CommandLineRunner run(RoleRepository roleRepository) {
		return args -> {
			if (roleRepository.findByName(ERole.ROLE_ADMIN).isEmpty()) {
				Role adminRole = new Role();
				adminRole.setName(ERole.ROLE_ADMIN);
				roleRepository.save(adminRole);
			}
			if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
				Role userRole = new Role();
				userRole.setName(ERole.ROLE_USER);
				roleRepository.save(userRole);
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SalesOrderApplication.class, args);
	}

}
