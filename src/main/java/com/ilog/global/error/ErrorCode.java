package com.ilog.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    // [제안] 없는 경로·메서드·미디어 타입이 500 으로 떨어지지 않게 추가
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 자원을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // auth
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    AUTH_PASSWORD_RESET_REQUIRED(HttpStatus.FORBIDDEN, "비밀번호 변경이 필요합니다."),

    // member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    MEMBER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    MEMBER_NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    MEMBER_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    MEMBER_PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),

    // post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    POST_NOT_OWNER(HttpStatus.FORBIDDEN, "작성자만 수정·삭제할 수 있습니다."),

    // hashtag
    HASHTAG_NOT_FOUND(HttpStatus.NOT_FOUND, "해시태그를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
