package com.ilog.post.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.support.fixture.MemberFixture;
import com.ilog.support.fixture.PostFixture;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostTest {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @Test
    @DisplayName("URL 없이 등록하면 빈 목록이고 수정 시각은 없다")
    void create_withoutUrls_emptyUrls() {
        // when
        Post post = Post.create(MemberFixture.create(1L), "제목", "내용", null);

        // then
        assertThat(post.getUrls()).isEmpty();
        assertThat(post.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("null 인 필드는 유지하고 값이 있는 필드만 바꾸며 수정 시각을 갱신한다")
    void update_partialFields_keepsNullFields() {
        // given
        Post post = PostFixture.create(1L);

        // when
        post.update("새 제목", null, null, NOW);

        // then
        assertThat(post.getTitle()).isEqualTo("새 제목");
        assertThat(post.getContent()).isEqualTo("내용");
        assertThat(post.getUrls()).containsExactly("https://ilog.com/1");
        assertThat(post.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("urls 가 오면 순서를 유지해 전체 교체한다")
    void update_urls_replacesAll() {
        // given
        Post post = PostFixture.create(1L);

        // when
        post.update(null, null, List.of("https://b.com", "https://a.com"), NOW);

        // then
        assertThat(post.getUrls()).containsExactly("https://b.com", "https://a.com");
    }

    @Test
    @DisplayName("작성자 본인이면 소유권 검사를 통과한다")
    void validateOwner_owner_passes() {
        // given
        Post post = PostFixture.create(1L);

        // when & then
        assertThat(post.isOwner(1L)).isTrue();
        assertThatCode(() -> post.validateOwner(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("작성자가 아니면 POST_NOT_OWNER 예외가 발생한다")
    void validateOwner_notOwner_throws() {
        // given
        Post post = PostFixture.create(1L);

        // when & then
        assertThat(post.isOwner(2L)).isFalse();
        assertThatThrownBy(() -> post.validateOwner(2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_OWNER);
    }
}
