package com.ilog.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.global.error.ErrorResponse;
import com.ilog.member.dto.MemberInfoResponse;
import com.ilog.member.dto.NicknameUpdateRequest;
import com.ilog.member.dto.NicknameUpdateResponse;
import com.ilog.member.dto.PasswordChangeRequest;
import com.ilog.member.dto.PasswordVerificationRequest;
import com.ilog.member.dto.SignupRequest;
import com.ilog.member.dto.WithdrawalRequest;
import com.ilog.member.entity.Member;
import com.ilog.member.repository.MemberRepository;
import com.ilog.support.fixture.MemberFixture;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoder passwordEncoder;

    MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService =
                new MemberService(
                        memberRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        private final SignupRequest request =
                new SignupRequest("a@b.com", "password1!", "password1!", "박기택", "기택");

        @Test
        @DisplayName("검증을 통과하면 비밀번호를 해싱해 저장하고 회원 ID 를 반환한다")
        void signup_valid_savesHashedPassword() {
            // given
            given(memberRepository.existsByEmailAndDeletedAtIsNull("a@b.com")).willReturn(false);
            given(memberRepository.existsByNickname("기택")).willReturn(false);
            given(passwordEncoder.encode("password1!")).willReturn("{bcrypt}hashed");
            given(memberRepository.saveAndFlush(any(Member.class)))
                    .willAnswer(
                            invocation -> {
                                Member saved = invocation.getArgument(0);
                                ReflectionTestUtils.setField(saved, "id", 1L);
                                return saved;
                            });

            // when
            Long memberId = memberService.signup(request);

            // then
            assertThat(memberId).isEqualTo(1L);
            then(memberRepository)
                    .should()
                    .saveAndFlush(argThat(m -> m.getPassword().equals("{bcrypt}hashed")));
        }

        @Test
        @DisplayName("비밀번호 확인이 다르면 MEMBER_PASSWORD_CONFIRM_MISMATCH 예외가 발생한다")
        void signup_confirmMismatch_throws() {
            // given
            SignupRequest mismatch =
                    new SignupRequest("a@b.com", "password1!", "different1!", "박기택", "기택");

            // when & then
            assertThatThrownBy(() -> memberService.signup(mismatch))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_PASSWORD_CONFIRM_MISMATCH);
            then(memberRepository).should(never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("활성 회원이 쓰는 이메일이면 MEMBER_EMAIL_DUPLICATED 예외가 발생한다")
        void signup_emailDuplicated_throws() {
            // given
            given(memberRepository.existsByEmailAndDeletedAtIsNull("a@b.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }

        @Test
        @DisplayName("이미 쓰는 닉네임이면 MEMBER_NICKNAME_DUPLICATED 예외가 발생한다")
        void signup_nicknameDuplicated_throws() {
            // given
            given(memberRepository.existsByEmailAndDeletedAtIsNull("a@b.com")).willReturn(false);
            given(memberRepository.existsByNickname("기택")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }

        @Test
        @DisplayName("사전 조회 이후 동시 가입으로 이메일 유니크 제약을 위반하면 409 코드로 변환한다")
        void signup_concurrentEmailInsert_translatesToDuplicated() {
            // given
            given(memberRepository.existsByEmailAndDeletedAtIsNull("a@b.com")).willReturn(false);
            given(memberRepository.existsByNickname("기택")).willReturn(false);
            given(passwordEncoder.encode("password1!")).willReturn("{bcrypt}hashed");
            given(memberRepository.saveAndFlush(any(Member.class)))
                    .willThrow(uniqueViolation(MemberService.EMAIL_UNIQUE_CONSTRAINT));

            // when & then
            assertThatThrownBy(() -> memberService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }

        @Test
        @DisplayName("알 수 없는 제약 위반은 변환하지 않고 그대로 던진다")
        void signup_unknownConstraintViolation_rethrows() {
            // given
            given(memberRepository.existsByEmailAndDeletedAtIsNull("a@b.com")).willReturn(false);
            given(memberRepository.existsByNickname("기택")).willReturn(false);
            given(passwordEncoder.encode("password1!")).willReturn("{bcrypt}hashed");
            given(memberRepository.saveAndFlush(any(Member.class)))
                    .willThrow(uniqueViolation("some_other_constraint"));

            // when & then
            assertThatThrownBy(() -> memberService.signup(request))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("중복 확인")
    class Availability {

        @Test
        @DisplayName("활성 회원이 쓰지 않는 이메일이면 사용 가능하다")
        void isEmailAvailable_notUsed_true() {
            // given
            given(memberRepository.existsByEmailAndDeletedAtIsNull("a@b.com")).willReturn(false);

            // when & then
            assertThat(memberService.isEmailAvailable("a@b.com")).isTrue();
        }

        @Test
        @DisplayName("다른 회원이 쓰는 닉네임이면 사용 불가능하다")
        void isNicknameAvailable_usedByOther_false() {
            // given
            given(memberRepository.existsByNickname("기택")).willReturn(true);

            // when & then
            assertThat(memberService.isNicknameAvailable("기택", null)).isFalse();
        }

        @Test
        @DisplayName("로그인 회원의 현재 닉네임이면 사용 가능하다")
        void isNicknameAvailable_ownCurrentNickname_true() {
            // given
            Member member = MemberFixture.create(1L, "a@b.com", "기택");
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));

            // when & then
            assertThat(memberService.isNicknameAvailable("기택", 1L)).isTrue();
            then(memberRepository).should(never()).existsByNickname(any());
        }

        @Test
        @DisplayName("로그인 상태라도 본인 닉네임이 아니면 중복 여부로 판단한다")
        void isNicknameAvailable_loggedInOtherNickname_checksDuplicate() {
            // given
            Member member = MemberFixture.create(1L, "a@b.com", "기택");
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname("새닉네임")).willReturn(false);

            // when & then
            assertThat(memberService.isNicknameAvailable("새닉네임", 1L)).isTrue();
        }
    }

    @Nested
    @DisplayName("닉네임 수정")
    class ChangeNickname {

        @Test
        @DisplayName("중복이 아니면 닉네임과 수정 시각을 갱신한다")
        void changeNickname_available_updates() {
            // given
            Member member = MemberFixture.create(1L, "a@b.com", "기택");
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname("새닉네임")).willReturn(false);

            // when
            NicknameUpdateResponse response =
                    memberService.changeNickname(1L, new NicknameUpdateRequest("새닉네임"));

            // then
            assertThat(response.nickname()).isEqualTo("새닉네임");
            assertThat(response.updatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("다른 회원이 쓰는 닉네임이면 MEMBER_NICKNAME_DUPLICATED 예외가 발생한다")
        void changeNickname_duplicated_throws() {
            // given
            Member member = MemberFixture.create(1L, "a@b.com", "기택");
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname("중복")).willReturn(true);

            // when & then
            assertThatThrownBy(
                            () -> memberService.changeNickname(1L, new NicknameUpdateRequest("중복")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
            assertThat(member.getNickname()).isEqualTo("기택");
        }

        @Test
        @DisplayName("플러시 중 닉네임 유니크 제약을 위반하면 409 코드로 변환한다")
        void changeNickname_concurrentUpdate_translatesToDuplicated() {
            // given
            Member member = MemberFixture.create(1L, "a@b.com", "기택");
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname("새닉네임")).willReturn(false);
            willThrow(uniqueViolation(MemberService.NICKNAME_UNIQUE_CONSTRAINT))
                    .given(memberRepository)
                    .flush();

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberService.changeNickname(
                                            1L, new NicknameUpdateRequest("새닉네임")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }

        @Test
        @DisplayName("탈퇴했거나 없는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
        void changeNickname_memberNotFound_throws() {
            // given
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberService.changeNickname(
                                            1L, new NicknameUpdateRequest("새닉네임")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        private Member member;

        @BeforeEach
        void setUp() {
            member = MemberFixture.create(1L);
            member.resetToTemporaryPassword("{bcrypt}temp", NOW.minusSeconds(3600));
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
        }

        @Test
        @DisplayName("현재 비밀번호가 맞고 새 비밀번호가 규칙을 만족하면 변경하고 임시 비밀번호 상태를 해제한다")
        void changePassword_valid_changesAndClearsReset() {
            // given
            given(passwordEncoder.matches("Temp1234!", "{bcrypt}temp")).willReturn(true);
            given(passwordEncoder.encode("newPassword1!")).willReturn("{bcrypt}new");

            // when
            memberService.changePassword(
                    1L, new PasswordChangeRequest("Temp1234!", "newPassword1!", "newPassword1!"));

            // then
            assertThat(member.getPassword()).isEqualTo("{bcrypt}new");
            assertThat(member.isPasswordResetRequired()).isFalse();
            assertThat(member.getUpdatedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 새 비밀번호 형식보다 먼저 MEMBER_PASSWORD_MISMATCH 예외가 발생한다")
        void changePassword_wrongCurrent_throwsBeforeRegexCheck() {
            // given
            given(passwordEncoder.matches("wrong", "{bcrypt}temp")).willReturn(false);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberService.changePassword(
                                            1L, new PasswordChangeRequest("wrong", "short", "x")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("새 비밀번호가 규칙에 맞지 않으면 newPassword 필드 에러와 INVALID_INPUT 예외가 발생한다")
        void changePassword_invalidNewPassword_throwsInvalidInput() {
            // given
            given(passwordEncoder.matches("Temp1234!", "{bcrypt}temp")).willReturn(true);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberService.changePassword(
                                            1L,
                                            new PasswordChangeRequest(
                                                    "Temp1234!", "onlyletters", "onlyletters")))
                    .isInstanceOfSatisfying(
                            BusinessException.class,
                            e -> {
                                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                                assertThat(e.getFieldErrors())
                                        .extracting(ErrorResponse.FieldError::field)
                                        .containsExactly("newPassword");
                            });
        }

        @Test
        @DisplayName("새 비밀번호 확인이 다르면 MEMBER_PASSWORD_CONFIRM_MISMATCH 예외가 발생한다")
        void changePassword_confirmMismatch_throws() {
            // given
            given(passwordEncoder.matches("Temp1234!", "{bcrypt}temp")).willReturn(true);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberService.changePassword(
                                            1L,
                                            new PasswordChangeRequest(
                                                    "Temp1234!", "newPassword1!", "other1234!")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_PASSWORD_CONFIRM_MISMATCH);
            assertThat(member.isPasswordResetRequired()).isTrue();
        }
    }

    @Nested
    @DisplayName("비밀번호 재확인·탈퇴")
    class VerifyAndWithdraw {

        @Test
        @DisplayName("비밀번호가 맞으면 개인정보를 반환한다")
        void verifyPasswordAndGetInfo_match_returnsInfo() {
            // given
            Member member = MemberFixture.create(1L, "a@b.com", "기택");
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
            given(passwordEncoder.matches("password1!", member.getPassword())).willReturn(true);

            // when
            MemberInfoResponse response =
                    memberService.verifyPasswordAndGetInfo(
                            1L, new PasswordVerificationRequest("password1!"));

            // then
            assertThat(response.email()).isEqualTo("a@b.com");
            assertThat(response.nickname()).isEqualTo("기택");
        }

        @Test
        @DisplayName("비밀번호가 틀리면 MEMBER_PASSWORD_MISMATCH 예외가 발생한다")
        void verifyPasswordAndGetInfo_mismatch_throws() {
            // given
            Member member = MemberFixture.create(1L);
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
            given(passwordEncoder.matches("wrong", member.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(
                            () ->
                                    memberService.verifyPasswordAndGetInfo(
                                            1L, new PasswordVerificationRequest("wrong")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_PASSWORD_MISMATCH);
        }

        @Test
        @DisplayName("비밀번호가 맞으면 소프트 삭제한다")
        void withdraw_match_softDeletes() {
            // given
            Member member = MemberFixture.create(1L);
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
            given(passwordEncoder.matches("password1!", member.getPassword())).willReturn(true);

            // when
            memberService.withdraw(1L, new WithdrawalRequest("password1!"));

            // then
            assertThat(member.getDeletedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 탈퇴하지 않고 MEMBER_PASSWORD_MISMATCH 예외가 발생한다")
        void withdraw_mismatch_throws() {
            // given
            Member member = MemberFixture.create(1L);
            given(memberRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(member));
            given(passwordEncoder.matches("wrong", member.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> memberService.withdraw(1L, new WithdrawalRequest("wrong")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_PASSWORD_MISMATCH);
            assertThat(member.isWithdrawn()).isFalse();
        }
    }

    private static DataIntegrityViolationException uniqueViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "duplicate key",
                new ConstraintViolationException(
                        "duplicate key", new SQLException("duplicate key"), constraintName));
    }
}
