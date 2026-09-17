package com.ilog.hashtag.dto;

import com.ilog.global.policy.PostPolicy;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

// TODO(D-06, D-09): 해시태그 생성 중복 허용 여부·최대 개수 확정 시 조정
public record HashtagCreateRequest(
        @ArraySchema(schema = @Schema(example = "JWT"))
                @NotEmpty
                @Size(max = PostPolicy.HASHTAG_MAX_COUNT)
                List<@NotBlank String> names) {}
