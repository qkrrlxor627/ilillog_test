package com.ilog.global.security;

import com.ilog.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 인증이 필요한 요청에 유효한 토큰이 없을 때 401. 만료 토큰이면 {@code AUTH_TOKEN_EXPIRED}. */
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        ErrorCode errorCode =
                request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE)
                                instanceof ErrorCode code
                        ? code
                        : ErrorCode.UNAUTHORIZED;
        errorResponseWriter.write(response, errorCode);
    }
}
