package com.ilog.hashtag.repository;

import com.ilog.hashtag.entity.PostHashtag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    /**
     * 같은 게시글에 이미 있는 태그는 무시한다 [확정]. 유니크 제약 기반이라 동시 요청에도 안전하다.
     *
     * @return 실제로 추가된 행 수 (이미 있으면 0)
     */
    @Modifying
    @Query(
            value =
                    """
                    INSERT INTO post_hashtag (post_id, tag_name)
                    VALUES (:postId, :name)
                    ON CONFLICT (post_id, tag_name) DO NOTHING
                    """,
            nativeQuery = true)
    int insertIgnoringDuplicate(@Param("postId") Long postId, @Param("name") String name);

    List<PostHashtag> findAllByPostIdOrderByIdAsc(Long postId);

    List<PostHashtag> findAllByPostIdInOrderByIdAsc(Collection<Long> postIds);

    Optional<PostHashtag> findByIdAndPostId(Long id, Long postId);
}
