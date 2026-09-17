package com.ilog.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "a@b.com") @NotBlank @Email String email,
        @Schema(example = "password1!") @NotBlank String password) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=****]";
    }
}
