package com.ilog.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 프로필별 CORS 허용 origin (Vue 개발 서버 등).
 *
 * @param allowedOrigins 허용 origin 목록. 환경변수 {@code CORS_ALLOWED_ORIGINS} 에 쉼표로 구분해 주입
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins =
                allowedOrigins == null
                        ? List.of()
                        : allowedOrigins.stream().filter(o -> !o.isBlank()).toList();
    }
}
