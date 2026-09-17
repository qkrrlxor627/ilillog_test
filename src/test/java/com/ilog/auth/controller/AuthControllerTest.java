package com.ilog.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilog.auth.dto.LoginRequest;
import com.ilog.auth.dto.TemporaryPasswordRequest;
import com.ilog.auth.dto.TokenResponse;
import com.ilog.auth.service.AuthService;
import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.global.security.LoginMember;
import com.ilog.support.WebMvcTestSupport;
import com.ilog.support.security.WithLoginMember;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends WebMvcTestSupport {

    @MockitoBean AuthService authService;

    @Test
    @DisplayName("로그인에 성공하면 200 과 토큰 정보를 반환한다")
    void login_valid_returns200() throws Exception {
        // given
        given(authService.login(new LoginRequest("a@b.com", "password1!")))
                .willReturn(new TokenResponse("access", null, "Bearer", 1800, true));

        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/tokens")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "a@b.com", "password": "password1!"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.data.refreshToken").isEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.isTempPassword").value(true))
                .andExpect(jsonPath("$.error").isEmpty());
    }

    @Test
    @DisplayName("이메일 형식이 아니면 400 과 fieldErrors 를 반환한다")
    void login_invalidEmail_returns400() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/tokens")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "not-email", "password": "password1!"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("email"));
        then(authService).should(never()).login(any());
    }

    @Test
    @DisplayName("본문이 JSON 이 아니면 400 을 반환한다")
    void login_malformedBody_returns400() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/tokens")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("자격 증명이 틀리면 401 AUTH_INVALID_CREDENTIALS 를 반환한다")
    void login_invalidCredentials_returns401() throws Exception {
        // given
        given(authService.login(any()))
                .willThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/tokens")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "a@b.com", "password": "wrong"}
                                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("이메일 또는 비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("유효한 Bearer 토큰으로 로그아웃하면 204 를 반환한다")
    void logout_validBearerToken_returns204() throws Exception {
        // given
        given(jwtProvider.parseMemberId("valid-token")).willReturn(1L);
        given(loginMemberLoader.loadActiveMember(1L))
                .willReturn(Optional.of(new LoginMember(1L, false)));

        // when & then
        mockMvc.perform(
                        delete("/api/v1/auth/tokens")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isNoContent());
        then(authService).should().logout(1L);
    }

    @Test
    @DisplayName("토큰 없이 로그아웃하면 401 을 반환한다")
    void logout_withoutToken_returns401() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/auth/tokens"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("만료된 토큰이면 401 AUTH_TOKEN_EXPIRED 를 반환한다")
    void logout_expiredToken_returns401TokenExpired() throws Exception {
        // given
        given(jwtProvider.parseMemberId("expired-token"))
                .willThrow(new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED));

        // when & then
        mockMvc.perform(
                        delete("/api/v1/auth/tokens")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("탈퇴한 회원의 토큰이면 401 을 반환한다")
    void logout_withdrawnMemberToken_returns401() throws Exception {
        // given
        given(jwtProvider.parseMemberId("withdrawn-token")).willReturn(1L);
        given(loginMemberLoader.loadActiveMember(1L)).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(
                        delete("/api/v1/auth/tokens")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer withdrawn-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithLoginMember(memberId = 1L, passwordResetRequired = true)
    @DisplayName("임시 비밀번호 상태에서도 로그아웃은 허용한다")
    void logout_passwordResetRequired_returns204() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/v1/auth/tokens")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("이메일·이름이 일치하면 임시 비밀번호를 발급하고 200 과 안내 문구를 반환한다")
    void issueTemporaryPassword_valid_returns200() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/temporary-passwords")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "a@b.com", "name": "박기택"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").isNotEmpty());
        then(authService)
                .should()
                .issueTemporaryPassword(new TemporaryPasswordRequest("a@b.com", "박기택"));
    }

    @Test
    @DisplayName("이름이 비어 있으면 400 을 반환한다")
    void issueTemporaryPassword_blankName_returns400() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/temporary-passwords")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "a@b.com", "name": " "}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("일치하는 회원이 없으면 404 MEMBER_NOT_FOUND 를 반환한다")
    void issueTemporaryPassword_notMatched_returns404() throws Exception {
        // given
        willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                .given(authService)
                .issueTemporaryPassword(any());

        // when & then
        mockMvc.perform(
                        post("/api/v1/auth/temporary-passwords")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email": "a@b.com", "name": "다른이름"}
                                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"));
    }
}
