package com.returnloop.partner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecoveryJobEventRepository extends JpaRepository<RecoveryJobEvent, UUID> {

    List<RecoveryJobEvent> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}
