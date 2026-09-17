package com.ilog.post.controller;

import com.ilog.global.response.ApiResponse;
import com.ilog.global.response.PageResponse;
import com.ilog.global.security.LoginMember;
import com.ilog.post.dto.PostCreateRequest;
import com.ilog.post.dto.PostDetailResponse;
import com.ilog.post.dto.PostIdResponse;
import com.ilog.post.dto.PostSearchRequest;
import com.ilog.post.dto.PostSummaryResponse;
import com.ilog.post.dto.PostUpdateRequest;
import com.ilog.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "게시글", description = "게시글 등록·목록/검색·상세·수정·삭제")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 등록 (FN-PST-001)")
    @PostMapping
    public ResponseEntity<ApiResponse<PostIdResponse>> createPost(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody PostCreateRequest request) {
        Long postId = postService.create(loginMember.memberId(), request);
        return ResponseEntity.created(URI.create("/api/v1/posts/" + postId))
                .body(ApiResponse.ok(new PostIdResponse(postId)));
    }

    @Operation(
            summary = "게시글 목록 + 검색 (FN-PST-002, 005)",
            description = "조건 간 AND, hashtags 는 OR. 내 글 모아보기는 nickname 에 내 닉네임")
    @GetMapping
    public ApiResponse<PageResponse<PostSummaryResponse>> searchPosts(
            @ParameterObject @Valid @ModelAttribute PostSearchRequest request) {
        return ApiResponse.ok(postService.search(request));
    }

    @Operation(summary = "게시글 상세 (FN-PST-002)")
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getPost(
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember) {
        return ApiResponse.ok(postService.getPost(postId, loginMember.memberId()));
    }

    @Operation(summary = "게시글 수정 (FN-PST-003)", description = "null 인 필드는 변경하지 않음, urls 는 전체 교체")
    @PatchMapping("/{postId}")
    public ApiResponse<PostDetailResponse> updatePost(
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody PostUpdateRequest request) {
        return ApiResponse.ok(postService.update(postId, loginMember.memberId(), request));
    }

    @Operation(summary = "게시글 삭제 (FN-PST-004)")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember) {
        postService.delete(postId, loginMember.memberId());
        return ResponseEntity.noContent().build();
    }
}
