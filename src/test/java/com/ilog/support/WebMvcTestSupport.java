package com.ilog.support;

import com.ilog.global.config.SecurityConfig;
import com.ilog.global.security.JwtProvider;
import com.ilog.global.security.LoginMemberLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 컨트롤러 웹 슬라이스 테스트 공통 설정. 실제 SecurityConfig(필터·401/403 핸들러)를 올리고 토큰 검증 의존성만 목으로 둔다.
 *
 * <p>하위 클래스는 {@code @WebMvcTest(XxxController.class)} 를 붙이고, 인증은 {@code @WithLoginMember} 로 주입한다.
 */
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public abstract class WebMvcTestSupport {

    @Autowired protected MockMvc mockMvc;

    @MockitoBean protected JwtProvider jwtProvider;

    @MockitoBean protected LoginMemberLoader loginMemberLoader;
}
