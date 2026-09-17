package com.ilog.member.dto;

import com.ilog.global.policy.MemberPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 닉네임만 수정 가능. 이메일·이름 필드는 두지 않는다. */
public record NicknameUpdateRequest(
        @Schema(example = "새닉네임")
                @NotBlank
                @Size(min = MemberPolicy.NICKNAME_MIN, max = MemberPolicy.NICKNAME_MAX)
                String nickname) {}
