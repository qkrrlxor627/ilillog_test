package com.ilog.global.security;

/**
 * 인증된 회원 주체. 컨트롤러에서 {@code @AuthenticationPrincipal LoginMember} 로 받고, 서비스에는 {@code memberId} 만
 * 넘긴다.
 *
 * @param memberId 회원 ID
 * @param passwordResetRequired 임시 비밀번호 상태 (비밀번호 변경 전까지 대부분의 API 차단)
 */
public record LoginMember(Long memberId, boolean passwordResetRequired) {}
