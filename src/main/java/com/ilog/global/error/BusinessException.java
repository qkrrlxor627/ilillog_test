package com.ilog.global.error;

import java.util.List;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorResponse.FieldError> fieldErrors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public BusinessException(ErrorCode errorCode, List<ErrorResponse.FieldError> fieldErrors) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    /** 서비스 레이어에서 검증한 입력값 오류 (예: 순서가 확정된 비밀번호 정규식 검사). */
    public static BusinessException invalidField(String field, String reason) {
        return new BusinessException(
                ErrorCode.INVALID_INPUT, List.of(new ErrorResponse.FieldError(field, reason)));
    }
}
