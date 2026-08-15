package com.example.bulkupdate.product;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private String note;

    protected Product() {
    }

    public Product(String sku, ProductStatus status, String note) {
        this.sku = sku;
        this.status = status;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public void changeNote(String note) {
        this.note = note;
    }

    public void suspend() {
        this.status = ProductStatus.SUSPENDED;
    }
}
