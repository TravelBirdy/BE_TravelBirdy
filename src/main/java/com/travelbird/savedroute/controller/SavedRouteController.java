package com.travelbird.savedroute.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.savedroute.controller.dto.SavedRouteListResponse;
import com.travelbird.savedroute.service.AiPreviewRouteSaveService;
import com.travelbird.savedroute.service.PostRouteSaveService;
import com.travelbird.savedroute.service.SavedRouteListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 경로 저장·취소·목록 조회. backend-functional-spec-v10.md §3.10. 전부 로그인 필수. */
@RestController
@RequiredArgsConstructor
public class SavedRouteController {

    private final PostRouteSaveService postRouteSaveService;
    private final AiPreviewRouteSaveService aiPreviewRouteSaveService;
    private final SavedRouteListService savedRouteListService;

    @PutMapping("/api/users/me/saved-routes/posts/{postId}")
    public ResponseEntity<Void> savePostRoute(@PathVariable Long postId) {
        postRouteSaveService.save(SecurityUtils.getCurrentUserId(), postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/users/me/saved-routes/posts/{postId}")
    public ResponseEntity<Void> cancelPostRoute(@PathVariable Long postId) {
        postRouteSaveService.cancel(SecurityUtils.getCurrentUserId(), postId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/users/me/saved-routes/ai-previews/{previewId}")
    public ResponseEntity<Void> saveAiPreviewRoute(@PathVariable Long previewId) {
        aiPreviewRouteSaveService.save(SecurityUtils.getCurrentUserId(), previewId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/users/me/saved-routes/ai-previews/{previewId}")
    public ResponseEntity<Void> cancelAiPreviewRoute(@PathVariable Long previewId) {
        aiPreviewRouteSaveService.cancel(SecurityUtils.getCurrentUserId(), previewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/me/saved-routes")
    public SavedRouteListResponse list(@RequestParam(required = false) Long cursor,
                                        @RequestParam(required = false) Integer size) {
        return savedRouteListService.list(cursor, size, SecurityUtils.getCurrentUserId());
    }
}
