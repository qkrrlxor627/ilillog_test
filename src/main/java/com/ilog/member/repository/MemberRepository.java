package com.ilog.member.repository;

import com.ilog.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    // TODO(D-11): 탈퇴 회원 이메일 재가입 정책 확정 시 조정. [잠정] 활성 회원 기준으로만 중복 판단
    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByNickname(String nickname);
}
