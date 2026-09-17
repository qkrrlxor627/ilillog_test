package com.ilog.member.controller;

import com.ilog.global.response.ApiResponse;
import com.ilog.global.security.LoginMember;
import com.ilog.member.dto.AvailabilityResponse;
import com.ilog.member.dto.EmailAvailabilityRequest;
import com.ilog.member.dto.MemberIdResponse;
import com.ilog.member.dto.MemberInfoResponse;
import com.ilog.member.dto.NicknameAvailabilityRequest;
import com.ilog.member.dto.NicknameUpdateRequest;
import com.ilog.member.dto.NicknameUpdateResponse;
import com.ilog.member.dto.PasswordChangeRequest;
import com.ilog.member.dto.PasswordVerificationRequest;
import com.ilog.member.dto.SignupRequest;
import com.ilog.member.dto.WithdrawalRequest;
import com.ilog.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원", description = "회원가입·중복 확인·내 정보")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class MemberController {

    private static final URI MY_INFO_LOCATION = URI.create("/api/v1/users/me");

    private final MemberService memberService;

    @Operation(summary = "회원가입 (FN-MBR-001)")
    @PostMapping
    public ResponseEntity<ApiResponse<MemberIdResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        Long memberId = memberService.signup(request);
        return ResponseEntity.created(MY_INFO_LOCATION)
                .body(ApiResponse.ok(new MemberIdResponse(memberId)));
    }

    @Operation(summary = "이메일 중복 확인 (FN-MBR-001)")
    @GetMapping("/email-availability")
    public ApiResponse<AvailabilityResponse> checkEmailAvailability(
            @Valid @ModelAttribute EmailAvailabilityRequest request) {
        return ApiResponse.ok(
                new AvailabilityResponse(memberService.isEmailAvailable(request.email())));
    }

    @Operation(
            summary = "닉네임 중복 확인 (FN-MBR-001, 005)",
            description = "로그인 상태에서 본인의 현재 닉네임은 사용 가능으로 응답")
    @GetMapping("/nickname-availability")
    public ApiResponse<AvailabilityResponse> checkNicknameAvailability(
            @Valid @ModelAttribute NicknameAvailabilityRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember) {
        Long loginMemberId = loginMember == null ? null : loginMember.memberId();
        return ApiResponse.ok(
                new AvailabilityResponse(
                        memberService.isNicknameAvailable(request.nickname(), loginMemberId)));
    }

    @Operation(summary = "닉네임 수정 (FN-MBR-005)")
    @PatchMapping("/me")
    public ApiResponse<NicknameUpdateResponse> changeNickname(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody NicknameUpdateRequest request) {
        return ApiResponse.ok(memberService.changeNickname(loginMember.memberId(), request));
    }

    @Operation(summary = "비밀번호 변경 (FN-MBR-005)", description = "임시 비밀번호 로그인 후 강제 변경도 이 API")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody PasswordChangeRequest request) {
        memberService.changePassword(loginMember.memberId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "비밀번호 재확인 + 개인정보 조회 (FN-MBR-004)")
    @PostMapping("/me/password-verification")
    public ApiResponse<MemberInfoResponse> verifyPassword(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody PasswordVerificationRequest request) {
        return ApiResponse.ok(
                memberService.verifyPasswordAndGetInfo(loginMember.memberId(), request));
    }

    @Operation(summary = "회원 탈퇴 (FN-MBR-006)", description = "DELETE 는 body 를 받지 않으므로 POST 로 처리")
    @PostMapping("/me/withdrawal")
    public ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody WithdrawalRequest request) {
        memberService.withdraw(loginMember.memberId(), request);
        return ResponseEntity.noContent().build();
    }
}
