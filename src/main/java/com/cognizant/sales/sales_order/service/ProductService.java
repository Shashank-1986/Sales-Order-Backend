package com.cognizant.sales.sales_order.service;

import com.cognizant.sales.sales_order.entity.Product;
import com.cognizant.sales.sales_order.exception.DuplicateResourceException;
import com.cognizant.sales.sales_order.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product createOrUpdateProduct(Product product) {
        // When creating a new product (id is null), check if the name is already taken.
        if (product.getId() == null) {
            productRepository.findByNameIgnoreCase(product.getName()).ifPresent(p -> {
                throw new DuplicateResourceException("Product with name '" + product.getName() + "' already exists.");
            });
        }
        return productRepository.save(product);
    }

    @Cacheable("products")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
    }
}