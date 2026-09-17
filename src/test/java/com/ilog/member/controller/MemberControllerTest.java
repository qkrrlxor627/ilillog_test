package com.ilog.member.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.member.dto.MemberInfoResponse;
import com.ilog.member.dto.NicknameUpdateRequest;
import com.ilog.member.dto.NicknameUpdateResponse;
import com.ilog.member.dto.PasswordChangeRequest;
import com.ilog.member.dto.WithdrawalRequest;
import com.ilog.member.service.MemberService;
import com.ilog.support.WebMvcTestSupport;
import com.ilog.support.security.WithLoginMember;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(MemberController.class)
class MemberControllerTest extends WebMvcTestSupport {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @MockitoBean MemberService memberService;

    @Nested
    @DisplayName("POST /api/v1/users 회원가입")
    class Signup {

        @Test
        @DisplayName("성공하면 201, Location, 회원 ID 를 반환한다")
        void signup_valid_returns201() throws Exception {
            // given
            given(memberService.signup(any())).willReturn(1L);

            // when & then
            mockMvc.perform(
                            post("/api/v1/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"email": "a@b.com", "password": "password1!", "passwordConfirm": "password1!",
                                             "name": "박기택", "nickname": "기택"}
                                            """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/users/me"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.memberId").value(1));
        }

        @Test
        @DisplayName("비밀번호가 규칙에 맞지 않으면 400 과 password fieldError 를 반환한다")
        void signup_weakPassword_returns400() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"email": "a@b.com", "password": "password", "passwordConfirm": "password",
                                             "name": "박기택", "nickname": "기택"}
                                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("password"));
            then(memberService).should(never()).signup(any());
        }

        @Test
        @DisplayName("이메일이 중복이면 409 MEMBER_EMAIL_DUPLICATED 를 반환한다")
        void signup_emailDuplicated_returns409() throws Exception {
            // given
            given(memberService.signup(any()))
                    .willThrow(new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED));

            // when & then
            mockMvc.perform(
                            post("/api/v1/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"email": "a@b.com", "password": "password1!", "passwordConfirm": "password1!",
                                             "name": "박기택", "nickname": "기택"}
                                            """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("MEMBER_EMAIL_DUPLICATED"));
        }
    }

    @Nested
    @DisplayName("GET 중복 확인")
    class Availability {

        @Test
        @DisplayName("이메일 사용 가능 여부를 비로그인으로 조회한다")
        void emailAvailability_anonymous_returns200() throws Exception {
            // given
            given(memberService.isEmailAvailable("a@b.com")).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/v1/users/email-availability").param("email", "a@b.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.available").value(true));
        }

        @Test
        @DisplayName("이메일 파라미터가 형식에 맞지 않으면 400 을 반환한다")
        void emailAvailability_invalidEmail_returns400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/email-availability").param("email", "wrong"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("email"));
        }

