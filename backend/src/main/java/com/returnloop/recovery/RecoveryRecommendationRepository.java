package com.returnloop.recovery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryRecommendationRepository extends JpaRepository<RecoveryRecommendation, UUID> {

    List<RecoveryRecommendation> findByReturnIdOrderByCreatedAtDesc(UUID returnId);

    List<RecoveryRecommendation> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<RecoveryRecommendation> findFirstByReturnIdOrderByCreatedAtDesc(UUID returnId);

    Optional<RecoveryRecommendation> findFirstByReturnItemIdOrderByCreatedAtDesc(UUID returnItemId);
}
