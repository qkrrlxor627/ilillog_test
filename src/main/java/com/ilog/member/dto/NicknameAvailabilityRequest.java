package com.ilog.member.dto;

import com.ilog.global.policy.MemberPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameAvailabilityRequest(
        @Schema(example = "기택")
                @NotBlank
                @Size(min = MemberPolicy.NICKNAME_MIN, max = MemberPolicy.NICKNAME_MAX)
                String nickname) {}
