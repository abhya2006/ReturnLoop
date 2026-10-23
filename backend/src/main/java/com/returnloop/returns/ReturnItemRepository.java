package com.returnloop.returns;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, UUID> {

    List<ReturnItem> findByReturnIdOrderById(UUID returnId);

    List<ReturnItem> findByOrganizationIdAndReturnId(UUID organizationId, UUID returnId);

    void deleteByReturnId(UUID returnId);
}
