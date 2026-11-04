package com.restaurant.dto;

import com.restaurant.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 80) String fullName,
            @Email @NotBlank @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Enter a 10-digit phone number.") String phone
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record UserResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            UserRole role,
            Long branchId,
            String branchName
    ) {
    }

    public record AuthResult(UserResponse user, String token) {
    }
}
