package com.ilog.support.fixture;

import com.ilog.hashtag.entity.PostHashtag;
import org.springframework.test.util.ReflectionTestUtils;

public final class HashtagFixture {

    private HashtagFixture() {}

    public static PostHashtag create(Long hashtagId, Long postId, String name) {
        PostHashtag hashtag = PostHashtag.create(postId, name);
        ReflectionTestUtils.setField(hashtag, "id", hashtagId);
        return hashtag;
    }
}
