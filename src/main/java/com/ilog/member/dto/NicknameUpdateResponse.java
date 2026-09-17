package com.ilog.member.dto;

import com.ilog.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record NicknameUpdateResponse(
        @Schema(example = "새닉네임") String nickname,
        @Schema(example = "2026-09-17T10:30:00Z") Instant updatedAt) {

    public static NicknameUpdateResponse from(Member member) {
        return new NicknameUpdateResponse(member.getNickname(), member.getUpdatedAt());
    }
}
