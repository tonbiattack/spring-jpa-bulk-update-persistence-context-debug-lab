package com.example.bulkupdate.product;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("商品が見つかりません: " + productId);
    }
}
