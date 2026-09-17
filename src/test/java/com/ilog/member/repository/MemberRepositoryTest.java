package com.ilog.member.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ilog.member.entity.Member;
import com.ilog.support.RepositoryTestSupport;
import com.ilog.support.fixture.MemberFixture;
import java.time.Instant;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class MemberRepositoryTest extends RepositoryTestSupport {

    @Autowired MemberRepository memberRepository;

    @Test
    @DisplayName("저장하면 생성 시각은 Auditing 으로 기록되고 수정 시각은 비어 있다")
    void save_recordsCreatedAtOnly() {
        // when
        Member saved = memberRepository.saveAndFlush(MemberFixture.create("a@b.com", "기택"));

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNull();
        assertThat(saved.isPasswordResetRequired()).isFalse();
    }

    @Test
    @DisplayName("활성 회원과 같은 이메일은 부분 유니크 인덱스 위반이다")
    void save_duplicateActiveEmail_violatesPartialUniqueIndex() {
        // given
        memberRepository.saveAndFlush(MemberFixture.create("a@b.com", "기택"));

        // when & then
        assertThatThrownBy(
                        () -> memberRepository.saveAndFlush(MemberFixture.create("a@b.com", "다른닉")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOfSatisfying(
                        ConstraintViolationException.class,
                        e -> assertThat(e.getConstraintName()).isEqualTo("uk_member_email_active"));
    }

    @Test
    @DisplayName("탈퇴한 회원의 이메일로는 다시 가입할 수 있다 (D-11 잠정)")
    void save_emailOfWithdrawnMember_allowed() {
        // given
        Member withdrawn = memberRepository.saveAndFlush(MemberFixture.create("a@b.com", "기택"));
        withdrawn.withdraw(Instant.parse("2026-09-01T00:00:00Z"));
        memberRepository.saveAndFlush(withdrawn);

        // when
        Member rejoined = memberRepository.saveAndFlush(MemberFixture.create("a@b.com", "새닉네임"));

        // then
        assertThat(rejoined.getId()).isNotEqualTo(withdrawn.getId());
        assertThat(memberRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .get()
                .extracting(Member::getId)
                .isEqualTo(rejoined.getId());
    }

    @Test
    @DisplayName("닉네임은 탈퇴 여부와 상관없이 유니크하다")
    void save_duplicateNickname_violatesUniqueIndex() {
        // given
        memberRepository.saveAndFlush(MemberFixture.create("a@b.com", "기택"));

        // when & then
        assertThatThrownBy(
                        () -> memberRepository.saveAndFlush(MemberFixture.create("c@d.com", "기택")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .cause()
                .isInstanceOfSatisfying(
                        ConstraintViolationException.class,
                        e -> assertThat(e.getConstraintName()).isEqualTo("uk_member_nickname"));
    }

    @Test
    @DisplayName("활성 회원 조회는 탈퇴 회원을 제외한다")
    void findActive_excludesWithdrawn() {
        // given
        Member member = memberRepository.saveAndFlush(MemberFixture.create("a@b.com", "기택"));
        member.withdraw(Instant.parse("2026-09-01T00:00:00Z"));
        memberRepository.saveAndFlush(member);

        // when & then
        assertThat(memberRepository.findByIdAndDeletedAtIsNull(member.getId())).isEmpty();
        assertThat(memberRepository.findByEmailAndDeletedAtIsNull("a@b.com")).isEmpty();
        assertThat(memberRepository.existsByEmailAndDeletedAtIsNull("a@b.com")).isFalse();
        assertThat(memberRepository.existsByNickname("기택")).isTrue();
    }
}
