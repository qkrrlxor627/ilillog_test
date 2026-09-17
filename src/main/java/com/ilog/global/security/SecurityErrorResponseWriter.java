package com.ilog.global.security;

import com.ilog.global.error.ErrorCode;
import com.ilog.global.error.ErrorResponse;
import com.ilog.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/** 필터 단계(컨트롤러 밖)에서 발생한 인증·인가 실패를 {@link ApiResponse} 형식으로 쓴다. */
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final JsonMapper jsonMapper;

    public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(
                response.getOutputStream(), ApiResponse.fail(ErrorResponse.of(errorCode)));
    }
}
