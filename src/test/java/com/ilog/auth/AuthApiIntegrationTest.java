package com.ilog.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

import com.ilog.global.security.JwtProperties;
import com.ilog.global.security.JwtProvider;
import com.ilog.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AuthApiIntegrationTest extends IntegrationTestSupport {

    @Autowired JwtProperties jwtProperties;

    @Test
    @DisplayName("회원가입 → 로그인 → 닉네임 수정 → 비밀번호 재확인 조회 → 로그아웃")
    void signupLoginAndManageMyInfo() {
        // given
        signup("a@b.com", DEFAULT_PASSWORD, "박기택", "기택");
        String token = login("a@b.com", DEFAULT_PASSWORD);

        // when & then: 닉네임 수정
        restTestClient
                .patch()
                .uri("/api/v1/users/me")
                .header(AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"nickname": "새닉네임"}
                        """)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.nickname")
                .isEqualTo("새닉네임")
                .jsonPath("$.data.updatedAt")
                .isNotEmpty();

        // when & then: 비밀번호 재확인 + 개인정보 조회
        restTestClient
                .post()
                .uri("/api/v1/users/me/password-verification")
                .header(AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"password": "%s"}
                        """
                                .formatted(DEFAULT_PASSWORD))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.email")
                .isEqualTo("a@b.com")
                .jsonPath("$.data.name")
                .isEqualTo("박기택")
                .jsonPath("$.data.nickname")
                .isEqualTo("새닉네임");

        // when & then: 로그인 상태에서 본인 현재 닉네임은 사용 가능
        restTestClient
                .get()
                .uri("/api/v1/users/nickname-availability?nickname={nickname}", "새닉네임")
                .header(AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.available")
                .isEqualTo(true);

        // when & then: 로그아웃
        restTestClient
                .delete()
                .uri("/api/v1/auth/tokens")
                .header(AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    @DisplayName("이미 가입된 이메일·닉네임은 사용 불가로 조회되고, 같은 이메일로 가입하면 409 를 반환한다")
    void duplicateEmail_unavailableAndConflict() {
        // given
        signup("a@b.com", DEFAULT_PASSWORD, "박기택", "기택");

        // when & then
        restTestClient
                .get()
                .uri("/api/v1/users/email-availability?email={email}", "a@b.com")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.available")
                .isEqualTo(false);
        restTestClient
                .get()
                .uri("/api/v1/users/nickname-availability?nickname={nickname}", "기택")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.available")
                .isEqualTo(false);
        restTestClient
                .post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"email": "a@b.com", "password": "password1!", "passwordConfirm": "password1!",
                         "name": "다른사람", "nickname": "다른닉"}
                        """)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("MEMBER_EMAIL_DUPLICATED");
    }

    @Test
    @DisplayName("임시 비밀번호로 로그인하면 비밀번호 변경 전까지 다른 API 가 403 이고, 변경하면 같은 토큰으로 이용 가능하다")
    void temporaryPasswordFlow_forcesPasswordChange() {
        // given
        signup("a@b.com", DEFAULT_PASSWORD, "박기택", "기택");
        restTestClient
                .post()
                .uri("/api/v1/auth/temporary-passwords")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"email": "a@b.com", "name": "박기택"}
                        """)
                .exchange()
                .expectStatus()
                .isOk();
        ArgumentCaptor<String> temporaryPassword = ArgumentCaptor.forClass(String.class);
        then(temporaryPasswordSender).should().send(eq("a@b.com"), temporaryPassword.capture());

        // when: 임시 비밀번호로 로그인
        String loginBody =
                restTestClient
                        .post()
                        .uri("/api/v1/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {"email": "a@b.com", "password": "%s"}
                                """
                                        .formatted(temporaryPassword.getValue()))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        String token = JsonPath.read(loginBody, "$.data.accessToken");

        // then
        assertThat((Boolean) JsonPath.read(loginBody, "$.data.isTempPassword")).isTrue();
        restTestClient
                .get()
                .uri("/api/v1/posts")
                .header(AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("AUTH_PASSWORD_RESET_REQUIRED");

        // when: 비밀번호 변경
        restTestClient
                .patch()
                .uri("/api/v1/users/me/password")
                .header(AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"currentPassword": "%s", "newPassword": "newPassword1!", "newPasswordConfirm": "newPassword1!"}
                        """
                                .formatted(temporaryPassword.getValue()))
                .exchange()
                .expectStatus()
                .isNoContent();

        // then
        restTestClient
                .get()
                .uri("/api/v1/posts")
                .header(AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus()
                .isOk();
        String newLoginBody =
                restTestClient
                        .post()
                        .uri("/api/v1/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {"email": "a@b.com", "password": "newPassword1!"}
                                """)
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        assertThat((Boolean) JsonPath.read(newLoginBody, "$.data.isTempPassword")).isFalse();
    }

    @Test
    @DisplayName("탈퇴하면 기존 토큰과 로그인이 거절되고, 같은 이메일로 다시 가입할 수 있다")
    void withdrawal_invalidatesTokenAndAllowsRejoin() {
        // given
        String token = signupAndLogin("a@b.com", "기택");

        // when
        restTestClient
                .post()
                .uri("/api/v1/users/me/withdrawal")
                .header(AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"password": "%s"}
                        """
                                .formatted(DEFAULT_PASSWORD))
                .exchange()
                .expectStatus()
                .isNoContent();

        // then
        restTestClient
                .get()
                .uri("/api/v1/posts")
                .header(AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus()
                .isUnauthorized();
        restTestClient
                .post()
                .uri("/api/v1/auth/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"email": "a@b.com", "password": "%s"}
                        """
                                .formatted(DEFAULT_PASSWORD))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("AUTH_INVALID_CREDENTIALS");
        assertThat(signup("a@b.com", DEFAULT_PASSWORD, "박기택", "새닉네임")).isNotNull();
    }

    @Test
    @DisplayName("만료된 토큰은 401 AUTH_TOKEN_EXPIRED, 위조 토큰은 401 UNAUTHORIZED 를 반환한다")
    void invalidTokens_return401WithReason() {
        // given
        signup("a@b.com", DEFAULT_PASSWORD, "박기택", "기택");
        Instant issuedAt =
                Instant.now()
                        .minus(Duration.ofSeconds(jwtProperties.accessTokenValiditySeconds() + 60));
        String expired =
                new JwtProvider(jwtProperties, Clock.fixed(issuedAt, ZoneOffset.UTC))
                        .createAccessToken(1L)
                        .value();

        // when & then
        restTestClient
                .get()
                .uri("/api/v1/posts")
                .header(AUTHORIZATION, bearer(expired))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("AUTH_TOKEN_EXPIRED");
        restTestClient
                .get()
                .uri("/api/v1/posts")
                .header(AUTHORIZATION, bearer("forged.token.value"))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("비로그인 허용 API 는 잘못된 토큰이 붙어 있어도 비로그인으로 처리한다")
    void permitAllApi_withInvalidToken_treatedAsAnonymous() {
        // when & then
        restTestClient
                .get()
                .uri("/api/v1/users/email-availability?email={email}", "new@b.com")
                .header(AUTHORIZATION, bearer("forged.token.value"))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.available")
                .isEqualTo(true);
    }

    @Test
    @DisplayName("존재하지 않는 경로는 인증 후 404 공통 에러로 응답한다")
    void unknownPath_authenticated_returns404() {
        // given
        String token = signupAndLogin("a@b.com", "기택");

        // when & then
        restTestClient
                .get()
                .uri("/api/v1/unknown")
                .header(AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("NOT_FOUND");
    }
}
