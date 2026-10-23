package com.returnloop.recovery;

import com.returnloop.common.domain.TenantEntity;
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
 * A stored recovery recommendation for a return item, including the full set
 * of evaluated options and the operator's decision.
 */
@Entity
@Table(name = "recovery_recommendations",
        indexes = {
                @Index(name = "idx_recovery_return_item", columnList = "return_id,return_item_id"),
                @Index(name = "idx_recovery_org_created", columnList = "organization_id,created_at")
        })
@Getter
@Setter
public class RecoveryRecommendation extends TenantEntity {

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "return_item_id", nullable = false)
    private UUID returnItemId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "sku", length = 64)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_path", nullable = false, length = 40)
    private RecoveryPath recommendedPath;

    @Column(name = "recommended_net_value", precision = 14, scale = 2)
    private BigDecimal recommendedNetValue;

    /** The full ranked option set, serialised as JSON. */
    @Column(name = "options_json", columnDefinition = "text")
    private String optionsJson;

    @Column(name = "strategy", nullable = false, length = 60)
    private String strategy = "rule-based-explainable";

    @Column(name = "approved", nullable = false)
    private boolean approved = false;

    @Column(name = "rejected", nullable = false)
    private boolean rejected = false;

    @Column(name = "decision_reason", length = 1000)
    private String decisionReason;

    @Column(name = "decided_by", length = 160)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "estimated_processing_days")
    private Integer estimatedProcessingDays;
}
