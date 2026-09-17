package com.ilog.hashtag.service;

import com.ilog.global.error.BusinessException;
import com.ilog.global.policy.PostPolicy;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * 해시태그 이름 정규화.
 *
 * <p>TODO(D-09): 표기 규칙 확정 시 조정. [잠정] 앞뒤 공백 제거 → 앞의 {@code #} 제거 → 다시 trim, 대소문자는 그대로
 */
public final class HashtagNormalizer {

    private HashtagNormalizer() {}

    public static String normalizeName(String raw) {
        String name = raw.strip();
        int start = 0;
        while (start < name.length() && name.charAt(start) == '#') {
            start++;
        }
        return name.substring(start).strip();
    }

    /**
     * 등록용 정규화. 요청 안의 중복은 하나로 합치고, 정규화 후 비었거나 너무 길면 400.
     *
     * @param field 에러 응답의 fieldErrors 에 쓸 필드명
     */
    public static List<String> normalizeForRegistration(
            @Nullable Collection<String> rawNames, String field) {
        if (rawNames == null) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (String raw : rawNames) {
            String name = raw == null ? "" : normalizeName(raw);
            if (name.isEmpty()) {
                throw BusinessException.invalidField(field, "해시태그는 비어 있을 수 없습니다.");
            }
            if (name.length() > PostPolicy.HASHTAG_NAME_MAX) {
                throw BusinessException.invalidField(
                        field, "해시태그는 " + PostPolicy.HASHTAG_NAME_MAX + "자 이하여야 합니다.");
            }
            names.add(name);
        }
        return List.copyOf(names);
    }

    /** 검색용 정규화. 비어 있는 값은 조용히 무시한다. */
    public static List<String> normalizeForSearch(@Nullable Collection<String> rawNames) {
        if (rawNames == null) {
            return List.of();
        }
        return rawNames.stream()
                .filter(Objects::nonNull)
                .map(HashtagNormalizer::normalizeName)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }
}
