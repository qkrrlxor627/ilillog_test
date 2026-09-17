package com.ilog.hashtag.dto;

import com.ilog.hashtag.entity.PostHashtag;
import io.swagger.v3.oas.annotations.media.Schema;

public record HashtagResponse(
        @Schema(example = "3") Long hashtagId, @Schema(example = "JWT") String name) {

    public static HashtagResponse from(PostHashtag hashtag) {
        return new HashtagResponse(hashtag.getId(), hashtag.getName());
    }
}