        @Test
        @DisplayName("이메일 파라미터가 없으면 400 을 반환한다")
        void emailAvailability_missingEmail_returns400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/email-availability"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("비로그인 닉네임 확인은 회원 ID 없이 조회한다")
        void nicknameAvailability_anonymous_passesNullMemberId() throws Exception {
            // given
            given(memberService.isNicknameAvailable(eq("기택"), isNull())).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/v1/users/nickname-availability").param("nickname", "기택"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.available").value(false));
        }

        @Test
        @WithLoginMember(memberId = 7L)
        @DisplayName("로그인 상태의 닉네임 확인은 본인 회원 ID 를 함께 넘긴다")
        void nicknameAvailability_loggedIn_passesMemberId() throws Exception {
            // given
            given(memberService.isNicknameAvailable("기택", 7L)).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/v1/users/nickname-availability").param("nickname", "기택"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.available").value(true));
        }

        @Test
        @DisplayName("닉네임이 최소 길이보다 짧으면 400 을 반환한다")
        void nicknameAvailability_tooShort_returns400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/users/nickname-availability").param("nickname", "a"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("nickname"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/me 닉네임 수정")
    class ChangeNickname {

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 200 과 변경된 닉네임·수정 시각을 반환한다")
        void changeNickname_valid_returns200() throws Exception {
            // given
            given(memberService.changeNickname(1L, new NicknameUpdateRequest("새닉네임")))
                    .willReturn(new NicknameUpdateResponse("새닉네임", NOW));

            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"nickname": "새닉네임", "email": "ignored@b.com"}
                                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
                    .andExpect(jsonPath("$.data.updatedAt").value("2026-09-17T10:00:00Z"));
        }

        @Test
        @WithLoginMember
        @DisplayName("닉네임이 비어 있으면 400 을 반환한다")
        void changeNickname_blank_returns400() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"nickname": ""}
                                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("인증 없이 호출하면 401 을 반환한다")
        void changeNickname_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"nickname": "새닉네임"}
                                            """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithLoginMember(passwordResetRequired = true)
        @DisplayName("임시 비밀번호 상태면 403 AUTH_PASSWORD_RESET_REQUIRED 를 반환한다")
        void changeNickname_passwordResetRequired_returns403() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"nickname": "새닉네임"}
                                            """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_REQUIRED"));
            then(memberService).should(never()).changeNickname(anyLong(), any());
        }

        @Test
        @WithLoginMember
        @DisplayName("닉네임이 중복이면 409 를 반환한다")
        void changeNickname_duplicated_returns409() throws Exception {
            // given
            given(memberService.changeNickname(anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED));

            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"nickname": "중복닉"}
                                            """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("MEMBER_NICKNAME_DUPLICATED"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/users/me/password 비밀번호 변경")
    class ChangePassword {

        private static final String BODY =
                """
                {"currentPassword": "password1!", "newPassword": "newPassword1!", "newPasswordConfirm": "newPassword1!"}
                """;

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 204 를 반환한다")
        void changePassword_valid_returns204() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me/password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isNoContent());
            then(memberService)
                    .should()
                    .changePassword(
                            1L,
                            new PasswordChangeRequest(
                                    "password1!", "newPassword1!", "newPassword1!"));
        }

        @Test
        @WithLoginMember(memberId = 1L, passwordResetRequired = true)
        @DisplayName("임시 비밀번호 상태에서도 비밀번호 변경은 허용한다")
        void changePassword_passwordResetRequired_returns204() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me/password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithLoginMember
        @DisplayName("필수 필드가 없으면 400 을 반환한다")
        void changePassword_missingField_returns400() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me/password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"currentPassword": "password1!"}
                                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors.length()").value(2));
        }

        @Test
        @WithLoginMember
        @DisplayName("현재 비밀번호가 틀리면 400 MEMBER_PASSWORD_MISMATCH 를 반환한다")
        void changePassword_wrongCurrent_returns400() throws Exception {
            // given
            willThrow(new BusinessException(ErrorCode.MEMBER_PASSWORD_MISMATCH))
                    .given(memberService)
                    .changePassword(anyLong(), any());

            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me/password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("MEMBER_PASSWORD_MISMATCH"));
        }

        @Test
        @DisplayName("인증 없이 호출하면 401 을 반환한다")
        void changePassword_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/users/me/password")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/me/password-verification 비밀번호 재확인")
    class VerifyPassword {

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("비밀번호가 맞으면 200 과 개인정보를 반환한다")
        void verifyPassword_match_returns200() throws Exception {
            // given
            given(memberService.verifyPasswordAndGetInfo(eq(1L), any()))
                    .willReturn(new MemberInfoResponse("a@b.com", "박기택", "기택", NOW, null));

            // when & then
            mockMvc.perform(
                            post("/api/v1/users/me/password-verification")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"password": "password1!"}
                                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("a@b.com"))
                    .andExpect(jsonPath("$.data.name").value("박기택"))
                    .andExpect(jsonPath("$.data.nickname").value("기택"))
                    .andExpect(jsonPath("$.data.createdAt").value("2026-09-17T10:00:00Z"))
                    .andExpect(jsonPath("$.data.updatedAt").isEmpty());
        }

        @Test
        @WithLoginMember
        @DisplayName("비밀번호가 비어 있으면 400 을 반환한다")
        void verifyPassword_blank_returns400() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/users/me/password-verification")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"password": ""}
                                            """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithLoginMember
        @DisplayName("비밀번호가 틀리면 400 MEMBER_PASSWORD_MISMATCH 를 반환한다")
        void verifyPassword_mismatch_returns400() throws Exception {
            // given
            given(memberService.verifyPasswordAndGetInfo(anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.MEMBER_PASSWORD_MISMATCH));

            // when & then
            mockMvc.perform(
                            post("/api/v1/users/me/password-verification")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"password": "wrong"}
                                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("MEMBER_PASSWORD_MISMATCH"));
        }

        @Test
        @DisplayName("인증 없이 호출하면 401 을 반환한다")
        void verifyPassword_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/users/me/password-verification")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"password": "password1!"}
                                            """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithLoginMember
        @DisplayName("회원을 찾을 수 없으면 404 를 반환한다")
        void verifyPassword_memberNotFound_returns404() throws Exception {
            // given
            given(memberService.verifyPasswordAndGetInfo(anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(
                            post("/api/v1/users/me/password-verification")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"password": "password1!"}
                                            """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/me/withdrawal 회원 탈퇴")
    class Withdraw {

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 204 를 반환한다")
        void withdraw_valid_returns204() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/users/me/withdrawal")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"password": "password1!"}
                                            """))
                    .andExpect(status().isNoContent());
            then(memberService).should().withdraw(1L, new WithdrawalRequest("password1!"));
        }

        @Test
        @WithLoginMember
        @DisplayName("비밀번호가 없으면 400 을 반환한다")
        void withdraw_missingPassword_returns400() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/users/me/withdrawal")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("password"));
        }

        @Test
        @DisplayName("인증 없이 호출하면 401 을 반환한다")
        void withdraw_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/users/me/withdrawal")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            {"password": "password1!"}
                                            """))
                    .andExpect(status().isUnauthorized());
        }
    }
}
