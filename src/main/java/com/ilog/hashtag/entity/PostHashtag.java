package com.ilog.hashtag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글에 붙은 해시태그. 태그 마스터 테이블 없이 (post_id, tag_name) 유니크.
 *
 * <p>게시글 삭제 시 DB 의 ON DELETE CASCADE 로 함께 삭제된다.
 */
@Getter
@Entity
@Table(name = "post_hashtag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hashtag_id")
    private Long id;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "tag_name", nullable = false, length = 50, updatable = false)
    private String name;

    private PostHashtag(Long postId, String name) {
        this.postId = postId;
        this.name = name;
    }

    public static PostHashtag create(Long postId, String name) {
        return new PostHashtag(postId, name);
    }
}
