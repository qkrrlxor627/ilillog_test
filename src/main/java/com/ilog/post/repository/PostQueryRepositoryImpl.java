package com.ilog.post.repository;

import com.ilog.global.policy.PostPolicy;
import com.ilog.hashtag.entity.PostHashtag;
import com.ilog.member.entity.Member;
import com.ilog.post.entity.Post;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

/**
 * Criteria API 기반 검색 구현.
 *
 * <p>[잠정] 매칭 규칙: title·nickname 대소문자 무시 부분 일치, 조건 간 AND, hashtags 는 태그 간 OR (TODO(D-03),
 * TODO(D-04)). keyword 는 title OR nickname OR 태그명 부분 일치 (TODO(D-01)). URL·content 는 검색 대상 아님 [확정].
 */
@RequiredArgsConstructor
public class PostQueryRepositoryImpl implements PostQueryRepository {

    private static final char LIKE_ESCAPE = '\\';

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Post> search(PostSearchCondition condition, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Post> contentQuery = cb.createQuery(Post.class);
        Root<Post> post = contentQuery.from(Post.class);
        Join<Post, Member> member = (Join<Post, Member>) post.<Post, Member>fetch("member");
        contentQuery
                .select(post)
                .where(predicates(cb, contentQuery, post, member, condition))
                .orderBy(orders(cb, post, pageable.getSort()));

        List<Post> content =
                entityManager
                        .createQuery(contentQuery)
                        .setFirstResult((int) pageable.getOffset())
                        .setMaxResults(pageable.getPageSize())
                        .getResultList();

        return PageableExecutionUtils.getPage(content, pageable, () -> count(cb, condition));
    }

    private long count(CriteriaBuilder cb, PostSearchCondition condition) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Post> post = countQuery.from(Post.class);
        Join<Post, Member> member = post.join("member", JoinType.INNER);
        countQuery
                .select(cb.count(post))
                .where(predicates(cb, countQuery, post, member, condition));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private static Predicate[] predicates(
            CriteriaBuilder cb,
            AbstractQuery<?> query,
            Root<Post> post,
            Join<Post, Member> member,
            PostSearchCondition condition) {
        List<Predicate> predicates = new ArrayList<>();

        if (condition.title() != null) {
            predicates.add(containsIgnoreCase(cb, post.get("title"), condition.title()));
        }
        if (condition.nickname() != null) {
            predicates.add(containsIgnoreCase(cb, member.get("nickname"), condition.nickname()));
        }
        if (!condition.hashtags().isEmpty()) {
            Subquery<Long> tagged = query.subquery(Long.class);
            Root<PostHashtag> hashtag = tagged.from(PostHashtag.class);
            tagged.select(hashtag.get("id"))
                    .where(
                            cb.equal(hashtag.get("postId"), post.get("id")),
                            hashtag.get("name").in(condition.hashtags()));
            predicates.add(cb.exists(tagged));
        }
        if (condition.date() != null) {
            LocalDate date = condition.date();
            Instant start = date.atStartOfDay(PostPolicy.SEARCH_ZONE).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(PostPolicy.SEARCH_ZONE).toInstant();
            Expression<Instant> createdAt = post.get("createdAt");
            predicates.add(cb.greaterThanOrEqualTo(createdAt, start));
            predicates.add(cb.lessThan(createdAt, end));
        }
        if (condition.keyword() != null) {
            String keyword = condition.keyword();
            Subquery<Long> tagged = query.subquery(Long.class);
            Root<PostHashtag> hashtag = tagged.from(PostHashtag.class);
            tagged.select(hashtag.get("id"))
                    .where(
                            cb.equal(hashtag.get("postId"), post.get("id")),
                            containsIgnoreCase(cb, hashtag.get("name"), keyword));
            predicates.add(
                    cb.or(
                            containsIgnoreCase(cb, post.get("title"), keyword),
                            containsIgnoreCase(cb, member.get("nickname"), keyword),
                            cb.exists(tagged)));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private static List<Order> orders(CriteriaBuilder cb, Root<Post> post, Sort sort) {
        List<Order> orders = new ArrayList<>();
        Sort effective = sort.isSorted() ? sort : Sort.by(Sort.Direction.DESC, "createdAt");
        for (Sort.Order order : effective) {
            if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
                throw new IllegalArgumentException("정렬할 수 없는 필드입니다: " + order.getProperty());
            }
            Expression<?> path = post.get(order.getProperty());
            orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        // 페이지 간 순서 안정성을 위한 타이브레이커
        orders.add(cb.desc(post.get("id")));
        return orders;
    }

    private static Predicate containsIgnoreCase(
            CriteriaBuilder cb, Expression<String> path, String value) {
        String pattern = "%" + escapeLike(value.toLowerCase(Locale.ROOT)) + "%";
        return cb.like(cb.lower(path), pattern, LIKE_ESCAPE);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
