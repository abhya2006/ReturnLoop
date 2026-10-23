package com.returnloop.logistics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByOrganizationIdAndActiveTrue(UUID organizationId);

    List<Vehicle> findByOrganizationIdOrderByNameAsc(UUID organizationId);
}
