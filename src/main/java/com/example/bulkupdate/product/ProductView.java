package com.example.bulkupdate.product;

public record ProductView(Long id, String sku, ProductStatus status, String note) {

    public static ProductView from(Product product) {
        return new ProductView(
                product.getId(),
                product.getSku(),
                product.getStatus(),
                product.getNote()
        );
    }
}
