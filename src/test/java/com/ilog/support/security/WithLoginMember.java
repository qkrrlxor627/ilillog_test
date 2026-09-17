package com.ilog.support.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/** 테스트에서 {@code @AuthenticationPrincipal LoginMember} 로 주입될 인증 주체를 설정한다. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithLoginMemberSecurityContextFactory.class)
public @interface WithLoginMember {

    long memberId() default 1L;

    boolean passwordResetRequired() default false;
}
