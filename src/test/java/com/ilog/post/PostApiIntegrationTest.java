package com.ilog.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.ilog.post.dto.PostSearchRequest;
import com.ilog.post.service.PostService;
import com.ilog.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class PostApiIntegrationTest extends IntegrationTestSupport {

    @Autowired PostService postService;
    @Autowired EntityManagerFactory entityManagerFactory;

    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        ownerToken = signupAndLogin("owner@ilog.com", "기택");
        otherToken = signupAndLogin("other@ilog.com", "다른사람");
    }

    @Test
    @DisplayName("게시글 등록 → 상세 → 검색 → 수정 → 해시태그 추가·삭제 → 삭제 전체 흐름")
    void postLifecycle() {
        // 등록
        Long postId =
                createPost(
                        ownerToken,
                        """
                        {"title": "JWT 인증 정리", "content": "오늘의 학습",
                         "urls": ["https://b.com", "https://a.com"], "hashtags": ["#JWT", "Spring", "JWT"]}
                        """);

        // 상세: URL 순서 유지, 태그 정규화·중복 제거, 수정 시각 null, 본인 글
        restTestClient
                .get()
                .uri("/api/v1/posts/{postId}", postId)
                .header(AUTHORIZATION, bearer(ownerToken))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.title")
                .isEqualTo("JWT 인증 정리")
                .jsonPath("$.data.urls")
                .isEqualTo(List.of("https://b.com", "https://a.com"))
                .jsonPath("$.data.hashtags[*].name")
                .isEqualTo(List.of("JWT", "Spring"))
                .jsonPath("$.data.nickname")
                .isEqualTo("기택")
                .jsonPath("$.data.createdAt")
                .isNotEmpty()
                .jsonPath("$.data.updatedAt")
                .isEmpty()
                .jsonPath("$.data.isMine")
                .isEqualTo(true);

        // 검색: 닉네임 + 태그
        restTestClient
                .get()
                .uri("/api/v1/posts?nickname={nickname}&hashtags={tag}", "기택", "Spring")
                .header(AUTHORIZATION, bearer(otherToken))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.content[0].postId")
                .isEqualTo(postId)
                .jsonPath("$.data.content[0].hashtags")
                .isEqualTo(List.of("JWT", "Spring"))
                .jsonPath("$.data.totalElements")
                .isEqualTo(1);

        // 수정: urls 전체 교체, hashtags 필드는 무시
        restTestClient
                .patch()
                .uri("/api/v1/posts/{postId}", postId)
                .header(AUTHORIZATION, bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"content": "수정한 내용", "urls": ["https://c.com"], "hashtags": ["무시"]}
                        """)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.title")
                .isEqualTo("JWT 인증 정리")
                .jsonPath("$.data.content")
                .isEqualTo("수정한 내용")
                .jsonPath("$.data.urls")
                .isEqualTo(List.of("https://c.com"))
                .jsonPath("$.data.hashtags[*].name")
                .isEqualTo(List.of("JWT", "Spring"))
                .jsonPath("$.data.updatedAt")
                .isNotEmpty();

        // 해시태그 추가: 이미 있는 태그는 무시하고 전체 목록 반환
        String addBody =
                restTestClient
                        .post()
                        .uri("/api/v1/posts/{postId}/hashtags", postId)
                        .header(AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                """
                                {"names": ["JWT", "#JPA"]}
                                """)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        List<String> names = JsonPath.read(addBody, "$.data[*].name");
        assertThat(names).containsExactly("JWT", "Spring", "JPA");
        Integer jpaTagId = JsonPath.read(addBody, "$.data[2].hashtagId");

        // 해시태그 삭제
        restTestClient
                .delete()
                .uri("/api/v1/posts/{postId}/hashtags/{hashtagId}", postId, jpaTagId)
                .header(AUTHORIZATION, bearer(ownerToken))
                .exchange()
                .expectStatus()
                .isNoContent();
        restTestClient
                .delete()
                .uri("/api/v1/posts/{postId}/hashtags/{hashtagId}", postId, jpaTagId)
                .header(AUTHORIZATION, bearer(ownerToken))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("HASHTAG_NOT_FOUND");

        // 삭제
        restTestClient
                .delete()
                .uri("/api/v1/posts/{postId}", postId)
                .header(AUTHORIZATION, bearer(ownerToken))
                .exchange()
                .expectStatus()
                .isNoContent();
        restTestClient
                .get()
                .uri("/api/v1/posts/{postId}", postId)
                .header(AUTHORIZATION, bearer(ownerToken))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("POST_NOT_FOUND");
    }

    @Test
    @DisplayName("타인의 게시글은 조회만 가능하고 수정·삭제·태그 변경은 403 이다")
    void otherMember_cannotModify() {
        // given
        Long postId =
                createPost(
                        ownerToken,
                        """
                        {"title": "제목", "content": "내용", "hashtags": ["JWT"]}
                        """);
        String detail =
                restTestClient
                        .get()
                        .uri("/api/v1/posts/{postId}", postId)
                        .header(AUTHORIZATION, bearer(otherToken))
                        .exchange()
                        .expectStatus()
                        .isOk()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        Integer tagId = JsonPath.read(detail, "$.data.hashtags[0].hashtagId");

        // when & then
        assertThat((Boolean) JsonPath.read(detail, "$.data.isMine")).isFalse();
        restTestClient
                .patch()
                .uri("/api/v1/posts/{postId}", postId)
                .header(AUTHORIZATION, bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"title": "탈취"}
                        """)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("POST_NOT_OWNER");
        restTestClient
                .delete()
                .uri("/api/v1/posts/{postId}", postId)
                .header(AUTHORIZATION, bearer(otherToken))
                .exchange()
                .expectStatus()
                .isForbidden();
        restTestClient
                .post()
                .uri("/api/v1/posts/{postId}/hashtags", postId)
                .header(AUTHORIZATION, bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"names": ["탈취"]}
                        """)
                .exchange()
                .expectStatus()
                .isForbidden();
        restTestClient
                .delete()
                .uri("/api/v1/posts/{postId}/hashtags/{hashtagId}", postId, tagId)
                .header(AUTHORIZATION, bearer(otherToken))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    @DisplayName("다른 게시글의 해시태그 ID 로 삭제하면 404 이다")
    void deleteHashtag_ofOtherPost_returns404() {
        // given
        Long myPost =
                createPost(
                        ownerToken,
                        """
                        {"title": "내 글", "content": "내용"}
                        """);
        Long otherPost =
                createPost(
                        ownerToken,
                        """
                        {"title": "다른 글", "content": "내용", "hashtags": ["JWT"]}
                        """);
        Integer otherTagId =
                JsonPath.read(
                        restTestClient
                                .get()
                                .uri("/api/v1/posts/{postId}", otherPost)
                                .header(AUTHORIZATION, bearer(ownerToken))
                                .exchange()
                                .expectBody(String.class)
                                .returnResult()
                                .getResponseBody(),
                        "$.data.hashtags[0].hashtagId");

        // when & then
        restTestClient
                .delete()
                .uri("/api/v1/posts/{postId}/hashtags/{hashtagId}", myPost, otherTagId)
                .header(AUTHORIZATION, bearer(ownerToken))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("HASHTAG_NOT_FOUND");
    }

    @Test
    @DisplayName("목록 조회는 게시글 수와 관계없이 쿼리 수가 고정이다 (N+1 없음)")
    void searchPosts_queryCountIsConstant() {
        // given
        for (int i = 0; i < 6; i++) {
            String token = i % 2 == 0 ? ownerToken : otherToken;
            createPost(
                    token,
                    """
                    {"title": "글 %d", "content": "내용", "urls": ["https://a.com"], "hashtags": ["T%d", "공통"]}
                    """
                            .formatted(i, i));
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        // when
        var page = postService.search(PostSearchRequest.empty());

        // then: 본문(작성자 fetch join) 1 + count 1 + 태그 일괄 조회 1
        assertThat(page.content()).hasSize(6);
        assertThat(page.content()).allSatisfy(post -> assertThat(post.hashtags()).hasSize(2));
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("상세 조회도 쿼리 수가 고정이다")
    void getPost_queryCountIsConstant() {
        // given
        Long postId =
                createPost(
                        ownerToken,
                        """
                        {"title": "제목", "content": "내용", "urls": ["https://a.com", "https://b.com"],
                         "hashtags": ["A", "B", "C"]}
                        """);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        // when
        var detail = postService.getPost(postId, 1L);

        // then: 게시글 1 + 작성자 1 + URL 1 + 태그 1
        assertThat(detail.hashtags()).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    @Test
    @DisplayName("게시글 등록 요청 검증에 실패하면 400 과 fieldErrors 를 반환한다")
    void createPost_invalid_returns400() {
        // when & then
        restTestClient
                .post()
                .uri("/api/v1/posts")
                .header(AUTHORIZATION, bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        """
                        {"title": "", "content": "내용", "hashtags": ["#"]}
                        """)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("INVALID_INPUT")
                .jsonPath("$.error.fieldErrors[0].field")
                .isEqualTo("title");
    }

    private Long createPost(String token, String body) {
        String response =
                restTestClient
                        .post()
                        .uri("/api/v1/posts")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .exchange()
                        .expectStatus()
                        .isCreated()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();
        return JsonPath.parse(response).read("$.data.postId", Long.class);
    }
}
