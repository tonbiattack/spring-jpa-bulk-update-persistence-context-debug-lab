package com.example.bulkupdate.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductSuspensionService productSuspensionService;

    public ProductController(ProductSuspensionService productSuspensionService) {
        this.productSuspensionService = productSuspensionService;
    }

    @PostMapping("/{productId}/suspension")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void suspend(@PathVariable Long productId, @RequestBody SuspendProductRequest request) {
        productSuspensionService.suspendAndRecordReason(productId, request.reason());
    }

    @GetMapping("/{productId}")
    public ProductView findById(@PathVariable Long productId) {
        return productSuspensionService.findById(productId);
    }

    public record SuspendProductRequest(String reason) {
    }
}
