package com.example.bulkupdate.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSuspensionService {

    private static final Logger logger = LoggerFactory.getLogger(ProductSuspensionService.class);

    private final ProductRepository productRepository;

    public ProductSuspensionService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public void suspendAndRecordReason(Long productId, String reason) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        logger.info("管理中エンティティを取得: id={}, status={}, note={}",
                product.getId(), product.getStatus(), product.getNote());

        int updatedRows = productRepository.updateStatus(productId, ProductStatus.SUSPENDED);
        logger.info("バルク更新を実行: id={}, updatedRows={}, 管理中status={}",
                productId, updatedRows, product.getStatus());

        product.changeNote(reason);
        logger.info("停止理由を更新: id={}, 管理中status={}, note={}",
                productId, product.getStatus(), product.getNote());
    }

    @Transactional(readOnly = true)
    public ProductView findById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return ProductView.from(product);
    }
}
