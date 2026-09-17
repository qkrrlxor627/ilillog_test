package com.ilog.member.entity;

import com.ilog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    /** BCrypt 해시. toString·로그에 노출하지 않는다. */
    @Column(nullable = false)
    private String password;

    /** 실명. 수정 불가. */
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "password_reset_required", nullable = false)
    private boolean passwordResetRequired;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Member(String email, String encodedPassword, String name, String nickname) {
        this.email = email;
        this.password = encodedPassword;
        this.name = name;
        this.nickname = nickname;
        this.passwordResetRequired = false;
    }

    public static Member create(
            String email, String encodedPassword, String name, String nickname) {
        return new Member(email, encodedPassword, name, nickname);
    }

    public void changeNickname(String nickname, Instant now) {
        this.nickname = nickname;
        markUpdated(now);
    }

    /** 본인이 비밀번호를 변경하면 임시 비밀번호 상태가 해제된다. */
    public void changePassword(String encodedPassword, Instant now) {
        this.password = encodedPassword;
        this.passwordResetRequired = false;
        markUpdated(now);
    }

    /** 임시 비밀번호로 교체하고, 다음 로그인 후 비밀번호 변경을 강제한다. */
    public void resetToTemporaryPassword(String encodedTemporaryPassword, Instant now) {
        this.password = encodedTemporaryPassword;
        this.passwordResetRequired = true;
        markUpdated(now);
    }

    /**
     * 소프트 삭제. 30일 보관.
     *
     * <p>TODO(D-10): 30일 내 복구 정책 확정 시 구현. TODO(D-11): 30일 경과 후 개인정보 파기 배치 [제안]
     */
    public void withdraw(Instant now) {
        this.deletedAt = now;
    }

    public boolean isWithdrawn() {
        return deletedAt != null;
    }

    @Override
    public String toString() {
        return "Member[id=" + id + ", nickname=" + nickname + ", withdrawn=" + isWithdrawn() + "]";
    }
}
