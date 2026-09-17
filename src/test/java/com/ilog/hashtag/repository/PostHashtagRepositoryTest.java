package com.ilog.hashtag.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ilog.hashtag.entity.PostHashtag;
import com.ilog.member.entity.Member;
import com.ilog.member.repository.MemberRepository;
import com.ilog.post.entity.Post;
import com.ilog.post.repository.PostRepository;
import com.ilog.support.RepositoryTestSupport;
import com.ilog.support.fixture.MemberFixture;
import com.ilog.support.fixture.PostFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PostHashtagRepositoryTest extends RepositoryTestSupport {

    @Autowired MemberRepository memberRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostHashtagRepository postHashtagRepository;
    @Autowired EntityManager entityManager;

    private Post post;
    private Post otherPost;

    @BeforeEach
    void setUp() {
        Member author = memberRepository.save(MemberFixture.create("a@b.com", "기택"));
        post = postRepository.save(PostFixture.create(author, "JWT"));
        otherPost = postRepository.save(PostFixture.create(author, "JPA"));
        entityManager.flush();
    }

    @Test
    @DisplayName("같은 게시글에 이미 있는 태그를 추가하면 무시한다")
    void insertIgnoringDuplicate_duplicate_ignored() {
        // given
        int first = postHashtagRepository.insertIgnoringDuplicate(post.getId(), "JWT");

        // when
        int second = postHashtagRepository.insertIgnoringDuplicate(post.getId(), "JWT");

        // then
        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(postHashtagRepository.findAllByPostIdOrderByIdAsc(post.getId()))
                .extracting(PostHashtag::getName)
                .containsExactly("JWT");
    }

    @Test
    @DisplayName("같은 이름이라도 다른 게시글이나 대소문자가 다르면 별개의 태그다")
    void insertIgnoringDuplicate_otherPostOrCase_inserted() {
        // when
        postHashtagRepository.insertIgnoringDuplicate(post.getId(), "JWT");
        postHashtagRepository.insertIgnoringDuplicate(post.getId(), "jwt");
        postHashtagRepository.insertIgnoringDuplicate(otherPost.getId(), "JWT");

        // then
        assertThat(postHashtagRepository.findAllByPostIdOrderByIdAsc(post.getId()))
                .extracting(PostHashtag::getName)
                .containsExactly("JWT", "jwt");
        assertThat(
                        postHashtagRepository.findAllByPostIdInOrderByIdAsc(
                                java.util.List.of(post.getId(), otherPost.getId())))
                .hasSize(3);
    }

    @Test
    @DisplayName("다른 게시글의 태그 ID 로는 조회되지 않는다")
    void findByIdAndPostId_tagOfOtherPost_empty() {
        // given
        postHashtagRepository.insertIgnoringDuplicate(otherPost.getId(), "JPA");
        Long otherTagId =
                postHashtagRepository
                        .findAllByPostIdOrderByIdAsc(otherPost.getId())
                        .getFirst()
                        .getId();

        // when & then
        assertThat(postHashtagRepository.findByIdAndPostId(otherTagId, post.getId())).isEmpty();
        assertThat(postHashtagRepository.findByIdAndPostId(otherTagId, otherPost.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("게시글을 삭제하면 해시태그와 URL 이 CASCADE 로 함께 삭제된다")
    void deletePost_cascadesHashtagsAndUrls() {
        // given
        postHashtagRepository.insertIgnoringDuplicate(post.getId(), "JWT");
        entityManager.clear();
        Post persisted = postRepository.findById(post.getId()).orElseThrow();

        // when
        postRepository.delete(persisted);
        entityManager.flush();

        // then
        assertThat(countRows("post_hashtag", post.getId())).isZero();
        assertThat(countRows("post_url", post.getId())).isZero();
        assertThat(postRepository.findById(post.getId())).isEmpty();
    }

    private long countRows(String table, Long postId) {
        return ((Number)
                        entityManager
                                .createNativeQuery(
                                        "SELECT count(*) FROM "
                                                + table
                                                + " WHERE post_id = :postId")
                                .setParameter("postId", postId)
                                .getSingleResult())
                .longValue();
    }
}
