package com.ilog.global.response;

import java.util.List;
import org.springframework.data.domain.Page;

/** Spring {@link Page} 를 그대로 직렬화하지 않기 위한 페이지 응답. */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
