package com.ilog.auth.service;

import com.ilog.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** [잠정] Access Token 만 발급한다. refreshToken 은 null. */
@Service
@RequiredArgsConstructor
public class AccessTokenOnlyTokenService implements TokenService {

    private final JwtProvider jwtProvider;

    @Override
    public IssuedToken issue(Long memberId) {
        JwtProvider.AccessToken accessToken = jwtProvider.createAccessToken(memberId);
        return new IssuedToken(accessToken.value(), null, accessToken.expiresIn());
    }

    @Override
    public void revoke(Long memberId) {
        // Access 단독 구조에서는 서버가 폐기할 상태가 없다. 프론트가 토큰을 삭제한다.
    }
}
