package com.ilog.post.repository;

import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * 게시글 검색 조건. 값이 없는 조건은 무시하고, 모두 비어 있으면 전체 목록.
 *
 * @param title 제목 부분 일치
 * @param nickname 작성자 닉네임 부분 일치
 * @param hashtags 태그 이름 (복수는 OR)
 * @param date 작성일 (서비스 기준 시간대의 하루)
 * @param keyword 통합 검색어
 */
public record PostSearchCondition(
        @Nullable String title,
        @Nullable String nickname,
        List<String> hashtags,
        @Nullable LocalDate date,
        @Nullable String keyword) {

    public PostSearchCondition {
        title = blankToNull(title);
        nickname = blankToNull(nickname);
        hashtags = hashtags == null ? List.of() : List.copyOf(hashtags);
        keyword = blankToNull(keyword);
    }

    public static PostSearchCondition empty() {
        return new PostSearchCondition(null, null, List.of(), null, null);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
