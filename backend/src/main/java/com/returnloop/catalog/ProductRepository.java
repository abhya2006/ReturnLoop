package com.returnloop.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByOrganizationIdAndSkuIgnoreCase(UUID organizationId, String sku);

    boolean existsByOrganizationIdAndSkuIgnoreCase(UUID organizationId, String sku);

    @Query("""
            select p from Product p
            where p.organizationId = :org
              and (:search is null or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(p.sku) like lower(concat('%', :search, '%'))
                   or lower(coalesce(p.brand, '')) like lower(concat('%', :search, '%')))
              and (:category is null or p.category = :category)
            """)
    Page<Product> search(@Param("org") UUID organizationId,
                          @Param("search") String search,
                          @Param("category") ProductCategory category,
                          Pageable pageable);

    List<Product> findByOrganizationIdAndIdIn(UUID organizationId, List<UUID> ids);

    List<Product> findByOrganizationIdOrderByNameAsc(UUID organizationId);
}
