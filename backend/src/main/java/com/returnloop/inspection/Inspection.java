package com.returnloop.inspection;

import com.returnloop.common.domain.TenantEntity;
import com.returnloop.returns.ProductCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A warehouse inspection of one return item.
 */
@Entity
@Table(name = "inspections",
        indexes = {
                @Index(name = "idx_inspections_return_item", columnList = "return_item_id"),
                @Index(name = "idx_inspections_org_status", columnList = "organization_id,status")
        })
@Getter
@Setter
public class Inspection extends TenantEntity {

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "return_item_id", nullable = false)
    private UUID returnItemId;

    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "product_name", length = 240)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private InspectionStatus status = InspectionStatus.AWAITING_INSPECTION;

    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_condition", length = 40)
    private ProductCondition packagingCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "functionality_result", length = 40)
    private ProductCondition functionalityResult;

    @Column(name = "functional", nullable = false)
    private boolean functional = false;

    @Column(name = "accessories_complete", nullable = false)
    private boolean accessoriesComplete = true;

    @Column(name = "damage_description", length = 1000)
    private String damageDescription;

    @Column(name = "missing_accessories", length = 500)
    private String missingAccessories;

    @Column(name = "estimated_repair_cost", precision = 14, scale = 2)
    private BigDecimal estimatedRepairCost;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "inspected_by", length = 160)
    private String inspectedBy;

    @Column(name = "inspected_at")
    private Instant inspectedAt;

    @Column(name = "flagged_unsafe", nullable = false)
    private boolean flaggedUnsafe = false;

    /**
     * Maps the inspection outcome onto a product condition so the recovery
     * engine reasons over a single, consistent vocabulary.
     */
    public ProductCondition toCondition() {
        if (flaggedUnsafe) {
            return ProductCondition.UNSAFE;
        }
        if (status == InspectionStatus.DAMAGED) {
            return functional ? ProductCondition.FAIR : ProductCondition.DAMAGED;
        }
        if (status == InspectionStatus.INCOMPLETE) {
            return ProductCondition.FAIR;
        }
        if (status == InspectionStatus.UNSAFE) {
            return ProductCondition.UNSAFE;
        }
        if (status == InspectionStatus.PASSED) {
            if (functionalityResult != null) {
                return functionalityResult;
            }
            return accessoriesComplete ? ProductCondition.LIKE_NEW : ProductCondition.GOOD;
        }
        return ProductCondition.GOOD;
    }
}
