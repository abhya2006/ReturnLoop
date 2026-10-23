package com.returnloop.returns;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReturnEventRepository extends JpaRepository<ReturnEvent, UUID> {

    List<ReturnEvent> findByReturnIdOrderByCreatedAtAsc(UUID returnId);

    List<ReturnEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
