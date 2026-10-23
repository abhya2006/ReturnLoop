package com.returnloop.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    @Query("""
            select i from InventoryItem i
            where i.organizationId = :org
              and (:search is null or lower(i.sku) like lower(concat('%', :search, '%'))
                   or lower(i.productName) like lower(concat('%', :search, '%')))
              and (:status is null or i.status = :status)
              and (:category is null or i.category = :category)
              and (:warehouseId is null or i.warehouseId = :warehouseId)
            """)
    Page<InventoryItem> search(@Param("org") UUID organizationId,
                                @Param("search") String search,
                                @Param("status") InventoryStatus status,
                                @Param("category") String category,
                                @Param("warehouseId") UUID warehouseId,
                                Pageable pageable);

    Optional<InventoryItem> findByOrganizationIdAndReturnIdAndSku(UUID organizationId, UUID returnId, String sku);

    List<InventoryItem> findByOrganizationIdAndStatus(UUID organizationId, InventoryStatus status);

    long countByOrganizationIdAndStatus(UUID organizationId, InventoryStatus status);

    @Query("""
            select i from InventoryItem i
            where i.organizationId = :org
              and i.status in :statuses
              and i.receivedAt < :cutoff
            order by i.receivedAt asc
            """)
    List<InventoryItem> findAging(@Param("org") UUID organizationId,
                                  @Param("statuses") List<InventoryStatus> statuses,
                                  @Param("cutoff") Instant cutoff);

    @Query("""
            select coalesce(sum(i.quantity), 0) from InventoryItem i
            where i.organizationId = :org and i.status = :status
            """)
    long countUnitsByStatus(@Param("org") UUID organizationId, @Param("status") InventoryStatus status);

    @Query("""
            select coalesce(sum(i.estimatedValue), 0) from InventoryItem i
            where i.organizationId = :org
            """)
    BigDecimal sumEstimatedValue(@Param("org") UUID organizationId);
}
