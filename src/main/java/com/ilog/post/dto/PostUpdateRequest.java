package com.ilog.post.dto;

import com.ilog.global.policy.PostPolicy;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 게시글 수정(PATCH). null 인 필드는 변경하지 않는다. urls 가 오면 전체 교체.
 *
 * <p>TODO(D-06): 수정 가능 범위 확정 시 조정. [잠정] 해시태그는 해시태그 API 로만 추가/삭제하므로 요청의 hashtags 는 무시한다.
 */
public record PostUpdateRequest(
        @Schema(example = "새 제목", nullable = true)
                @Pattern(regexp = NOT_BLANK_IF_PRESENT, message = "공백일 수 없습니다.")
                @Size(max = PostPolicy.TITLE_MAX)
                String title,
        @Schema(example = "수정한 내용", nullable = true)
                @Pattern(regexp = NOT_BLANK_IF_PRESENT, message = "공백일 수 없습니다.")
                String content,
        @ArraySchema(schema = @Schema(example = "https://docs.spring.io"))
                @Size(max = PostPolicy.URL_MAX_COUNT)
                List<
                                @NotBlank @Size(max = PostPolicy.URL_MAX_LENGTH)
                                @Pattern(
                                        regexp = PostPolicy.URL_REGEX,
                                        message = "http(s) URL 형식이어야 합니다.")
                                String>
                        urls) {

    /** null 은 통과(@Pattern 은 null 을 검사하지 않음), 값이 있으면 공백이 아닌 문자가 하나 이상. */
    static final String NOT_BLANK_IF_PRESENT = "(?s).*\\S.*";
}
