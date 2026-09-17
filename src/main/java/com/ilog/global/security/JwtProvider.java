package com.ilog.global.security;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/** HS256 Access Token 발급·검증. */
@Component
public class JwtProvider {

    private static final String ISSUER = "ilog";
    private static final String EXPIRED_ERROR_CODE = "token_expired";

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final Clock clock;
    private final long accessTokenValiditySeconds;

    public JwtProvider(JwtProperties properties, Clock clock) {
        SecretKey key =
                new SecretKeySpec(
                        properties.secret().getBytes(StandardCharsets.UTF_8),
                        MacAlgorithm.HS256.getName());
        this.encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.decoder.setJwtValidator(new ExpirationValidator(clock));
        this.clock = clock;
        this.accessTokenValiditySeconds = properties.accessTokenValiditySeconds();
    }

    public AccessToken createAccessToken(Long memberId) {
        Instant now = clock.instant();
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(ISSUER)
                        .subject(String.valueOf(memberId))
                        .id(UUID.randomUUID().toString())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(accessTokenValiditySeconds))
                        .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, accessTokenValiditySeconds);
    }

    /**
     * 토큰을 검증하고 회원 ID 를 꺼낸다.
     *
     * @throws BusinessException 만료 시 {@link ErrorCode#AUTH_TOKEN_EXPIRED}, 그 외 {@link
     *     ErrorCode#UNAUTHORIZED}
     */
    public Long parseMemberId(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            return Long.valueOf(jwt.getSubject());
        } catch (JwtValidationException e) {
            boolean expired =
                    e.getErrors().stream()
                            .anyMatch(error -> EXPIRED_ERROR_CODE.equals(error.getErrorCode()));
            throw new BusinessException(
                    expired ? ErrorCode.AUTH_TOKEN_EXPIRED : ErrorCode.UNAUTHORIZED);
        } catch (JwtException | NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /** 발급된 Access Token. toString 에 토큰 값을 노출하지 않는다. */
    public record AccessToken(String value, long expiresIn) {

        @Override
        public String toString() {
            return "AccessToken[value=****, expiresIn=" + expiresIn + "]";
        }
    }

    /** 주입된 {@link Clock} 기준으로 만료·발급자를 검사한다. (테스트에서 시간 제어 가능) */
    private record ExpirationValidator(Clock clock) implements OAuth2TokenValidator<Jwt> {

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (!ISSUER.equals(jwt.getClaimAsString("iss")) || jwt.getSubject() == null) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "잘못된 토큰입니다.", null));
            }
            Instant expiresAt = jwt.getExpiresAt();
            if (expiresAt == null || !clock.instant().isBefore(expiresAt)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error(EXPIRED_ERROR_CODE, "토큰이 만료되었습니다.", null));
            }
            return OAuth2TokenValidatorResult.success();
        }
    }
}
