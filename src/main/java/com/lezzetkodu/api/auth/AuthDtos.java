package com.lezzetkodu.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank
            @Size(min = 3, max = 50)
            @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Sadece harf, rakam, nokta, alt çizgi ve tire kullanılabilir")
            String username,

            @NotBlank @Email @Size(max = 255)
            String email,

            // Ust sinir 72: BCrypt 72 bayttan sonrasini dikkate almaz.
            @NotBlank @Size(min = 8, max = 72)
            String password
    ) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record AuthResponse(String accessToken, String tokenType, long expiresIn, String username) {
    }
}
