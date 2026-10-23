package com.returnloop.partner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    @Query("""
            select p from Partner p
            where p.organizationId = :org
              and (:search is null or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
              and (:type is null or p.partnerType = :type)
              and (:category is null or lower(p.supportedCategories) like lower(concat('%', :category, '%')))
              and (:region is null or lower(p.serviceableRegions) like lower(concat('%', :region, '%')))
              and (:availableOnly = false or p.currentlyAvailable = true)
            """)
    Page<Partner> search(@Param("org") UUID organizationId,
                         @Param("search") String search,
                         @Param("type") PartnerType type,
                         @Param("category") String category,
                         @Param("region") String region,
                         @Param("availableOnly") boolean availableOnly,
                         Pageable pageable);

    List<Partner> findByOrganizationIdAndPartnerTypeAndCurrentlyAvailableTrue(UUID organizationId,
                                                                                PartnerType partnerType);

    Optional<Partner> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
