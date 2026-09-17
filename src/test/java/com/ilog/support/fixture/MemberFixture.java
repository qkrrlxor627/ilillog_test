package com.ilog.support.fixture;

import com.ilog.member.entity.Member;
import org.springframework.test.util.ReflectionTestUtils;

public final class MemberFixture {

    public static final String ENCODED_PASSWORD = "{bcrypt}encoded";

    private MemberFixture() {}

    /** 저장 전(식별자 없음) 회원. 리포지토리·통합 테스트용. */
    public static Member create(String email, String nickname) {
        return Member.create(email, ENCODED_PASSWORD, "테스터", nickname);
    }

    /** 식별자가 있는 회원. 단위 테스트용. */
    public static Member create(Long id) {
        return create(id, "user" + id + "@ilog.com", "닉네임" + id);
    }

    public static Member create(Long id, String email, String nickname) {
        Member member = create(email, nickname);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
