package com.ilog.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ilog.hashtag.repository.PostHashtagRepository;
import com.ilog.member.entity.Member;
import com.ilog.member.repository.MemberRepository;
import com.ilog.post.entity.Post;
import com.ilog.support.RepositoryTestSupport;
import com.ilog.support.fixture.MemberFixture;
import com.ilog.support.fixture.PostFixture;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PostRepositoryTest extends RepositoryTestSupport {

    private static final Pageable LATEST =
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Autowired MemberRepository memberRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostHashtagRepository postHashtagRepository;
    @Autowired EntityManager entityManager;

    private Post jwtPost;
    private Post jpaPost;
    private Post testPost;

    @BeforeEach
    void setUp() {
        Member gitaek = memberRepository.save(MemberFixture.create("gitaek@ilog.com", "기택"));
        Member tester = memberRepository.save(MemberFixture.create("tester@ilog.com", "Tester"));

        // KST 2026-09-17 10:00
        jwtPost = savePost(gitaek, "JWT 인증 정리", "2026-09-17T01:00:00Z", "JWT", "Spring");
        // KST 2026-09-17 00:30
        jpaPost = savePost(gitaek, "JPA N+1 해결", "2026-09-16T15:30:00Z", "JPA");
        // KST 2026-09-16 23:59
        testPost = savePost(tester, "100% 테스트_커버리지", "2026-09-16T14:59:00Z", "Test");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("조건이 없으면 전체 목록을 작성일 내림차순으로 반환하고 작성자를 함께 조회한다")
    void search_noCondition_returnsAllLatestFirst() {
        // when
        Page<Post> page = postRepository.search(PostSearchCondition.empty(), LATEST);

        // then
        assertThat(ids(page)).containsExactly(jwtPost.getId(), jpaPost.getId(), testPost.getId());
        assertThat(page.getContent().getFirst().getMember().getNickname()).isEqualTo("기택");
    }

    @Test
    @DisplayName("제목은 대소문자를 무시한 부분 일치로 검색한다")
    void search_title_partialIgnoreCase() {
        // when
        Page<Post> page =
                postRepository.search(condition("jwt", null, List.of(), null, null), LATEST);

        // then
        assertThat(ids(page)).containsExactly(jwtPost.getId());
    }

    @Test
    @DisplayName("작성자 닉네임 부분 일치로 검색한다 (내 글 모아보기)")
    void search_nickname_partial() {
        // when
        Page<Post> page =
                postRepository.search(condition(null, "기", List.of(), null, null), LATEST);

        // then
        assertThat(ids(page)).containsExactly(jwtPost.getId(), jpaPost.getId());
    }

    @Test
    @DisplayName("해시태그 여러 개는 OR 로 검색하고 대소문자를 구분한다")
    void search_hashtags_orAndCaseSensitive() {
        // when
        Page<Post> or =
                postRepository.search(
                        condition(null, null, List.of("JWT", "JPA"), null, null), LATEST);
        Page<Post> lowerCase =
                postRepository.search(condition(null, null, List.of("jwt"), null, null), LATEST);

        // then
        assertThat(ids(or)).containsExactly(jwtPost.getId(), jpaPost.getId());
        assertThat(lowerCase.getContent()).isEmpty();
    }

    @Test
    @DisplayName("작성일은 서비스 기준 시간대(KST)의 하루로 검색한다")
    void search_date_kstDay() {
        // when
        Page<Post> page =
                postRepository.search(
                        condition(null, null, List.of(), LocalDate.of(2026, 9, 17), null), LATEST);

        // then
        assertThat(ids(page)).containsExactly(jwtPost.getId(), jpaPost.getId());
    }

    @Test
    @DisplayName("통합 검색어는 제목·닉네임·태그 중 하나라도 부분 일치하면 검색된다 (내용·URL 제외)")
    void search_keyword_matchesTitleNicknameOrHashtag() {
        // when
        Page<Post> byNicknameOrTag =
                postRepository.search(condition(null, null, List.of(), null, "test"), LATEST);
        Page<Post> byTitle =
                postRepository.search(condition(null, null, List.of(), null, "정리"), LATEST);
        Page<Post> byContent =
                postRepository.search(condition(null, null, List.of(), null, "내용"), LATEST);

        // then
        assertThat(ids(byNicknameOrTag)).containsExactly(testPost.getId());
        assertThat(ids(byTitle)).containsExactly(jwtPost.getId());
        assertThat(byContent.getContent()).isEmpty();
    }

    @Test
    @DisplayName("여러 조건은 AND 로 결합한다")
    void search_combinedConditions_and() {
        // when
        Page<Post> page =
                postRepository.search(
                        condition(null, "기택", List.of("JPA", "Test"), null, null), LATEST);

        // then
        assertThat(ids(page)).containsExactly(jpaPost.getId());
    }

    @Test
    @DisplayName("검색어의 % 와 _ 는 와일드카드가 아니라 문자로 취급한다")
    void search_likeWildcards_escaped() {
        // when
        Page<Post> percent =
                postRepository.search(condition("%", null, List.of(), null, null), LATEST);
        Page<Post> underscore =
                postRepository.search(condition("_", null, List.of(), null, null), LATEST);

        // then
        assertThat(ids(percent)).containsExactly(testPost.getId());
        assertThat(ids(underscore)).containsExactly(testPost.getId());
    }

    @Test
    @DisplayName("페이지 크기만큼 반환하고 전체 개수와 다음 페이지 여부를 계산한다")
    void search_paging_countsTotal() {
        // when
        Page<Post> first =
                postRepository.search(
                        PostSearchCondition.empty(), PageRequest.of(0, 2, LATEST.getSort()));
        Page<Post> second =
                postRepository.search(
                        PostSearchCondition.empty(), PageRequest.of(1, 2, LATEST.getSort()));

        // then
        assertThat(ids(first)).containsExactly(jwtPost.getId(), jpaPost.getId());
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.hasNext()).isTrue();
        assertThat(ids(second)).containsExactly(testPost.getId());
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    @DisplayName("제목 오름차순 정렬을 지원한다")
    void search_sortByTitleAsc() {
        // when
        Page<Post> page =
                postRepository.search(
                        PostSearchCondition.empty(),
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")));

        // then
        assertThat(page.getContent())
                .extracting(Post::getTitle)
                .containsExactly("100% 테스트_커버리지", "JPA N+1 해결", "JWT 인증 정리");
    }

    @Test
    @DisplayName("화이트리스트에 없는 필드로 정렬하면 예외가 발생한다")
    void search_unsupportedSort_throws() {
        // when & then
        assertThatThrownBy(
                        () ->
                                postRepository.search(
                                        PostSearchCondition.empty(),
                                        PageRequest.of(0, 10, Sort.by("content"))))
                .isInstanceOfAny(
                        IllegalArgumentException.class, InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("content");
    }

    @Test
    @DisplayName("URL 은 입력 순서대로 저장되고 수정 시 전체 교체된다")
    void urls_keepOrderAndReplaceAll() {
        // given
        Post post = postRepository.findById(jwtPost.getId()).orElseThrow();
        post.update(
                null,
                null,
                List.of("https://b.com", "https://a.com", "https://c.com"),
                Instant.now());
        entityManager.flush();
        entityManager.clear();

        // when
        Post reloaded = postRepository.findById(jwtPost.getId()).orElseThrow();

        // then
        assertThat(reloaded.getUrls())
                .containsExactly("https://b.com", "https://a.com", "https://c.com");
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    private Post savePost(Member author, String title, String createdAt, String... hashtags) {
        Post post = postRepository.save(PostFixture.create(author, title));
        entityManager.flush();
        entityManager
                .createNativeQuery("UPDATE post SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", Instant.parse(createdAt))
                .setParameter("id", post.getId())
                .executeUpdate();
        for (String hashtag : hashtags) {
            postHashtagRepository.insertIgnoringDuplicate(post.getId(), hashtag);
        }
        return post;
    }

    private static PostSearchCondition condition(
            String title, String nickname, List<String> hashtags, LocalDate date, String keyword) {
        return new PostSearchCondition(title, nickname, hashtags, date, keyword);
    }

    private static List<Long> ids(Page<Post> page) {
        return page.getContent().stream().map(Post::getId).toList();
    }
}
