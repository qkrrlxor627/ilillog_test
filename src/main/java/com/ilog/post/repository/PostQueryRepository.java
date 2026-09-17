package com.ilog.post.repository;

import com.ilog.post.entity.Post;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 게시글 목록 + 검색 동적 쿼리. */
public interface PostQueryRepository {

    /** 정렬 허용 필드 화이트리스트. 임의 컬럼 정렬 금지. TODO(D-05) */
    Set<String> SORTABLE_PROPERTIES = Set.of("createdAt", "title");

    /** 작성자(member)는 fetch join 으로 함께 조회한다. */
    Page<Post> search(PostSearchCondition condition, Pageable pageable);
}
