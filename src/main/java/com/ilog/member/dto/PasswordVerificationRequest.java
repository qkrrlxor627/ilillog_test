package com.ilog.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PasswordVerificationRequest(
        @Schema(example = "password1!") @NotBlank String password) {

    @Override
    public String toString() {
        return "PasswordVerificationRequest[****]";
    }
}
