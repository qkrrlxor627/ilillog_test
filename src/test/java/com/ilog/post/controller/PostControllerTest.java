package com.ilog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.global.response.PageResponse;
import com.ilog.hashtag.dto.HashtagResponse;
import com.ilog.post.dto.PostDetailResponse;
import com.ilog.post.dto.PostSearchRequest;
import com.ilog.post.dto.PostSummaryResponse;
import com.ilog.post.dto.PostUpdateRequest;
import com.ilog.post.service.PostService;
import com.ilog.support.WebMvcTestSupport;
import com.ilog.support.security.WithLoginMember;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(PostController.class)
class PostControllerTest extends WebMvcTestSupport {

    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");

    @MockitoBean PostService postService;

    @Nested
    @DisplayName("POST /api/v1/posts 게시글 등록")
    class CreatePost {

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 201, Location, 게시글 ID 를 반환한다")
        void createPost_valid_returns201() throws Exception {
            // given
            given(postService.create(eq(1L), any())).willReturn(12L);

            // when & then
            mockMvc.perform(
                            post("/api/v1/posts")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "title": "JWT", "content": "내용", "urls": ["https://a.com"], "hashtags": ["JWT"] }
                                            """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/posts/12"))
                    .andExpect(jsonPath("$.data.postId").value(12));
        }

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("제목이 비어 있으면 400과 fieldErrors를 반환한다")
        void createPost_titleBlank_returns400() throws Exception {
            // given
            String body =
                    """
                    { "title": "", "content": "내용", "urls": [], "hashtags": [] }
                    """;

            // when & then
            mockMvc.perform(
                            post("/api/v1/posts")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("title"));
        }

        @Test
        @WithLoginMember
        @DisplayName("URL 이 http(s) 형식이 아니면 400 을 반환한다")
        void createPost_invalidUrl_returns400() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/posts")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "title": "제목", "content": "내용", "urls": ["ftp://a.com"] }
                                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("urls[0]"));
            then(postService).should(never()).create(anyLong(), any());
        }

        @Test
        @DisplayName("인증 없이 등록하면 401 을 반환한다")
        void createPost_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/posts")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "title": "제목", "content": "내용" }
                                            """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        }

        @Test
        @WithLoginMember
        @DisplayName("Content-Type 이 JSON 이 아니면 415 를 반환한다")
        void createPost_unsupportedMediaType_returns415() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/posts")
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .content("title=제목"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts 목록 + 검색")
    class SearchPosts {

        @Test
        @WithLoginMember
        @DisplayName("검색 조건을 바인딩해 서비스에 넘기고 페이지 응답을 반환한다")
        void searchPosts_withConditions_returns200() throws Exception {
            // given
            PageResponse<PostSummaryResponse> page =
                    new PageResponse<>(
                            List.of(new PostSummaryResponse(12L, "JWT", "기택", NOW, List.of("JWT"))),
                            0,
                            10,
                            35,
                            4,
                            true);
            ArgumentCaptor<PostSearchRequest> captor =
                    ArgumentCaptor.forClass(PostSearchRequest.class);
            given(postService.search(captor.capture())).willReturn(page);

            // when & then
            mockMvc.perform(
                            get("/api/v1/posts")
                                    .param("title", "JWT")
                                    .param("nickname", "기택")
                                    .param("hashtags", "JWT", "Spring")
                                    .param("date", "2026-09-17")
                                    .param("page", "1")
                                    .param("size", "20")
                                    .param("sort", "title,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].postId").value(12))
                    .andExpect(jsonPath("$.data.content[0].nickname").value("기택"))
                    .andExpect(jsonPath("$.data.content[0].hashtags[0]").value("JWT"))
                    .andExpect(jsonPath("$.data.totalElements").value(35))
                    .andExpect(jsonPath("$.data.totalPages").value(4))
                    .andExpect(jsonPath("$.data.hasNext").value(true));

            PostSearchRequest request = captor.getValue();
            assertThat(request.hashtags()).containsExactly("JWT", "Spring");
            assertThat(request.date()).isEqualTo(LocalDate.of(2026, 9, 17));
            assertThat(request.toPageable().getSort())
                    .isEqualTo(Sort.by(Sort.Direction.ASC, "title"));
        }

        @Test
        @WithLoginMember
        @DisplayName("페이지 크기가 최대값을 넘으면 400 을 반환한다")
        void searchPosts_sizeTooLarge_returns400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts").param("size", "51"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("size"));
        }

        @Test
        @WithLoginMember
        @DisplayName("허용하지 않은 정렬 필드면 400 을 반환한다")
        void searchPosts_unsupportedSort_returns400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts").param("sort", "content,desc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("sort"));
        }

