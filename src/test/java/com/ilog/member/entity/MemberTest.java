package com.ilog.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.ilog.support.fixture.MemberFixture;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @Test
    @DisplayName("가입 직후에는 임시 비밀번호 상태가 아니고 수정 시각이 없다")
    void create_initialState() {
        // when
        Member member = Member.create("a@b.com", "{bcrypt}pw", "박기택", "기택");

        // then
        assertThat(member.isPasswordResetRequired()).isFalse();
        assertThat(member.isWithdrawn()).isFalse();
        assertThat(member.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("닉네임을 바꾸면 수정 시각이 갱신된다")
    void changeNickname_updatesNicknameAndUpdatedAt() {
        // given
        Member member = MemberFixture.create(1L);

        // when
        member.changeNickname("새닉네임", NOW);

        // then
        assertThat(member.getNickname()).isEqualTo("새닉네임");
        assertThat(member.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("임시 비밀번호로 교체하면 비밀번호 변경이 필요한 상태가 된다")
    void resetToTemporaryPassword_setsResetRequired() {
        // given
        Member member = MemberFixture.create(1L);

        // when
        member.resetToTemporaryPassword("{bcrypt}temp", NOW);

        // then
        assertThat(member.getPassword()).isEqualTo("{bcrypt}temp");
        assertThat(member.isPasswordResetRequired()).isTrue();
        assertThat(member.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("비밀번호를 변경하면 임시 비밀번호 상태가 해제된다")
    void changePassword_clearsResetRequired() {
        // given
        Member member = MemberFixture.create(1L);
        member.resetToTemporaryPassword("{bcrypt}temp", NOW);

        // when
        member.changePassword("{bcrypt}new", NOW.plusSeconds(60));

        // then
        assertThat(member.getPassword()).isEqualTo("{bcrypt}new");
        assertThat(member.isPasswordResetRequired()).isFalse();
        assertThat(member.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    @DisplayName("탈퇴하면 삭제 시각이 기록된다")
    void withdraw_setsDeletedAt() {
        // given
        Member member = MemberFixture.create(1L);

        // when
        member.withdraw(NOW);

        // then
        assertThat(member.isWithdrawn()).isTrue();
        assertThat(member.getDeletedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("toString 에 비밀번호와 이메일이 노출되지 않는다")
    void toString_excludesSensitiveFields() {
        // given
        Member member = MemberFixture.create(1L);

        // when
        String text = member.toString();

        // then
        assertThat(text).doesNotContain(member.getPassword()).doesNotContain(member.getEmail());
    }
}
