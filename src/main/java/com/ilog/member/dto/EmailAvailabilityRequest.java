package com.ilog.member.dto;

import com.ilog.global.policy.MemberPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailAvailabilityRequest(
        @Schema(example = "a@b.com") @NotBlank @Email @Size(max = MemberPolicy.EMAIL_MAX)
                String email) {}
