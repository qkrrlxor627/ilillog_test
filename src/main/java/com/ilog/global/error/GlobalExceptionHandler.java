package com.ilog.global.error;

import com.ilog.global.response.ApiResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 모든 예외를 {@link ApiResponse} 실패 형식으로 변환하는 유일한 지점. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return respond(e.getErrorCode(), e.getFieldErrors());
    }

    /**
     * {@code @Valid @RequestBody} 와 {@code @Valid @ModelAttribute} (MethodArgumentNotValidException
     * 포함).
     */
    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiResponse<Void>> handleBind(BindException e) {
        List<ErrorResponse.FieldError> fieldErrors =
                e.getBindingResult().getFieldErrors().stream()
                        .map(
                                error ->
                                        new ErrorResponse.FieldError(
                                                error.getField(), reasonOf(error)))
                        .toList();
        return respond(ErrorCode.INVALID_INPUT, fieldErrors);
    }

    /** {@code @RequestParam}, {@code @PathVariable} 에 붙은 제약 조건 위반. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        List<ErrorResponse.FieldError> fieldErrors =
                e.getParameterValidationResults().stream()
                        .flatMap(
                                result ->
                                        result.getResolvableErrors().stream()
                                                .map(
                                                        error ->
                                                                new ErrorResponse.FieldError(
                                                                        result.getMethodParameter()
                                                                                .getParameterName(),
                                                                        error.getDefaultMessage())))
                        .toList();
        return respond(ErrorCode.INVALID_INPUT, fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException e) {
        return respond(
                ErrorCode.INVALID_INPUT,
                List.of(new ErrorResponse.FieldError(e.getParameterName(), "필수 값입니다.")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(
                ErrorCode.INVALID_INPUT,
                List.of(new ErrorResponse.FieldError(e.getName(), "형식이 올바르지 않습니다.")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return respond(ErrorCode.INVALID_INPUT, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e) {
        return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, List.of());
    }

    /** 500 응답에는 스택트레이스·SQL 을 절대 싣지 않는다. 로그로만 남긴다. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return respond(ErrorCode.INTERNAL_ERROR, List.of());
    }

    private static ResponseEntity<ApiResponse<Void>> respond(
            ErrorCode errorCode, List<ErrorResponse.FieldError> fieldErrors) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(ErrorResponse.of(errorCode, fieldErrors)));
    }

    private static String reasonOf(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "올바르지 않은 값입니다.";
    }
}
