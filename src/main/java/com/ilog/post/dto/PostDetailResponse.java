package com.ilog.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ilog.hashtag.dto.HashtagResponse;
import com.ilog.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * 게시글 상세 (수정 응답도 같은 형태).
 *
 * @param isMine [제안] 수정·삭제 버튼 노출용
 */
public record PostDetailResponse(
        @Schema(example = "12") Long postId,
        @Schema(example = "JWT 인증 정리") String title,
        @Schema(example = "오늘은 JWT 를 공부했다.") String content,
        List<String> urls,
        List<HashtagResponse> hashtags,
        @Schema(example = "기택") String nickname,
        @Schema(example = "2026-09-17T10:30:00Z") Instant createdAt,
        @Schema(nullable = true) Instant updatedAt,
        @Schema(example = "true") @JsonProperty("isMine") boolean isMine) {

    public static PostDetailResponse of(
            Post post, List<HashtagResponse> hashtags, Long loginMemberId) {
        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                List.copyOf(post.getUrls()),
                hashtags,
                post.getMember().getNickname(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.isOwner(loginMemberId));
    }
}
