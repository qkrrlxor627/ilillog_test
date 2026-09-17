package com.ilog.auth.service;

import com.ilog.auth.dto.LoginRequest;
import com.ilog.auth.dto.TemporaryPasswordRequest;
import com.ilog.auth.dto.TokenResponse;
import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.member.entity.Member;
import com.ilog.member.repository.MemberRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final TemporaryPasswordSender temporaryPasswordSender;
    private final Clock clock;

    /** 로그인. 이메일 없음·비밀번호 불일치·탈퇴 회원은 모두 같은 에러로 응답한다. (이메일 존재 여부 비노출) */
    public TokenResponse login(LoginRequest request) {
        Member member =
                memberRepository
                        .findByEmailAndDeletedAtIsNull(request.email())
                        .filter(m -> passwordEncoder.matches(request.password(), m.getPassword()))
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        // TODO(D-12): 임시 비밀번호 유효기간 확정 시 만료 검사 추가. [잠정] 만료 없음
        TokenService.IssuedToken token = tokenService.issue(member.getId());
        return TokenResponse.of(token, member.isPasswordResetRequired());
    }

    public void logout(Long memberId) {
        tokenService.revoke(memberId);
    }

    /**
     * 이메일·이름이 모두 일치하는 활성 회원에게만 임시 비밀번호를 발급한다.
     *
     * <p>TODO(D-17): 안내 문구와 불일치 시 응답 정책 확정 시 조정. [잠정] 불일치 시 404 MEMBER_NOT_FOUND
     */
    @Transactional
    public void issueTemporaryPassword(TemporaryPasswordRequest request) {
        Member member =
                memberRepository
                        .findByEmailAndDeletedAtIsNull(request.email())
                        .filter(m -> m.getName().equals(request.name()))
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String temporaryPassword = temporaryPasswordGenerator.generate();
        member.resetToTemporaryPassword(passwordEncoder.encode(temporaryPassword), clock.instant());
        temporaryPasswordSender.send(member.getEmail(), temporaryPassword);
    }
}
