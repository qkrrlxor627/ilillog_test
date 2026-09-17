package com.ilog.post.dto;

import com.ilog.global.policy.MemberPolicy;
import com.ilog.global.policy.PostPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 게시글 목록 + 검색 query parameter. 조건이 없으면 전체 목록.
 *
 * <p>TODO(D-01~D-05): 검색 방식·정렬·페이징 확정 시 조정. [잠정] page 0 부터, size 기본 10 최대 50, sort 기본
 * createdAt,desc (허용 필드: createdAt, title)
 */
public record PostSearchRequest(
        @Schema(description = "제목 부분 일치") @Size(max = PostPolicy.TITLE_MAX) String title,
        @Schema(description = "작성자 닉네임 부분 일치") @Size(max = MemberPolicy.NICKNAME_MAX)
                String nickname,
        @Schema(description = "태그 이름 (복수는 OR)", example = "JWT")
                @Size(max = PostPolicy.HASHTAG_MAX_COUNT)
                List<String> hashtags,
        @Schema(description = "작성일 (YYYY-MM-DD)", example = "2026-09-17")
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate date,
        @Schema(description = "통합 검색어 (제목·닉네임·태그)") @Size(max = PostPolicy.TITLE_MAX)
                String keyword,
        @Schema(description = "페이지 (0부터)", example = "0") @PositiveOrZero Integer page,
        @Schema(description = "페이지 크기", example = "10") @Min(1) @Max(PostPolicy.PAGE_SIZE_MAX)
                Integer size,
        @Schema(description = "정렬 (createdAt|title),(asc|desc)", example = "createdAt,desc")
                @Pattern(
                        regexp = SORT_REGEX,
                        message = "정렬은 createdAt 또는 title 에 asc/desc 만 허용합니다.")
                String sort) {

    static final String SORT_REGEX = "^(createdAt|title)(,(?i)(asc|desc))?$";

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    public static PostSearchRequest empty() {
        return new PostSearchRequest(null, null, null, null, null, null, null, null);
    }

    public Pageable toPageable() {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? PostPolicy.PAGE_SIZE_DEFAULT : size;
        return PageRequest.of(pageNumber, pageSize, toSort(sort));
    }

    private static Sort toSort(@Nullable String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_SORT;
        }
        String[] parts = sort.split(",");
        Sort.Direction direction =
                parts.length > 1
                        ? Sort.Direction.fromString(parts[1].toUpperCase(Locale.ROOT))
                        : Sort.Direction.DESC;
        return Sort.by(direction, parts[0]);
    }
}
