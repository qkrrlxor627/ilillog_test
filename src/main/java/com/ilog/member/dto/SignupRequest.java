package com.ilog.member.dto;

import com.ilog.global.policy.MemberPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// TODO(D-14): 이메일 인증 도입 여부 확정 시 조정
public record SignupRequest(
        @Schema(example = "a@b.com") @NotBlank @Email @Size(max = MemberPolicy.EMAIL_MAX)
                String email,
        @Schema(example = "password1!")
                @NotBlank
                @Pattern(
                        regexp = MemberPolicy.PASSWORD_REGEX,
                        message = MemberPolicy.PASSWORD_MESSAGE)
                String password,
        @Schema(example = "password1!") @NotBlank String passwordConfirm,
        @Schema(example = "박기택") @NotBlank @Size(max = MemberPolicy.NAME_MAX) String name,
        @Schema(example = "기택")
                @NotBlank
                @Size(min = MemberPolicy.NICKNAME_MIN, max = MemberPolicy.NICKNAME_MAX)
                String nickname) {

    @Override
    public String toString() {
        return "SignupRequest[email=" + email + ", nickname=" + nickname + "]";
    }
}
