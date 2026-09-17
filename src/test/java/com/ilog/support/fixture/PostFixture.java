package com.ilog.support.fixture;

import com.ilog.member.entity.Member;
import com.ilog.post.entity.Post;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public final class PostFixture {

    private PostFixture() {}

    /** 저장 전 게시글. 리포지토리 테스트용. */
    public static Post create(Member author, String title) {
        return Post.create(author, title, "내용", List.of("https://ilog.com/1"));
    }

    /** 작성자 ID 만 지정한 식별자 있는 게시글. 단위 테스트용. */
    public static Post create(Long ownerId) {
        return create(10L, MemberFixture.create(ownerId));
    }

    public static Post create(Long postId, Member author) {
        Post post = Post.create(author, "제목", "내용", List.of("https://ilog.com/1"));
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
