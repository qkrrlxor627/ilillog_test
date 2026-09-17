package com.ilog.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TemporaryPasswordResponse(
        @Schema(example = "임시 비밀번호가 발급되었습니다. 로그인 후 비밀번호를 변경해 주세요.") String message) {

    // TODO(D-17): 임시 비밀번호 안내 문구 확정 시 교체
    private static final String DEFAULT_MESSAGE = "임시 비밀번호가 발급되었습니다. 로그인 후 비밀번호를 변경해 주세요.";

    public static TemporaryPasswordResponse issued() {
        return new TemporaryPasswordResponse(DEFAULT_MESSAGE);
    }
}
