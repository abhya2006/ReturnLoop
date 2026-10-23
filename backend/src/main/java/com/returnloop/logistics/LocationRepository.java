package com.returnloop.logistics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    List<Location> findByOrganizationIdAndLocationType(UUID organizationId, LocationType locationType);

    List<Location> findByOrganizationIdAndActiveTrue(UUID organizationId);

    Optional<Location> findFirstByOrganizationIdAndLocationTypeOrderByNameAsc(UUID organizationId,
                                                                           LocationType locationType);
}
