package com.ilog.support;

import com.ilog.auth.service.TemporaryPasswordSender;
import com.ilog.support.container.PostgresTestContainer;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 인증 포함 전체 흐름 통합 테스트 공통 설정. 모든 하위 클래스가 같은 컨텍스트·컨테이너를 공유한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @ServiceConnection static final PostgreSQLContainer POSTGRES = PostgresTestContainer.INSTANCE;

    @Autowired protected RestTestClient restTestClient;

    @Autowired private JdbcTemplate jdbcTemplate;

    /** 발급된 임시 비밀번호를 캡처하기 위한 스파이 (컨텍스트 공유를 위해 공통 클래스에 둔다). */
    @MockitoSpyBean protected TemporaryPasswordSender temporaryPasswordSender;

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE post_hashtag, post_url, post, member RESTART IDENTITY CASCADE");
    }

    protected Long signup(String email, String password, String name, String nickname) {
        String body =
                restTestClient
                        .post()
                        .uri("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {"email": "%s", "password": "%s", "passwordConfirm": "%s", "name": "%s", "nickname": "%s"}
                                """
                                        .formatted(email, password, password, name, nickname))
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        return JsonPath.parse(body).read("$.data.memberId", Long.class);
    }

    protected String login(String email, String password) {
        String body =
                restTestClient
                        .post()
                        .uri("/api/v1/auth/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {"email": "%s", "password": "%s"}
                                """
                                        .formatted(email, password))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        return JsonPath.read(body, "$.data.accessToken");
    }

    protected String signupAndLogin(String email, String nickname) {
        signup(email, DEFAULT_PASSWORD, "테스터", nickname);
        return login(email, DEFAULT_PASSWORD);
    }

    protected static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    protected static final String DEFAULT_PASSWORD = "password1!";
    protected static final String AUTHORIZATION = HttpHeaders.AUTHORIZATION;
}
