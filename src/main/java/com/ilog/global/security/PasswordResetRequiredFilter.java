package com.ilog.global.security;

import com.ilog.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * [제안] 임시 비밀번호로 로그인한 회원은 비밀번호 변경·로그아웃 외 요청을 403 {@code AUTH_PASSWORD_RESET_REQUIRED} 로 막는다.
 * (CLAUDE.md 7-2 로그인)
 */
@RequiredArgsConstructor
public class PasswordResetRequiredFilter extends OncePerRequestFilter {

    private static final List<RequestMatcher> ALLOWED =
            List.of(
                    PathPatternRequestMatcher.pathPattern(
                            HttpMethod.PATCH, "/api/v1/users/me/password"),
                    PathPatternRequestMatcher.pathPattern(
                            HttpMethod.DELETE, "/api/v1/auth/tokens"));

    private final SecurityErrorResponseWriter errorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (requiresPasswordReset() && ALLOWED.stream().noneMatch(m -> m.matches(request))) {
            errorResponseWriter.write(response, ErrorCode.AUTH_PASSWORD_RESET_REQUIRED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean requiresPasswordReset() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof LoginMember loginMember
                && loginMember.passwordResetRequired();
    }
}
