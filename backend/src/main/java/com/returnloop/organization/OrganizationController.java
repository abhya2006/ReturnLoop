package com.returnloop.organization;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.web.ApiResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Tenant administration")
public class OrganizationController {

    private final OrganizationRepository repository;
    private final CurrentUser currentUser;

    @GetMapping("/current")
    public ApiResponse<OrganizationResponse> current() {
        return ApiResponse.of(OrganizationResponse.from(require(currentUser.organizationId())));
    }

    @PutMapping("/current")
    public ApiResponse<OrganizationResponse> update(@RequestBody UpdateOrganizationRequest request) {
        Organization organization = require(currentUser.organizationId());
        if (request.name() != null) {
            organization.setName(request.name());
        }
        if (request.contactEmail() != null) {
            organization.setContactEmail(request.contactEmail());
        }
        if (request.country() != null) {
            organization.setCountry(request.country());
        }
        if (request.holdingThresholdDays() != null) {
            if (request.holdingThresholdDays() < 1) {
                throw new com.returnloop.common.error.ApiException(
                        com.returnloop.common.error.ErrorCode.VALIDATION_FAILED,
                        "The holding threshold must be at least one day");
            }
            organization.setHoldingThresholdDays(request.holdingThresholdDays());
        }
        return ApiResponse.of(OrganizationResponse.from(repository.save(organization)));
    }

    private Organization require(UUID organizationId) {
        return repository.findById(organizationId)
                .orElseThrow(() -> ApiException.notFound("Organization", organizationId));
    }

    public record UpdateOrganizationRequest(String name, String contactEmail, String country,
                                            Integer holdingThresholdDays) {
    }
}
