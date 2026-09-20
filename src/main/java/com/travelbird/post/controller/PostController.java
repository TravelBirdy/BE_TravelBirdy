package com.travelbird.post.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.post.controller.dto.CreatePostRequest;
import com.travelbird.post.controller.dto.CreatePostResponse;
import com.travelbird.post.service.PostCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 게시글 작성·임시 저장·발행. backend-functional-spec-v10.md §3.8.1 — 로그인 필수. */
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostCreateService postCreateService;

    @PostMapping("/api/posts")
    public ResponseEntity<CreatePostResponse> create(@RequestBody CreatePostRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CreatePostResponse response = postCreateService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