        @Test
        @WithLoginMember
        @DisplayName("날짜 형식이 잘못되면 400 을 반환한다")
        void searchPosts_invalidDate_returns400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts").param("date", "2026/09/17"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("date"));
        }

        @Test
        @DisplayName("인증 없이 조회하면 401 을 반환한다")
        void searchPosts_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithLoginMember(passwordResetRequired = true)
        @DisplayName("임시 비밀번호 상태면 403 AUTH_PASSWORD_RESET_REQUIRED 를 반환한다")
        void searchPosts_passwordResetRequired_returns403() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_RESET_REQUIRED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/posts/{postId} 상세")
    class GetPost {

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 200 과 상세 정보를 반환한다")
        void getPost_exists_returns200() throws Exception {
            // given
            given(postService.getPost(12L, 1L))
                    .willReturn(
                            new PostDetailResponse(
                                    12L,
                                    "JWT",
                                    "내용",
                                    List.of("https://a.com"),
                                    List.of(new HashtagResponse(3L, "JWT")),
                                    "기택",
                                    NOW,
                                    null,
                                    true));

            // when & then
            mockMvc.perform(get("/api/v1/posts/12"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.postId").value(12))
                    .andExpect(jsonPath("$.data.urls[0]").value("https://a.com"))
                    .andExpect(jsonPath("$.data.hashtags[0].hashtagId").value(3))
                    .andExpect(jsonPath("$.data.hashtags[0].name").value("JWT"))
                    .andExpect(jsonPath("$.data.createdAt").value("2026-09-17T10:00:00Z"))
                    .andExpect(jsonPath("$.data.updatedAt").isEmpty())
                    .andExpect(jsonPath("$.data.isMine").value(true));
        }

        @Test
        @WithLoginMember
        @DisplayName("없는 게시글이면 404 POST_NOT_FOUND 를 반환한다")
        void getPost_notFound_returns404() throws Exception {
            // given
            given(postService.getPost(anyLong(), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/posts/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
        }

        @Test
        @WithLoginMember
        @DisplayName("postId 가 숫자가 아니면 400 을 반환한다")
        void getPost_invalidPathVariable_returns400() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("postId"));
        }

        @Test
        @DisplayName("인증 없이 조회하면 401 을 반환한다")
        void getPost_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts/12")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithLoginMember
        @DisplayName("예상하지 못한 예외는 내부 정보 없이 500 으로 응답한다")
        void getPost_unexpectedError_returns500WithoutDetails() throws Exception {
            // given
            given(postService.getPost(anyLong(), anyLong()))
                    .willThrow(new IllegalStateException("SELECT * FROM secret"));

            // when & then
            mockMvc.perform(get("/api/v1/posts/12"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/posts/{postId} 수정")
    class UpdatePost {

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 200 과 상세 형태를 반환하고, 요청의 hashtags 는 무시한다")
        void updatePost_valid_returns200AndIgnoresHashtags() throws Exception {
            // given
            given(postService.update(eq(12L), eq(1L), any()))
                    .willReturn(
                            new PostDetailResponse(
                                    12L, "새 제목", "내용", List.of(), List.of(), "기택", NOW, NOW, true));

            // when & then
            mockMvc.perform(
                            patch("/api/v1/posts/12")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "title": "새 제목", "hashtags": ["무시"] }
                                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("새 제목"))
                    .andExpect(jsonPath("$.data.updatedAt").value("2026-09-17T10:00:00Z"));
            then(postService).should().update(12L, 1L, new PostUpdateRequest("새 제목", null, null));
        }

        @Test
        @WithLoginMember
        @DisplayName("값이 있는 제목이 공백뿐이면 400 을 반환한다")
        void updatePost_blankTitle_returns400() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/posts/12")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "title": "   " }
                                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("title"));
        }

        @Test
        @WithLoginMember(memberId = 2L)
        @DisplayName("작성자가 아니면 403 POST_NOT_OWNER 를 반환한다")
        void updatePost_notOwner_returns403() throws Exception {
            // given
            given(postService.update(anyLong(), anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_OWNER));

            // when & then
            mockMvc.perform(
                            patch("/api/v1/posts/12")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "title": "새 제목" }
                                            """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_OWNER"));
        }

        @Test
        @WithLoginMember
        @DisplayName("없는 게시글이면 404 를 반환한다")
        void updatePost_notFound_returns404() throws Exception {
            // given
            given(postService.update(anyLong(), anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            // when & then
            mockMvc.perform(
                            patch("/api/v1/posts/99")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "title": "새 제목" }
                                            """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("인증 없이 수정하면 401 을 반환한다")
        void updatePost_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(
                            patch("/api/v1/posts/12")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "title": "새 제목" }
                                            """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithLoginMember
        @DisplayName("지원하지 않는 메서드(PUT)는 405 를 반환한다")
        void updatePost_putMethod_returns405() throws Exception {
            // when & then
            mockMvc.perform(
                            put("/api/v1/posts/12")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/posts/{postId} 삭제")
    class DeletePost {

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 204 를 반환한다")
        void deletePost_owner_returns204() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/posts/12")).andExpect(status().isNoContent());
            then(postService).should().delete(12L, 1L);
        }

        @Test
        @WithLoginMember(memberId = 2L)
        @DisplayName("작성자가 아니면 403 을 반환한다")
        void deletePost_notOwner_returns403() throws Exception {
            // given
            willThrow(new BusinessException(ErrorCode.POST_NOT_OWNER))
                    .given(postService)
                    .delete(12L, 2L);

            // when & then
            mockMvc.perform(delete("/api/v1/posts/12"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_OWNER"));
        }

        @Test
        @WithLoginMember
        @DisplayName("없는 게시글이면 404 를 반환한다")
        void deletePost_notFound_returns404() throws Exception {
            // given
            willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND))
                    .given(postService)
                    .delete(anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete("/api/v1/posts/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("인증 없이 삭제하면 401 을 반환한다")
        void deletePost_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/posts/12")).andExpect(status().isUnauthorized());
        }
    }
}
