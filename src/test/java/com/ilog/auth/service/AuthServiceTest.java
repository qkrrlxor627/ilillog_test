package com.ilog.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.ilog.auth.dto.LoginRequest;
import com.ilog.auth.dto.TemporaryPasswordRequest;
import com.ilog.auth.dto.TokenResponse;
import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.member.entity.Member;
import com.ilog.member.repository.MemberRepository;
import com.ilog.support.fixture.MemberFixture;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;
    @Mock TemporaryPasswordGenerator temporaryPasswordGenerator;
    @Mock TemporaryPasswordSender temporaryPasswordSender;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(
                        memberRepository,
                        passwordEncoder,
                        tokenService,
                        temporaryPasswordGenerator,
                        temporaryPasswordSender,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("이메일과 비밀번호가 일치하면 Access Token 을 발급한다")
    void login_validCredentials_returnsToken() {
        // given
        Member member = MemberFixture.create(1L);
        given(memberRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .willReturn(Optional.of(member));
        given(passwordEncoder.matches("password1!", member.getPassword())).willReturn(true);
        given(tokenService.issue(1L))
                .willReturn(new TokenService.IssuedToken("access", null, 1800));

        // when
        TokenResponse response = authService.login(new LoginRequest("a@b.com", "password1!"));

        // then
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isNull();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800);
        assertThat(response.isTempPassword()).isFalse();
    }

    @Test
    @DisplayName("임시 비밀번호 상태의 회원이 로그인하면 isTempPassword 가 true 다")
    void login_passwordResetRequired_returnsTempPasswordTrue() {
        // given
        Member member = MemberFixture.create(1L);
        member.resetToTemporaryPassword("{bcrypt}temp", NOW);
        given(memberRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .willReturn(Optional.of(member));
        given(passwordEncoder.matches("Temp1234!", "{bcrypt}temp")).willReturn(true);
        given(tokenService.issue(1L))
                .willReturn(new TokenService.IssuedToken("access", null, 1800));

        // when
        TokenResponse response = authService.login(new LoginRequest("a@b.com", "Temp1234!"));

        // then
        assertThat(response.isTempPassword()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는(또는 탈퇴한) 이메일이면 AUTH_INVALID_CREDENTIALS 예외가 발생한다")
    void login_unknownEmail_throwsInvalidCredentials() {
        // given
        given(memberRepository.findByEmailAndDeletedAtIsNull("none@b.com"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("none@b.com", "password1!")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
        then(tokenService).should(never()).issue(any());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 이메일이 없을 때와 같은 AUTH_INVALID_CREDENTIALS 예외가 발생한다")
    void login_wrongPassword_throwsInvalidCredentials() {
        // given
        Member member = MemberFixture.create(1L);
        given(memberRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrong", member.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("로그아웃하면 토큰 폐기를 위임한다")
    void logout_delegatesRevoke() {
        // when
        authService.logout(1L);

        // then
        then(tokenService).should().revoke(1L);
    }

    @Test
    @DisplayName("이메일과 이름이 일치하면 임시 비밀번호로 교체하고 전달한다")
    void issueTemporaryPassword_matched_resetsAndSends() {
        // given
        Member member = MemberFixture.create(1L, "a@b.com", "기택");
        given(memberRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .willReturn(Optional.of(member));
        given(temporaryPasswordGenerator.generate()).willReturn("Temp1234!");
        given(passwordEncoder.encode("Temp1234!")).willReturn("{bcrypt}temp");

        // when
        authService.issueTemporaryPassword(new TemporaryPasswordRequest("a@b.com", "테스터"));

        // then
        assertThat(member.getPassword()).isEqualTo("{bcrypt}temp");
        assertThat(member.isPasswordResetRequired()).isTrue();
        assertThat(member.getUpdatedAt()).isEqualTo(NOW);
        then(temporaryPasswordSender).should().send("a@b.com", "Temp1234!");
    }

    @Test
    @DisplayName("이름이 일치하지 않으면 MEMBER_NOT_FOUND 예외가 발생하고 비밀번호는 바뀌지 않는다")
    void issueTemporaryPassword_nameMismatch_throwsMemberNotFound() {
        // given
        Member member = MemberFixture.create(1L, "a@b.com", "기택");
        given(memberRepository.findByEmailAndDeletedAtIsNull("a@b.com"))
                .willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(
                        () ->
                                authService.issueTemporaryPassword(
                                        new TemporaryPasswordRequest("a@b.com", "다른이름")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        assertThat(member.isPasswordResetRequired()).isFalse();
        then(temporaryPasswordSender).should(never()).send(anyString(), anyString());
    }
}
