package com.ilog.hashtag.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.hashtag.dto.HashtagResponse;
import com.ilog.hashtag.service.HashtagService;
import com.ilog.support.WebMvcTestSupport;
import com.ilog.support.security.WithLoginMember;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(HashtagController.class)
class HashtagControllerTest extends WebMvcTestSupport {

    @MockitoBean HashtagService hashtagService;

    @Nested
    @DisplayName("POST /api/v1/posts/{postId}/hashtags 해시태그 등록")
    class AddHashtags {

        private static final String BODY =
                """
                { "names": ["JWT", "Spring"] }
                """;

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 201 과 게시글의 전체 태그 목록을 반환한다")
        void addHashtags_owner_returns201() throws Exception {
            // given
            given(hashtagService.addHashtags(eq(12L), eq(1L), any()))
                    .willReturn(
                            List.of(
                                    new HashtagResponse(3L, "JWT"),
                                    new HashtagResponse(4L, "Spring")));

            // when & then
            mockMvc.perform(
                            post("/api/v1/posts/12/hashtags")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/posts/12/hashtags"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].hashtagId").value(3))
                    .andExpect(jsonPath("$.data[1].name").value("Spring"));
        }

        @Test
        @WithLoginMember
        @DisplayName("names 가 비어 있으면 400 을 반환한다")
        void addHashtags_emptyNames_returns400() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/posts/12/hashtags")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                                            { "names": [] }
                                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors[0].field").value("names"));
            then(hashtagService).should(never()).addHashtags(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("인증 없이 등록하면 401 을 반환한다")
        void addHashtags_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(
                            post("/api/v1/posts/12/hashtags")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithLoginMember(memberId = 2L)
        @DisplayName("게시글 작성자가 아니면 403 을 반환한다")
        void addHashtags_notOwner_returns403() throws Exception {
            // given
            given(hashtagService.addHashtags(anyLong(), anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_OWNER));

            // when & then
            mockMvc.perform(
                            post("/api/v1/posts/12/hashtags")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_OWNER"));
        }

        @Test
        @WithLoginMember
        @DisplayName("게시글이 없으면 404 를 반환한다")
        void addHashtags_postNotFound_returns404() throws Exception {
            // given
            given(hashtagService.addHashtags(anyLong(), anyLong(), any()))
                    .willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

            // when & then
            mockMvc.perform(
                            post("/api/v1/posts/99/hashtags")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/posts/{postId}/hashtags/{hashtagId} 해시태그 삭제")
    class DeleteHashtag {

        @Test
        @WithLoginMember(memberId = 1L)
        @DisplayName("성공하면 204 를 반환한다")
        void deleteHashtag_owner_returns204() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/posts/12/hashtags/3"))
                    .andExpect(status().isNoContent());
            then(hashtagService).should().deleteHashtag(12L, 3L, 1L);
        }

        @Test
        @DisplayName("인증 없이 삭제하면 401 을 반환한다")
        void deleteHashtag_unauthenticated_returns401() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/posts/12/hashtags/3"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithLoginMember(memberId = 2L)
        @DisplayName("게시글 작성자가 아니면 403 을 반환한다")
        void deleteHashtag_notOwner_returns403() throws Exception {
            // given
            willThrow(new BusinessException(ErrorCode.POST_NOT_OWNER))
                    .given(hashtagService)
                    .deleteHashtag(12L, 3L, 2L);

            // when & then
            mockMvc.perform(delete("/api/v1/posts/12/hashtags/3"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithLoginMember
        @DisplayName("태그가 해당 게시글 소속이 아니면 404 HASHTAG_NOT_FOUND 를 반환한다")
        void deleteHashtag_tagOfOtherPost_returns404() throws Exception {
            // given
            willThrow(new BusinessException(ErrorCode.HASHTAG_NOT_FOUND))
                    .given(hashtagService)
                    .deleteHashtag(anyLong(), anyLong(), anyLong());

            // when & then
            mockMvc.perform(delete("/api/v1/posts/12/hashtags/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("HASHTAG_NOT_FOUND"));
        }
    }
}
