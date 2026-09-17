package com.ilog.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청.
 *
 * <p>확정된 처리 순서(현재 비밀번호 확인 → 정규식 → 확인 일치)를 지키기 위해 정규식은 DTO 가 아니라 서비스에서 검사한다.
 */
public record PasswordChangeRequest(
        @Schema(example = "password1!") @NotBlank String currentPassword,
        @Schema(example = "newPassword1!") @NotBlank String newPassword,
        @Schema(example = "newPassword1!") @NotBlank String newPasswordConfirm) {

    @Override
    public String toString() {
        return "PasswordChangeRequest[****]";
    }
}
