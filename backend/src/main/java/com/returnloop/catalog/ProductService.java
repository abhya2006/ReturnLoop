package com.returnloop.catalog;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(UUID organizationId, String search, ProductCategory category,
                                                 Pageable pageable) {
        Page<Product> page = repository.search(organizationId,
                search == null || search.isBlank() ? null : search.trim(), category, pageable);
        return PageResponse.from(page, ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listAll(UUID organizationId) {
        return repository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Product require(UUID organizationId, UUID productId) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product", productId));
        if (!product.getOrganizationId().equals(organizationId)) {
            throw new ApiException(com.returnloop.common.error.ErrorCode.CROSS_TENANT_ACCESS);
        }
        return product;
    }

    @Transactional
    public ProductResponse create(UUID organizationId, ProductRequest request) {
        if (repository.existsByOrganizationIdAndSkuIgnoreCase(organizationId, request.sku())) {
            throw ApiException.conflict("A product with SKU " + request.sku() + " already exists");
        }
        Product product = new Product();
        product.setOrganizationId(organizationId);
        product.setSku(request.sku().trim());
        apply(product, request);
        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID organizationId, UUID productId, ProductRequest request) {
        Product product = require(organizationId, productId);
        apply(product, request);
        return ProductResponse.from(repository.save(product));
    }

    private void apply(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setBrand(request.brand());
        product.setModel(request.model());
        product.setCategory(request.category() == null ? ProductCategory.OTHER : request.category());
        product.setDescription(request.description());
        product.setImageUrl(request.imageUrl());
        product.setWeightKg(request.weightKg());
        product.setOriginalPrice(request.originalPrice());
        product.setResaleValue(request.resaleValue() == null ? request.originalPrice() : request.resaleValue());
        product.setActive(request.active() == null || request.active());
    }

    public record ProductRequest(String sku, String name, String brand, String model, ProductCategory category,
                                 String description, String imageUrl, BigDecimal weightKg, BigDecimal originalPrice,
                                 BigDecimal resaleValue, Boolean active) {
    }
}
