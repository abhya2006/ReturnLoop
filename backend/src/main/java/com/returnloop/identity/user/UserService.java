package com.returnloop.identity.user;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.common.web.PageResponse;
import com.returnloop.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User administration for an organisation.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> list(UUID organizationId) {
        return repository.findByOrganizationIdOrderByFullNameAsc(organizationId).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(UUID organizationId, CreateUserRequest request) {
        validateEmail(request.email());
        if (request.fullName() == null || request.fullName().isBlank() || request.fullName().length() > 160) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A name of 1 to 160 characters is required");
        }
        if (repository.existsByOrganizationIdAndEmailIgnoreCase(organizationId, request.email())) {
            throw ApiException.conflict("A user with email " + request.email() + " already exists");
        }
        if (request.password() == null || request.password().length() < 10) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "The password must be at least 10 characters long");
        }
        UserAccount user = new UserAccount();
        user.setOrganizationId(organizationId);
        user.setEmail(request.email().toLowerCase());
        user.setFullName(request.fullName());
        user.setRole(request.role() == null ? Role.VIEWER : request.role());
        user.setJobTitle(request.jobTitle());
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return UserResponse.from(repository.save(user));
    }

    @Transactional
    public UserResponse update(UUID organizationId, UUID userId, UpdateUserRequest request) {
        UserAccount user = require(organizationId, userId);
        if (request.fullName() != null) {
            if (request.fullName().isBlank() || request.fullName().length() > 160) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "A name of 1 to 160 characters is required");
            }
            user.setFullName(request.fullName());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.jobTitle() != null) {
            user.setJobTitle(request.jobTitle());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.password() != null) {
            if (request.password().length() < 10) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED,
                        "The password must be at least 10 characters long");
            }
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(repository.save(user));
    }

    @Transactional
    public void deactivate(UUID organizationId, UUID userId) {
        UserAccount user = require(organizationId, userId);
        user.setActive(false);
        repository.save(user);
    }

    private void validateEmail(String email) {
        if (email == null || email.length() > 254
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A valid email address is required");
        }
    }

    @Transactional(readOnly = true)
    public UserAccount require(UUID organizationId, UUID userId) {
        UserAccount user = repository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User", userId));
        if (!user.getOrganizationId().equals(organizationId)) {
            throw new ApiException(ErrorCode.CROSS_TENANT_ACCESS);
        }
        return user;
    }

    public record CreateUserRequest(@NotBlank @Email @Size(max = 254) String email,
                                   @NotBlank @Size(max = 160) String fullName,
                                   @NotBlank @Size(min = 10, max = 128) String password,
                                   Role role, @Size(max = 160) String jobTitle) {
    }

    public record UpdateUserRequest(@Size(min = 1, max = 160) String fullName, Role role,
                                    @Size(max = 160) String jobTitle, Boolean active,
                                    @Size(min = 10, max = 128) String password) {
    }
}
