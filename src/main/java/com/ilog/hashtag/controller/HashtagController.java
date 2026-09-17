package com.ilog.hashtag.controller;

import com.ilog.global.response.ApiResponse;
import com.ilog.global.security.LoginMember;
import com.ilog.hashtag.dto.HashtagCreateRequest;
import com.ilog.hashtag.dto.HashtagResponse;
import com.ilog.hashtag.service.HashtagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "해시태그", description = "게시글 해시태그 등록·삭제")
@RestController
@RequestMapping("/api/v1/posts/{postId}/hashtags")
@RequiredArgsConstructor
public class HashtagController {

    private final HashtagService hashtagService;

    @Operation(summary = "해시태그 등록", description = "게시글 작성자만. 이미 있는 태그는 무시하고 전체 태그 목록을 반환")
    @PostMapping
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> addHashtags(
            @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody HashtagCreateRequest request) {
        List<HashtagResponse> hashtags =
                hashtagService.addHashtags(postId, loginMember.memberId(), request);
        return ResponseEntity.created(URI.create("/api/v1/posts/" + postId + "/hashtags"))
                .body(ApiResponse.ok(hashtags));
    }

    // 명세 불일치: 엔드포인트 DB 에 행이 없지만 기본규칙·기능명세에 존재 → [잠정] 구현
    @Operation(summary = "해시태그 삭제", description = "게시글 작성자만")
    @DeleteMapping("/{hashtagId}")
    public ResponseEntity<Void> deleteHashtag(
            @PathVariable Long postId,
            @PathVariable Long hashtagId,
            @Parameter(hidden = true) @AuthenticationPrincipal LoginMember loginMember) {
        hashtagService.deleteHashtag(postId, hashtagId, loginMember.memberId());
        return ResponseEntity.noContent().build();
    }
}
