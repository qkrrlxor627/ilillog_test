package com.ilog.auth.service;

import com.ilog.global.security.LoginMember;
import com.ilog.global.security.LoginMemberLoader;
import com.ilog.member.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 요청마다 활성 회원 여부와 임시 비밀번호 상태를 DB 에서 확인한다. (탈퇴 회원 토큰 즉시 무효화) */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DefaultLoginMemberLoader implements LoginMemberLoader {

    private final MemberRepository memberRepository;

    @Override
    public Optional<LoginMember> loadActiveMember(Long memberId) {
        return memberRepository
                .findByIdAndDeletedAtIsNull(memberId)
                .map(member -> new LoginMember(member.getId(), member.isPasswordResetRequired()));
    }
}
