package com.ilog.auth.dto;

import com.ilog.global.policy.MemberPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemporaryPasswordRequest(
        @Schema(example = "a@b.com") @NotBlank @Email String email,
        @Schema(example = "박기택") @NotBlank @Size(max = MemberPolicy.NAME_MAX) String name) {}
