package com.ilog.auth.controller;

import com.ilog.auth.dto.LoginRequest;
import com.ilog.auth.dto.TemporaryPasswordRequest;
import com.ilog.auth.dto.TemporaryPasswordResponse;
import com.ilog.auth.dto.TokenResponse;
import com.ilog.auth.service.AuthService;
import com.ilog.global.response.ApiResponse;
import com.ilog.global.security.LoginMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "로그인·로그아웃·임시 비밀번호")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인 (FN-MBR-002)")
    @PostMapping("/tokens")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    // 명세 불일치: 엔드포인트 DB 는 POST /auth/tokens 이나 로그인과 겹쳐 [잠정] DELETE 로 구현
    @Operation(summary = "로그아웃")
    @DeleteMapping("/tokens")
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember) {
        authService.logout(loginMember.memberId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "임시 비밀번호 발급 (FN-MBR-003)", description = "이메일·이름이 모두 일치할 때만 발급")
    @PostMapping("/temporary-passwords")
    public ApiResponse<TemporaryPasswordResponse> issueTemporaryPassword(
            @Valid @RequestBody TemporaryPasswordRequest request) {
        authService.issueTemporaryPassword(request);
        return ApiResponse.ok(TemporaryPasswordResponse.issued());
    }
}
