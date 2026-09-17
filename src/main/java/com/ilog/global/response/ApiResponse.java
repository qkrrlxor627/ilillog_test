package com.ilog.global.response;

import com.ilog.global.error.ErrorResponse;

/** 모든 API 공통 응답 래퍼. [잠정] 프론트 회의 결과에 따라 이 클래스만 바꾼다. (CLAUDE.md 5장) */
public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}
