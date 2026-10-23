package com.returnloop.identity.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByGoogleSubject(String googleSubject);

    List<UserAccount> findByOrganizationIdOrderByFullNameAsc(UUID organizationId);

    List<UserAccount> findByOrganizationId(UUID organizationId);

    List<UserAccount> findByOrganizationIdAndRole(UUID organizationId, com.returnloop.security.Role role);

    boolean existsByOrganizationIdAndEmailIgnoreCase(UUID organizationId, String email);
}
