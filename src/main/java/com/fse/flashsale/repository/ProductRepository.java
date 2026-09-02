package com.fse.flashsale.repository;

import com.fse.flashsale.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
