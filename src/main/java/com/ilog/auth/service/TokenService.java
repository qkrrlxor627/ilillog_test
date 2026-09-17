package com.ilog.auth.service;

/**
 * 토큰 발급·폐기 전략.
 *
 * <p>TODO: Access/Refresh Token 구조와 Refresh 저장소(DB or Redis), 재발급 API 는 팀 결정 대기. [잠정] Access Token
 * 단독 발급 구현({@link AccessTokenOnlyTokenService})으로 시작하고, 결정되면 이 인터페이스의 구현체만 교체한다.
 */
public interface TokenService {

    IssuedToken issue(Long memberId);

    /** 로그아웃. Refresh 도입 시 해당 Refresh Token 을 폐기한다. */
    void revoke(Long memberId);

    record IssuedToken(String accessToken, String refreshToken, long expiresIn) {

        @Override
        public String toString() {
            return "IssuedToken[accessToken=****, refreshToken=****, expiresIn=" + expiresIn + "]";
        }
    }
}
