package com.ilog.hashtag.service;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.hashtag.dto.HashtagCreateRequest;
import com.ilog.hashtag.dto.HashtagResponse;
import com.ilog.hashtag.entity.PostHashtag;
import com.ilog.hashtag.repository.PostHashtagRepository;
import com.ilog.post.entity.Post;
import com.ilog.post.repository.PostRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HashtagService {

    private final PostRepository postRepository;
    private final PostHashtagRepository postHashtagRepository;

    /** 해시태그 등록. 게시글 작성자 본인만, 이미 있는 태그는 무시하고 해당 게시글의 전체 태그 목록을 반환한다. */
    @Transactional
    public List<HashtagResponse> addHashtags(
            Long postId, Long memberId, HashtagCreateRequest request) {
        List<String> names = HashtagNormalizer.normalizeForRegistration(request.names(), "names");
        Post post = getPost(postId);
        post.validateOwner(memberId);

        registerAll(post.getId(), names);
        return getHashtags(post.getId());
    }

    /**
     * 정규화된 태그들을 중복 무시로 저장한다. 게시글 등록에서도 재사용한다.
     *
     * <p>소유권 검사는 호출자 책임이다.
     */
    @Transactional
    public void registerAll(Long postId, Collection<String> normalizedNames) {
        normalizedNames.forEach(
                name -> postHashtagRepository.insertIgnoringDuplicate(postId, name));
    }

    public List<HashtagResponse> getHashtags(Long postId) {
        return postHashtagRepository.findAllByPostIdOrderByIdAsc(postId).stream()
                .map(HashtagResponse::from)
                .toList();
    }

    /** 해시태그 삭제. 다른 게시글 소속 태그면 404 (다른 게시글 태그 삭제 방지). */
    @Transactional
    public void deleteHashtag(Long postId, Long hashtagId, Long memberId) {
        Post post = getPost(postId);
        post.validateOwner(memberId);

        PostHashtag hashtag =
                postHashtagRepository
                        .findByIdAndPostId(hashtagId, post.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.HASHTAG_NOT_FOUND));
        postHashtagRepository.delete(hashtag);
    }

    private Post getPost(Long postId) {
        return postRepository
                .findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }
}
