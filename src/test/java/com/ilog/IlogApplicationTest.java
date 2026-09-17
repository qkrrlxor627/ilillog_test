package com.ilog;

import com.ilog.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IlogApplicationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("애플리케이션이 기동되고 Flyway 스키마가 엔티티 매핑 검증을 통과한다")
    void contextLoads() {}

    @Test
    @DisplayName("헬스체크는 인증 없이 200 을 반환한다")
    void actuatorHealth_anonymous_returns200() {
        // when & then
        restTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    @DisplayName("OpenAPI 문서는 인증 없이 조회된다")
    void apiDocs_anonymous_returns200() {
        // when & then
        restTestClient
                .get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paths['/api/v1/posts']")
                .exists();
    }

    @Test
    @DisplayName("토큰 없이 보호된 API 를 호출하면 401 과 공통 에러 형식을 반환한다")
    void protectedApi_withoutToken_returns401() {
        // when & then
        restTestClient
                .get()
                .uri("/api/v1/posts")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(false)
                .jsonPath("$.data")
                .isEmpty()
                .jsonPath("$.error.code")
                .isEqualTo("UNAUTHORIZED");
    }
}
