package com.returnloop.identity.auth;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.common.web.ApiResponse;
import com.returnloop.security.AuthenticatedUser;
import com.returnloop.security.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and profile")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(authService.login(
                new AuthService.LoginRequest(request.email(), request.password())));
    }

    @PostMapping("/google")
    public ApiResponse<AuthService.LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ApiResponse.of(authService.loginWithGoogle(request.credential()));
    }

    @PostMapping("/register")
    public ApiResponse<AuthService.LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.of(authService.register(new AuthService.RegisterRequest(
                request.organizationName(), request.fullName(), request.email(), request.password())));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<AuthService.ForgotPasswordResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        return ApiResponse.of(authService.requestPasswordReset(request.email()));
    }

    @PostMapping("/verify-reset-code")
    public ApiResponse<MessageResponse> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        authService.verifyPasswordResetCode(request.email(), request.code());
        return ApiResponse.of(new MessageResponse("Reset code verified"));
    }

    @PostMapping("/reset-password")
    public ApiResponse<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.email(), request.code(), request.newPassword());
        return ApiResponse.of(new MessageResponse("Password updated successfully"));
    }

    @GetMapping("/me")
    public ApiResponse<AuthService.UserProfile> me() {
        AuthenticatedUser principal = currentUser.require();
        return ApiResponse.of(authService.profile(principal));
    }

    public record LoginRequest(
            @NotBlank(message = "email is required") @Email(message = "must be a valid email") String email,
            @NotBlank(message = "password is required") String password) {
    }

    public record RegisterRequest(@NotBlank @Size(max=160) String organizationName,
                                  @NotBlank @Size(max=160) String fullName,
                                  @NotBlank @Email @Size(max=254) String email,
                                  @NotBlank @Size(min=10,max=72) String password) { }
    public record GoogleLoginRequest(@NotBlank @Size(max=10000) String credential) { }
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) { }
    public record VerifyResetCodeRequest(@NotBlank @Email @Size(max=254) String email,
                                         @NotBlank @Pattern(regexp="\\d{6}") String code) { }
    public record ResetPasswordRequest(@NotBlank @Email @Size(max=254) String email,
                                       @NotBlank @Size(min=6,max=6) String code,
                                       @NotBlank @Size(min=10,max=72) String newPassword) { }
    public record MessageResponse(String message) { }
}
