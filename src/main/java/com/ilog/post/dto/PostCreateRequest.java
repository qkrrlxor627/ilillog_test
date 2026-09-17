package com.ilog.post.dto;

import com.ilog.global.policy.PostPolicy;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 게시글 등록. 작성자·작성일은 요청으로 받지 않는다 [확정]. TODO(D-08), TODO(D-09) */
public record PostCreateRequest(
        @Schema(example = "JWT 인증 정리") @NotBlank @Size(max = PostPolicy.TITLE_MAX) String title,
        @Schema(example = "오늘은 JWT 를 공부했다.") @NotBlank String content,
        @ArraySchema(schema = @Schema(example = "https://docs.spring.io"))
                @Size(max = PostPolicy.URL_MAX_COUNT)
                List<
                                @NotBlank @Size(max = PostPolicy.URL_MAX_LENGTH)
                                @Pattern(
                                        regexp = PostPolicy.URL_REGEX,
                                        message = "http(s) URL 형식이어야 합니다.")
                                String>
                        urls,
        @ArraySchema(schema = @Schema(example = "JWT")) @Size(max = PostPolicy.HASHTAG_MAX_COUNT)
                List<@NotBlank String> hashtags) {}
