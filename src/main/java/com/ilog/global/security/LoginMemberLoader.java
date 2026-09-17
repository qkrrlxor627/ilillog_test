package com.ilog.global.security;

import java.util.Optional;

/** 토큰의 회원 ID 로 현재 활성 회원 상태를 조회한다. 탈퇴·존재하지 않는 회원이면 비어 있다. */
public interface LoginMemberLoader {

    Optional<LoginMember> loadActiveMember(Long memberId);
}
