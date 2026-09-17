package com.ilog.member.dto;

import com.ilog.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record MemberInfoResponse(
        @Schema(example = "a@b.com") String email,
        @Schema(example = "박기택") String name,
        @Schema(example = "기택") String nickname,
        @Schema(example = "2026-09-17T10:30:00Z") Instant createdAt,
        @Schema(example = "2026-09-18T10:30:00Z") Instant updatedAt) {

    public static MemberInfoResponse from(Member member) {
        return new MemberInfoResponse(
                member.getEmail(),
                member.getName(),
                member.getNickname(),
                member.getCreatedAt(),
                member.getUpdatedAt());
    }
}
