package com.ilog.member.service;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.global.policy.MemberPolicy;
import com.ilog.member.dto.MemberInfoResponse;
import com.ilog.member.dto.NicknameUpdateRequest;
import com.ilog.member.dto.NicknameUpdateResponse;
import com.ilog.member.dto.PasswordChangeRequest;
import com.ilog.member.dto.PasswordVerificationRequest;
import com.ilog.member.dto.SignupRequest;
import com.ilog.member.dto.WithdrawalRequest;
import com.ilog.member.entity.Member;
import com.ilog.member.repository.MemberRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    static final String EMAIL_UNIQUE_CONSTRAINT = "uk_member_email_active";
    static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_member_nickname";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    /** 회원가입. 형식 검증(DTO) → 비밀번호 확인 → 이메일 중복 → 닉네임 중복 → 해싱 → 저장. */
    @Transactional
    public Long signup(SignupRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_CONFIRM_MISMATCH);
        }
        if (memberRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }
        Member member =
                Member.create(
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.name(),
                        request.nickname());
        try {
            return memberRepository.saveAndFlush(member).getId();
        } catch (DataIntegrityViolationException e) {
            // 사전 조회 이후 동시 가입이 먼저 커밋된 경우
            throw translateDuplicate(e);
        }
    }

    // TODO(D-11): 탈퇴 회원 이메일을 사용 가능으로 볼지 확정 시 조정. [잠정] 활성 회원 기준
    public boolean isEmailAvailable(String email) {
        return !memberRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    /** [제안] 로그인 상태에서 본인의 현재 닉네임을 넣으면 사용 가능으로 본다. */
    public boolean isNicknameAvailable(String nickname, @Nullable Long loginMemberId) {
        if (loginMemberId != null && isCurrentNickname(loginMemberId, nickname)) {
            return true;
        }
        return !memberRepository.existsByNickname(nickname);
    }

    // TODO(D-15): 수정 화면에도 비밀번호 재확인을 적용할지 확정 시 조정. [잠정] 미적용
    @Transactional
    public NicknameUpdateResponse changeNickname(Long memberId, NicknameUpdateRequest request) {
        Member member = getActiveMember(memberId);
        String nickname = request.nickname();
        if (!member.getNickname().equals(nickname) && memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }
        member.changeNickname(nickname, clock.instant());
        try {
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw translateDuplicate(e);
        }
        return NicknameUpdateResponse.from(member);
    }

    /** 비밀번호 변경. [확정] 현재 비밀번호 확인 → 정규식 → 확인 일치 → 해싱 갱신 → 임시 비밀번호 상태 해제 → updated_at 갱신. */
    @Transactional
    public void changePassword(Long memberId, PasswordChangeRequest request) {
        Member member = getActiveMember(memberId);
        verifyPassword(member, request.currentPassword());
        if (!MemberPolicy.isValidPassword(request.newPassword())) {
            throw BusinessException.invalidField("newPassword", MemberPolicy.PASSWORD_MESSAGE);
        }
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_CONFIRM_MISMATCH);
        }
        // TODO(D-13): 이전 비밀번호 재사용 금지(password history) 확정 시 추가
        member.changePassword(passwordEncoder.encode(request.newPassword()), clock.instant());
    }

    public MemberInfoResponse verifyPasswordAndGetInfo(
            Long memberId, PasswordVerificationRequest request) {
        Member member = getActiveMember(memberId);
        verifyPassword(member, request.password());
        return MemberInfoResponse.from(member);
    }

    /**
     * 회원 탈퇴(소프트 삭제).
     *
     * <p>TODO: 탈퇴 회원의 게시글 노출 정책은 명세에 없음. [잠정] 게시글은 그대로 노출
     */
    @Transactional
    public void withdraw(Long memberId, WithdrawalRequest request) {
        Member member = getActiveMember(memberId);
        verifyPassword(member, request.password());
        member.withdraw(clock.instant());
    }

    private Member getActiveMember(Long memberId) {
        return memberRepository
                .findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private boolean isCurrentNickname(Long memberId, String nickname) {
        return memberRepository
                .findByIdAndDeletedAtIsNull(memberId)
                .map(member -> member.getNickname().equals(nickname))
                .orElse(false);
    }

    private void verifyPassword(Member member, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new BusinessException(ErrorCode.MEMBER_PASSWORD_MISMATCH);
        }
    }

    private static RuntimeException translateDuplicate(DataIntegrityViolationException e) {
        String constraintName =
                e.getCause() instanceof ConstraintViolationException violation
                        ? violation.getConstraintName()
                        : null;
        if (EMAIL_UNIQUE_CONSTRAINT.equals(constraintName)) {
            return new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        if (NICKNAME_UNIQUE_CONSTRAINT.equals(constraintName)) {
            return new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }
        return e;
    }
}
