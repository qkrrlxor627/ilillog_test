package com.ilog.global.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 설정. 서명 키는 환경변수(JWT_SECRET)로만 주입한다.
 *
 * @param secret HS256 서명 키 (32자 이상)
 * @param accessTokenValiditySeconds Access Token 유효 시간(초). [잠정] 30분
 */
@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret, @Positive long accessTokenValiditySeconds) {

    @Override
    public String toString() {
        return "JwtProperties[secret=****, accessTokenValiditySeconds="
                + accessTokenValiditySeconds
                + "]";
    }
}
