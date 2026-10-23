package com.returnloop.identity.user;

import com.returnloop.common.web.ApiResponse;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users & Roles", description = "Organisation user administration")
public class UserController {

    private final UserService service;
    private final CurrentUser currentUser;

    @GetMapping
    public ApiResponse<List<UserResponse>> list() {
        return ApiResponse.of(service.list(currentUser.organizationId()));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserService.CreateUserRequest request) {
        return ApiResponse.of(service.create(currentUser.organizationId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable UUID id,
                                            @Valid @RequestBody UserService.UpdateUserRequest request) {
        return ApiResponse.of(service.update(currentUser.organizationId(), id, request));
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        service.deactivate(currentUser.organizationId(), id);
        return ApiResponse.of(null);
    }
}
