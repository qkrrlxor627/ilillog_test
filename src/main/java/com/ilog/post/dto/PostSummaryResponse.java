package com.ilog.post.dto;

import com.ilog.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * 게시글 목록 항목. 제목·작성자 닉네임·작성일 [확정].
 *
 * <p>TODO(D-05): 목록 노출 항목 확정 시 조정. [잠정] hashtags 포함
 */
public record PostSummaryResponse(
        @Schema(example = "12") Long postId,
        @Schema(example = "JWT 인증 정리") String title,
        @Schema(example = "기택") String nickname,
        @Schema(example = "2026-09-17T10:30:00Z") Instant createdAt,
        List<String> hashtags) {

    public static PostSummaryResponse of(Post post, List<String> hashtags) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getMember().getNickname(),
                post.getCreatedAt(),
                hashtags);
    }
}
