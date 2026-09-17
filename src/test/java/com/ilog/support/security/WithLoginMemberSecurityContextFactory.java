package com.ilog.support.security;

import com.ilog.global.security.LoginMember;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithLoginMemberSecurityContextFactory
        implements WithSecurityContextFactory<WithLoginMember> {

    @Override
    public SecurityContext createSecurityContext(WithLoginMember annotation) {
        LoginMember loginMember =
                new LoginMember(annotation.memberId(), annotation.passwordResetRequired());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        loginMember, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))));
        return context;
    }
}
