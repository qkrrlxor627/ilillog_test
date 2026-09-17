package com.ilog.global.error;

import java.util.List;

public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) {

    public record FieldError(String field, String reason) {}

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }
}
