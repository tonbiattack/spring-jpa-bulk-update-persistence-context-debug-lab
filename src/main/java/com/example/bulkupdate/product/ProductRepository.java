package com.example.bulkupdate.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("update Product product set product.status = :status where product.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") ProductStatus status);
}
