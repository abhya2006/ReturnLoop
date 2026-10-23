package com.returnloop.inspection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InspectionAttachmentRepository extends JpaRepository<InspectionAttachment, UUID> {

    List<InspectionAttachment> findByInspectionIdOrderByCreatedAtAsc(UUID inspectionId);
}
