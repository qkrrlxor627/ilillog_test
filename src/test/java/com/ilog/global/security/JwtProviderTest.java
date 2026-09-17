package com.ilog.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes!!";
    private static final long VALIDITY_SECONDS = 1800;
    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    private final JwtProvider jwtProvider = providerAt(NOW, SECRET);

    @Test
    @DisplayName("발급한 Access Token 을 검증하면 회원 ID 를 꺼낸다")
    void parseMemberId_validToken_returnsMemberId() {
        // given
        JwtProvider.AccessToken token = jwtProvider.createAccessToken(42L);

        // when
        Long memberId = jwtProvider.parseMemberId(token.value());

        // then
        assertThat(memberId).isEqualTo(42L);
        assertThat(token.expiresIn()).isEqualTo(VALIDITY_SECONDS);
    }

    @Test
    @DisplayName("유효 시간이 지난 토큰은 AUTH_TOKEN_EXPIRED 예외가 발생한다")
    void parseMemberId_expiredToken_throwsTokenExpired() {
        // given
        String token = jwtProvider.createAccessToken(42L).value();
        JwtProvider later = providerAt(NOW.plus(Duration.ofSeconds(VALIDITY_SECONDS)), SECRET);

        // when & then
        assertThatThrownBy(() -> later.parseMemberId(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("다른 키로 서명된 토큰은 UNAUTHORIZED 예외가 발생한다")
    void parseMemberId_differentSecret_throwsUnauthorized() {
        // given
        String token =
                providerAt(NOW, "another-secret-key-must-be-32-bytes-long!!")
                        .createAccessToken(42L)
                        .value();

        // when & then
        assertThatThrownBy(() -> jwtProvider.parseMemberId(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("변조된 토큰은 UNAUTHORIZED 예외가 발생한다")
    void parseMemberId_tamperedToken_throwsUnauthorized() {
        // given
        String token = jwtProvider.createAccessToken(42L).value();
        String tampered = token.substring(0, token.length() - 2) + "xx";

        // when & then
        assertThatThrownBy(() -> jwtProvider.parseMemberId(tampered))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("JWT 형식이 아닌 문자열은 UNAUTHORIZED 예외가 발생한다")
    void parseMemberId_malformed_throwsUnauthorized() {
        // when & then
        assertThatThrownBy(() -> jwtProvider.parseMemberId("not-a-jwt"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("설정과 토큰의 toString 에 비밀값이 노출되지 않는다")
    void toString_masksSecrets() {
        // given
        JwtProperties properties = new JwtProperties(SECRET, VALIDITY_SECONDS);
        JwtProvider.AccessToken token = jwtProvider.createAccessToken(1L);

        // when & then
        assertThat(properties.toString()).doesNotContain(SECRET);
        assertThat(token.toString()).doesNotContain(token.value());
    }

    private static JwtProvider providerAt(Instant instant, String secret) {
        return new JwtProvider(
                new JwtProperties(secret, VALIDITY_SECONDS), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
