package com.travelbird.post.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.post.controller.dto.CreatePostRequest;
import com.travelbird.post.controller.dto.CreatePostResponse;
import com.travelbird.post.controller.dto.MyPostsResponse;
import com.travelbird.post.controller.dto.PostDetailResponse;
import com.travelbird.post.service.PostCreateService;
import com.travelbird.post.service.PostDetailService;
import com.travelbird.post.service.PostMyListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 작성/목록/상세. backend-functional-spec-v10.md §3.8.1, §3.8.3. 작성·목록은
 * 로그인 필수, 상세는 비로그인도 허용한다(SecurityConfig permitAll, PR#17).
 */
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostCreateService postCreateService;
    private final PostMyListService postMyListService;
    private final PostDetailService postDetailService;

    @PostMapping("/api/posts")
    public ResponseEntity<CreatePostResponse> create(@RequestBody CreatePostRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CreatePostResponse response = postCreateService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/users/me/posts")
    public MyPostsResponse listMine(@RequestParam(required = false) Long cursor,
                                     @RequestParam(required = false) Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return postMyListService.list(userId, cursor, size);
    }

    @GetMapping("/api/posts/{postId}")
    public PostDetailResponse getDetail(@PathVariable Long postId) {
        return postDetailService.getDetail(postId, SecurityUtils.getCurrentUserIdOrNull());
    }
}
