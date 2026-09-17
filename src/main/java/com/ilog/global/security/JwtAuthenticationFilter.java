package com.ilog.global.security;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer {token}} 을 검증해 SecurityContext 에 {@link LoginMember} 를 채운다.
 *
 * <p>서블릿 필터로 이중 등록되지 않도록 빈이 아니라 SecurityConfig 에서 직접 생성한다. 토큰이 잘못돼도 여기서 바로 거절하지 않고, 인증이 필요한 요청일 때
 * EntryPoint 가 {@link #AUTH_ERROR_ATTRIBUTE} 를 보고 응답한다. (permitAll API 는 비로그인으로 진행)
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE =
            JwtAuthenticationFilter.class.getName() + ".error";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<SimpleGrantedAuthority> AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));

    private final JwtProvider jwtProvider;
    private final LoginMemberLoader loginMemberLoader;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            authenticate(request, token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            Long memberId = jwtProvider.parseMemberId(token);
            loginMemberLoader
                    .loadActiveMember(memberId)
                    .ifPresentOrElse(
                            this::setAuthentication,
                            () ->
                                    request.setAttribute(
                                            AUTH_ERROR_ATTRIBUTE, ErrorCode.UNAUTHORIZED));
        } catch (BusinessException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
        }
    }

    private void setAuthentication(LoginMember loginMember) {
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(loginMember, null, AUTHORITIES));
        securityContextHolderStrategy.setContext(context);
    }

    private static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
