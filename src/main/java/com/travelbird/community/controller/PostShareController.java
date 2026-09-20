package com.travelbird.community.controller;

import com.travelbird.community.controller.dto.ShareRequest;
import com.travelbird.community.service.PostShareService;
import com.travelbird.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 게시글 공유수 증가. backend-functional-spec-v10.md §3.9.5 — 인증 선택. */
@RestController
@RequiredArgsConstructor
public class PostShareController {

    private final PostShareService postShareService;

    @PostMapping("/api/posts/{postId}/shares")
    public ResponseEntity<Void> share(@PathVariable Long postId, @RequestBody ShareRequest request) {
        postShareService.share(postId, SecurityUtils.getCurrentUserIdOrNull(), request.channel());
        return ResponseEntity.noContent().build();
    }
}
