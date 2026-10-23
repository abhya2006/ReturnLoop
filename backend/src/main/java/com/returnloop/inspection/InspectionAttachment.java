package com.returnloop.inspection;

import com.returnloop.common.domain.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * A photo or document captured during an inspection.
 */
@Entity
@Table(name = "inspection_attachments",
        indexes = @Index(name = "idx_attachments_inspection", columnList = "inspection_id"))
@Getter
@Setter
public class InspectionAttachment extends TenantEntity {

    @Column(name = "inspection_id", nullable = false)
    private UUID inspectionId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "caption", length = 500)
    private String caption;
}
