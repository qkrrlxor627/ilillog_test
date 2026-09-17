package com.ilog.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.global.response.PageResponse;
import com.ilog.hashtag.dto.HashtagResponse;
import com.ilog.hashtag.repository.PostHashtagRepository;
import com.ilog.hashtag.service.HashtagService;
import com.ilog.member.entity.Member;
import com.ilog.member.repository.MemberRepository;
import com.ilog.post.dto.PostCreateRequest;
import com.ilog.post.dto.PostDetailResponse;
import com.ilog.post.dto.PostSearchRequest;
import com.ilog.post.dto.PostSummaryResponse;
import com.ilog.post.dto.PostUpdateRequest;
import com.ilog.post.entity.Post;
import com.ilog.post.repository.PostRepository;
import com.ilog.post.repository.PostSearchCondition;
import com.ilog.support.fixture.HashtagFixture;
import com.ilog.support.fixture.MemberFixture;
import com.ilog.support.fixture.PostFixture;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @Mock PostRepository postRepository;
    @Mock MemberRepository memberRepository;
    @Mock PostHashtagRepository postHashtagRepository;
    @Mock HashtagService hashtagService;

    PostService postService;

    @BeforeEach
    void setUp() {
        postService =
                new PostService(
                        postRepository,
                        memberRepository,
                        postHashtagRepository,
                        hashtagService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("게시글을 등록하면 작성자를 토큰 회원으로 저장하고 정규화한 해시태그를 등록한다")
    void create_valid_savesPostAndHashtags() {
        // given
        Member author = MemberFixture.create(1L);
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class)))
                .willAnswer(
                        invocation -> {
                            Post post = invocation.getArgument(0);
                            ReflectionTestUtils.setField(post, "id", 12L);
                            return post;
                        });
        PostCreateRequest request =
                new PostCreateRequest(
                        "제목", "내용", List.of("https://a.com"), List.of("#JWT", " Spring ", "JWT"));

        // when
        Long postId = postService.create(1L, request);

        // then
        assertThat(postId).isEqualTo(12L);
        then(hashtagService).should().registerAll(12L, List.of("JWT", "Spring"));
    }

    @Test
    @DisplayName("정규화 후 비어 있는 해시태그가 있으면 저장하지 않고 INVALID_INPUT 예외가 발생한다")
    void create_blankHashtagAfterNormalize_throws() {
        // given
        PostCreateRequest request = new PostCreateRequest("제목", "내용", null, List.of("#"));

        // when & then
        assertThatThrownBy(() -> postService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
        then(postRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("작성자 회원이 없으면 MEMBER_NOT_FOUND 예외가 발생한다")
    void create_memberNotFound_throws() {
        // given
        given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () -> postService.create(1L, new PostCreateRequest("제목", "내용", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("상세 조회 시 본인 글이면 isMine 이 true 다")
    void getPost_owner_isMineTrue() {
        // given
        Post post = PostFixture.create(1L);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(hashtagService.getHashtags(10L)).willReturn(List.of(new HashtagResponse(3L, "JWT")));

        // when
        PostDetailResponse response = postService.getPost(10L, 1L);

        // then
        assertThat(response.postId()).isEqualTo(10L);
        assertThat(response.isMine()).isTrue();
        assertThat(response.hashtags()).containsExactly(new HashtagResponse(3L, "JWT"));
        assertThat(response.urls()).containsExactly("https://ilog.com/1");
    }

    @Test
    @DisplayName("상세 조회 시 타인 글이면 isMine 이 false 다")
    void getPost_otherMember_isMineFalse() {
        // given
        given(postRepository.findById(10L)).willReturn(Optional.of(PostFixture.create(1L)));
        given(hashtagService.getHashtags(10L)).willReturn(List.of());

        // when & then
        assertThat(postService.getPost(10L, 2L).isMine()).isFalse();
    }

    @Test
    @DisplayName("없는 게시글을 조회하면 POST_NOT_FOUND 예외가 발생한다")
    void getPost_notFound_throws() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.getPost(99L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("목록 조회는 해시태그를 게시글 ID 로 한 번에 조회해 매핑하고 검색 태그를 정규화한다")
    void search_mapsHashtagsInSingleQuery() {
        // given
        Member author = MemberFixture.create(1L);
        Post first = PostFixture.create(10L, author);
        Post second = PostFixture.create(11L, author);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(first, second), pageable, 2);
        ArgumentCaptor<PostSearchCondition> condition =
                ArgumentCaptor.forClass(PostSearchCondition.class);
        given(postRepository.search(condition.capture(), any(Pageable.class))).willReturn(page);
        given(postHashtagRepository.findAllByPostIdInOrderByIdAsc(List.of(10L, 11L)))
                .willReturn(
                        List.of(
                                HashtagFixture.create(1L, 10L, "JWT"),
                                HashtagFixture.create(2L, 10L, "Spring")));
        PostSearchRequest request =
                new PostSearchRequest(" 제목 ", null, List.of("#JWT", ""), null, null, 0, 10, null);

        // when
        PageResponse<PostSummaryResponse> response = postService.search(request);

        // then
        assertThat(condition.getValue().title()).isEqualTo("제목");
        assertThat(condition.getValue().hashtags()).containsExactly("JWT");
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.content())
                .extracting(PostSummaryResponse::postId, PostSummaryResponse::hashtags)
                .containsExactly(tuple(10L, List.of("JWT", "Spring")), tuple(11L, List.of()));
    }

    @Test
    @DisplayName("결과가 없으면 해시태그 조회를 하지 않는다")
    void search_emptyPage_skipsHashtagQuery() {
        // given
        given(postRepository.search(any(), any())).willReturn(Page.empty(PageRequest.of(0, 10)));

        // when
        PageResponse<PostSummaryResponse> response = postService.search(PostSearchRequest.empty());

        // then
        assertThat(response.content()).isEmpty();
        then(postHashtagRepository).should(never()).findAllByPostIdInOrderByIdAsc(anyCollection());
    }

    @Test
    @DisplayName("작성자가 아닌 회원이 수정하면 POST_NOT_OWNER 예외가 발생한다")
    void updatePost_notOwner_throws() {
        // given
        Post post = PostFixture.create(1L /* ownerId */);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when & then
        assertThatThrownBy(
                        () ->
                                postService.update(
                                        10L, 2L, new PostUpdateRequest("새 제목", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_OWNER);
        assertThat(post.getTitle()).isEqualTo("제목");
    }

    @Test
    @DisplayName("작성자가 수정하면 변경된 상세 응답과 수정 시각을 반환한다")
    void updatePost_owner_returnsUpdatedDetail() {
        // given
        Post post = PostFixture.create(1L);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(hashtagService.getHashtags(10L)).willReturn(List.of());

        // when
        PostDetailResponse response =
                postService.update(
                        10L, 1L, new PostUpdateRequest(null, "새 내용", List.of("https://new.com")));

        // then
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("새 내용");
        assertThat(response.urls()).containsExactly("https://new.com");
        assertThat(response.updatedAt()).isEqualTo(NOW);
        assertThat(response.isMine()).isTrue();
    }

    @Test
    @DisplayName("없는 게시글을 수정하면 POST_NOT_FOUND 예외가 발생한다")
    void updatePost_notFound_throws() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () -> postService.update(99L, 1L, new PostUpdateRequest("t", null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 삭제하면 게시글을 삭제한다")
    void deletePost_owner_deletes() {
        // given
        Post post = PostFixture.create(1L);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        // when
        postService.delete(10L, 1L);

        // then
        then(postRepository).should().delete(post);
    }

    @Test
    @DisplayName("작성자가 아니면 삭제하지 않고 POST_NOT_OWNER 예외가 발생한다")
    void deletePost_notOwner_throws() {
        // given
        given(postRepository.findById(10L)).willReturn(Optional.of(PostFixture.create(1L)));

        // when & then
        assertThatThrownBy(() -> postService.delete(10L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_OWNER);
        then(postRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("없는 게시글을 삭제하면 POST_NOT_FOUND 예외가 발생한다")
    void deletePost_notFound_throws() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.delete(99L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }
}
