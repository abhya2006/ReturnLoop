package com.returnloop.catalog;

import com.returnloop.common.web.ApiResponse;
import com.returnloop.common.web.PageResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Product and SKU catalogue")
public class ProductController {

    private final ProductService service;
    private final CurrentUser currentUser;

    @GetMapping
    public PageResponse<ProductResponse> search(@RequestParam(required = false) String search,
                                                @RequestParam(required = false) ProductCategory category,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "25") int size,
                                                @RequestParam(defaultValue = "name,asc") String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(parseSort(sort)));
        return service.search(currentUser.organizationId(), search, category, pageable);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> get(@PathVariable UUID id) {
        return ApiResponse.of(ProductResponse.from(service.require(currentUser.organizationId(), id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductService.ProductRequest request) {
        ProductResponse created = service.create(currentUser.organizationId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody ProductService.ProductRequest request) {
        return ApiResponse.of(service.update(currentUser.organizationId(), id, request));
    }

    private static Sort.Order[] parseSort(String sort) {
        String[] parts = sort.split(",");
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = parts[0].isBlank() ? "name" : parts[0];
        return new Sort.Order[]{Sort.Order.by(property).with(direction)};
    }
}
