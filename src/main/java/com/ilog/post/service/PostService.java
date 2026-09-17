package com.ilog.post.service;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.global.response.PageResponse;
import com.ilog.hashtag.entity.PostHashtag;
import com.ilog.hashtag.repository.PostHashtagRepository;
import com.ilog.hashtag.service.HashtagNormalizer;
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
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final HashtagService hashtagService;
    private final Clock clock;

    /** 게시글 등록. 작성자 = 토큰 회원, 작성일 = 서버 시각. */
    @Transactional
    public Long create(Long memberId, PostCreateRequest request) {
        List<String> hashtags =
                HashtagNormalizer.normalizeForRegistration(request.hashtags(), "hashtags");
        Member author =
                memberRepository
                        .findByIdAndDeletedAtIsNull(memberId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Post post =
                postRepository.save(
                        Post.create(author, request.title(), request.content(), request.urls()));
        hashtagService.registerAll(post.getId(), hashtags);
        return post.getId();
    }

    public PostDetailResponse getPost(Long postId, Long loginMemberId) {
        Post post = findPost(postId);
        return PostDetailResponse.of(post, hashtagService.getHashtags(postId), loginMemberId);
    }

    /** 목록 + 검색. 쿼리 수 고정: 본문(작성자 fetch join) 1 + count 1 + 태그 일괄 조회 1. */
    public PageResponse<PostSummaryResponse> search(PostSearchRequest request) {
        PostSearchCondition condition =
                new PostSearchCondition(
                        request.title(),
                        request.nickname(),
                        HashtagNormalizer.normalizeForSearch(request.hashtags()),
                        request.date(),
                        request.keyword());
        Page<Post> posts = postRepository.search(condition, request.toPageable());

        Map<Long, List<String>> hashtagsByPostId = findHashtagNames(posts.getContent());
        return PageResponse.from(
                posts.map(
                        post ->
                                PostSummaryResponse.of(
                                        post,
                                        hashtagsByPostId.getOrDefault(post.getId(), List.of()))));
    }

    /** 게시글 수정. 작성자 본인만, 이력 없이 덮어쓰고 updated_at 만 갱신. */
    @Transactional
    public PostDetailResponse update(Long postId, Long memberId, PostUpdateRequest request) {
        Post post = findPost(postId);
        post.validateOwner(memberId);

        post.update(request.title(), request.content(), request.urls(), clock.instant());
        return PostDetailResponse.of(post, hashtagService.getHashtags(postId), memberId);
    }

    /** 게시글 삭제. 작성자 본인만, 해시태그·URL 은 CASCADE 로 함께 삭제. */
    @Transactional
    public void delete(Long postId, Long memberId) {
        Post post = findPost(postId);
        post.validateOwner(memberId);
        remove(post);
    }

    /** TODO(D-07): 소프트/하드 삭제 확정 시 이 메서드만 교체. [잠정] 물리 삭제 */
    private void remove(Post post) {
        postRepository.delete(post);
    }

    private Post findPost(Long postId) {
        return postRepository
                .findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    private Map<Long, List<String>> findHashtagNames(List<Post> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return postHashtagRepository.findAllByPostIdInOrderByIdAsc(postIds).stream()
                .collect(
                        Collectors.groupingBy(
                                PostHashtag::getPostId,
                                Collectors.mapping(PostHashtag::getName, Collectors.toList())));
    }
}
