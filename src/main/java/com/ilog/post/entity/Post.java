package com.ilog.post.entity;

import com.ilog.global.entity.BaseTimeEntity;
import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.member.entity.Member;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Getter
@Entity
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작성자 = 토큰의 회원. 변경 불가. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** post_url 테이블. sort_order 로 입력 순서를 유지하고, 수정 시 전체 교체한다. */
    @ElementCollection
    @CollectionTable(name = "post_url", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "url", nullable = false, columnDefinition = "text")
    private List<String> urls = new ArrayList<>();

    private Post(Member member, String title, String content, List<String> urls) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.urls.addAll(urls);
    }

    public static Post create(
            Member author, String title, String content, @Nullable List<String> urls) {
        return new Post(author, title, content, urls == null ? List.of() : urls);
    }

    /** PATCH 수정. null 인 필드는 변경하지 않고, urls 가 오면 전체 교체한다. 이력 없이 덮어쓰고 updated_at 만 갱신. */
    public void update(
            @Nullable String title,
            @Nullable String content,
            @Nullable List<String> urls,
            Instant now) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (urls != null) {
            this.urls.clear();
            this.urls.addAll(urls);
        }
        markUpdated(now);
    }

    public boolean isOwner(Long memberId) {
        return member.getId().equals(memberId);
    }

    public void validateOwner(Long memberId) {
        if (!isOwner(memberId)) {
            throw new BusinessException(ErrorCode.POST_NOT_OWNER);
        }
    }
}
