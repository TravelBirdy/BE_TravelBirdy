package com.travelbird.community.controller;

import com.travelbird.community.service.PostViewService;
import com.travelbird.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게시글 조회수 증가. backend-functional-spec-v10.md §3.9.4 — 인증 선택, 항상 204. */
@RestController
@RequiredArgsConstructor
public class PostViewController {

    private final PostViewService postViewService;

    @PostMapping("/api/posts/{postId}/views")
    public ResponseEntity<Void> recordView(@PathVariable Long postId) {
        postViewService.recordView(postId, SecurityUtils.getCurrentUserIdOrNull());
        return ResponseEntity.noContent().build();
    }
}
