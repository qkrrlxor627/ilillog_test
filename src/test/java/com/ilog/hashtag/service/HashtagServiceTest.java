package com.ilog.hashtag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.hashtag.dto.HashtagCreateRequest;
import com.ilog.hashtag.dto.HashtagResponse;
import com.ilog.hashtag.entity.PostHashtag;
import com.ilog.hashtag.repository.PostHashtagRepository;
import com.ilog.post.repository.PostRepository;
import com.ilog.support.fixture.HashtagFixture;
import com.ilog.support.fixture.PostFixture;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    @Mock PostRepository postRepository;
    @Mock PostHashtagRepository postHashtagRepository;
    @InjectMocks HashtagService hashtagService;

    @Test
    @DisplayName("작성자가 태그를 추가하면 정규화해 중복 무시로 저장하고 게시글의 전체 태그를 반환한다")
    void addHashtags_owner_insertsAndReturnsAll() {
        // given
        given(postRepository.findById(10L)).willReturn(Optional.of(PostFixture.create(1L)));
        given(postHashtagRepository.findAllByPostIdOrderByIdAsc(10L))
                .willReturn(
                        List.of(
                                HashtagFixture.create(3L, 10L, "JWT"),
                                HashtagFixture.create(4L, 10L, "Spring")));

        // when
        List<HashtagResponse> response =
                hashtagService.addHashtags(
                        10L, 1L, new HashtagCreateRequest(List.of("#JWT", "Spring")));

        // then
        then(postHashtagRepository).should().insertIgnoringDuplicate(10L, "JWT");
        then(postHashtagRepository).should().insertIgnoringDuplicate(10L, "Spring");
        assertThat(response)
                .containsExactly(new HashtagResponse(3L, "JWT"), new HashtagResponse(4L, "Spring"));
    }

    @Test
    @DisplayName("작성자가 아니면 태그를 저장하지 않고 POST_NOT_OWNER 예외가 발생한다")
    void addHashtags_notOwner_throws() {
        // given
        given(postRepository.findById(10L)).willReturn(Optional.of(PostFixture.create(1L)));

        // when & then
        assertThatThrownBy(
                        () ->
                                hashtagService.addHashtags(
                                        10L, 2L, new HashtagCreateRequest(List.of("JWT"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_OWNER);
        then(postHashtagRepository).should(never()).insertIgnoringDuplicate(anyLong(), anyString());
    }

    @Test
    @DisplayName("게시글이 없으면 POST_NOT_FOUND 예외가 발생한다")
    void addHashtags_postNotFound_throws() {
        // given
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                        () ->
                                hashtagService.addHashtags(
                                        99L, 1L, new HashtagCreateRequest(List.of("JWT"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자가 자기 게시글의 태그를 삭제한다")
    void deleteHashtag_owner_deletes() {
        // given
        PostHashtag hashtag = HashtagFixture.create(3L, 10L, "JWT");
        given(postRepository.findById(10L)).willReturn(Optional.of(PostFixture.create(1L)));
        given(postHashtagRepository.findByIdAndPostId(3L, 10L)).willReturn(Optional.of(hashtag));

        // when
        hashtagService.deleteHashtag(10L, 3L, 1L);

        // then
        then(postHashtagRepository).should().delete(hashtag);
    }

    @Test
    @DisplayName("태그가 해당 게시글 소속이 아니면 HASHTAG_NOT_FOUND 예외가 발생한다")
    void deleteHashtag_tagOfOtherPost_throwsNotFound() {
        // given
        given(postRepository.findById(10L)).willReturn(Optional.of(PostFixture.create(1L)));
        given(postHashtagRepository.findByIdAndPostId(3L, 10L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> hashtagService.deleteHashtag(10L, 3L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.HASHTAG_NOT_FOUND);
        then(postHashtagRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("작성자가 아니면 태그를 삭제하지 않고 POST_NOT_OWNER 예외가 발생한다")
    void deleteHashtag_notOwner_throws() {
        // given
        given(postRepository.findById(10L)).willReturn(Optional.of(PostFixture.create(1L)));

        // when & then
        assertThatThrownBy(() -> hashtagService.deleteHashtag(10L, 3L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_OWNER);
        then(postHashtagRepository).should(never()).findByIdAndPostId(anyLong(), anyLong());
    }
}
