package com.ilog.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ilog.auth.service.TokenService;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 응답. {@code isTempPassword=true} 면 프론트가 이동 불가한 비밀번호 변경 화면으로 강제 전환한다.
 *
 * <p>[잠정] Access 단독 발급이므로 refreshToken 은 null.
 */
public record TokenResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...") String accessToken,
        @Schema(nullable = true) String refreshToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(example = "1800") long expiresIn,
        @Schema(example = "false") @JsonProperty("isTempPassword") boolean isTempPassword) {

    private static final String BEARER = "Bearer";

    public static TokenResponse of(TokenService.IssuedToken token, boolean isTempPassword) {
        return new TokenResponse(
                token.accessToken(),
                token.refreshToken(),
                BEARER,
                token.expiresIn(),
                isTempPassword);
    }

    @Override
    public String toString() {
        return "TokenResponse[accessToken=****, refreshToken=****, expiresIn="
                + expiresIn
                + ", isTempPassword="
                + isTempPassword
                + "]";
    }
}
